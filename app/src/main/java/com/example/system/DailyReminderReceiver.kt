package com.example.system

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppConstants
import com.example.data.AttendanceCalculator
import com.example.data.AttendanceRecord
import com.example.data.AttendanceRepository
import com.example.data.AttendanceStatus
import com.example.data.DateUtils
import com.example.data.LocalPreferencesStore
import com.example.data.SriLankaCalendarDefaults
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val source = intent.getStringExtra(ReminderScheduler.EXTRA_SOURCE) ?: "notification"
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ReminderScheduler.ACTION_DAILY_REMINDER -> handleDailyReminder(context)
                    ReminderScheduler.ACTION_QUICK_YES -> handleQuickCheckIn(context, AttendanceStatus.PRESENT, source)
                    ReminderScheduler.ACTION_QUICK_EXCUSED -> handleQuickCheckIn(context, AttendanceStatus.SCHOOL_EVENT, source)
                    ReminderScheduler.ACTION_QUICK_NO -> handleQuickCheckIn(context, AttendanceStatus.ABSENT, source)
                }
            } catch (e: Exception) {
                Log.w("DailyReminderReceiver", "Reminder action error: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleDailyReminder(context: Context) {
        val prefs = LocalPreferencesStore(context).getSnapshot()
        // Always reschedule the next school day
        if (prefs.enabled) {
            ReminderScheduler.scheduleDailyReminder(context, true, prefs.hour, prefs.minute)
        }

        val todayKey = DateUtils.todayKey()
        if (DateUtils.isWeekend(todayKey)) {
            return
        }
        val repository = AttendanceRepository(context.applicationContext)
        val uid = Firebase.auth.currentUser?.uid
        if (!uid.isNullOrBlank() && repository.hasLoggedAttendance(uid, todayKey)) {
            return
        }
        val isNonSchoolDay = runCatching {
            repository.isPublishedNonSchoolDay(todayKey)
        }.getOrElse {
            !AttendanceCalculator.isSchoolDay(todayKey, SriLankaCalendarDefaults.defaultEvents)
        }
        if (isNonSchoolDay) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ReminderScheduler.ensureNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AppConstants.EXTRA_OPEN_CHECKIN, true)
        }
        val openPending = PendingIntent.getActivity(
            context,
            8020,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val presentPending = buildQuickActionPendingIntent(
            context = context,
            action = ReminderScheduler.ACTION_QUICK_YES,
            requestCode = 8021
        )
        val excusedPending = buildQuickActionPendingIntent(
            context = context,
            action = ReminderScheduler.ACTION_QUICK_EXCUSED,
            requestCode = 8023
        )
        val absentPending = buildQuickActionPendingIntent(
            context = context,
            action = ReminderScheduler.ACTION_QUICK_NO,
            requestCode = 8022
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(0, context.getString(R.string.notif_action_present), presentPending)
            .addAction(0, context.getString(R.string.notif_action_excused), excusedPending)
            .addAction(0, context.getString(R.string.notif_action_absent), absentPending)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(ReminderScheduler.NOTIFICATION_ID, notification)
    }

    private fun buildQuickActionPendingIntent(
        context: Context,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderScheduler.EXTRA_SOURCE, "notification")
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private suspend fun handleQuickCheckIn(
        context: Context,
        status: AttendanceStatus,
        source: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(ReminderScheduler.NOTIFICATION_ID)

        val uid = Firebase.auth.currentUser?.uid ?: return
        val todayKey = DateUtils.todayKey()
        if (DateUtils.isWeekend(todayKey)) return

        val appCtx = context.applicationContext
        val activeTerm = AttendanceCalculator.resolveActiveTerm(SriLankaCalendarDefaults.defaultTerms, todayKey)
        val termId = AttendanceCalculator.resolveTermIdForDate(todayKey, SriLankaCalendarDefaults.defaultTerms)
        val repository = AttendanceRepository(appCtx)

        val result = repository.upsertAttendance(
            targetUserId = uid,
            dateKey = todayKey,
            termId = termId,
            status = status,
            subjectTags = emptyList(),
            note = null,
            markedBy = "student"
        )
        if (result.isSuccess) {
            LocalPreferencesStore(appCtx).markPromptDismissedForDate(todayKey)
            SyncScheduler.scheduleBackgroundSync(appCtx)

            // Refresh home screen widget immediately with newly logged attendance
            val (profile, records, events) = repository.getQuickWidgetMetrics(uid, todayKey)
            val mergedRecords = records.filterNot { it.dateKey == todayKey } +
                AttendanceRecord(userId = uid, dateKey = todayKey, termId = termId, status = status.raw)
            val stats = AttendanceCalculator.calculateStats(
                records = mergedRecords,
                thresholdPercent = activeTerm.minEligibilityPercent,
                countExcusedAsPresent = true,
                includePriorBalance = true,
                priorPresent = profile?.priorPresentCount ?: 0,
                priorAbsent = profile?.priorAbsentCount ?: 0,
                termFilter = activeTerm
            )
            val streak = AttendanceCalculator.calculateCurrentStreak(
                records = mergedRecords,
                calendarEvents = events,
                countExcusedAsPresent = true,
                todayKey = todayKey
            )
            AttendanceWidgetStore.saveAndRefresh(
                appCtx,
                WidgetSnapshot(
                    percentage = stats.percentage,
                    eligible = stats.eligible,
                    thresholdPercent = stats.thresholdPercent,
                    totalCountedDays = stats.totalCountedDays,
                    safeSkipsRemaining = stats.safeSkipsRemaining,
                    daysNeededToRecover = stats.daysNeededToRecover,
                    streak = streak,
                    termName = activeTerm.name,
                    todayDateKey = todayKey,
                    todayStatusLabel = status.label,
                    todayNoSchoolTitle = null,
                    signedIn = true
                )
            )

            // Provide instant silent confirmation when tapped from the notification shade
            if (source == "notification") {
                showQuickConfirmation(appCtx, manager, status, todayKey, stats.percentage)
            }
        }
    }

    private fun showQuickConfirmation(
        context: Context,
        manager: NotificationManager?,
        status: AttendanceStatus,
        dateKey: String,
        newPercent: Double
    ) {
        if (manager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            8025,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val confirmNotif = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Marked ${status.label} (${DateUtils.formatReadableShort(dateKey)})")
            .setContentText("Term attendance updated to ${String.format(java.util.Locale.US, "%.1f%%", newPercent)}. Tap to edit or add notes.")
            .setSilent(true)
            .setTimeoutAfter(5_000L)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()
        manager.notify(ReminderScheduler.CONFIRMATION_NOTIFICATION_ID, confirmNotif)
    }
}
