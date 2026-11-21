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
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ReminderScheduler.ACTION_DAILY_REMINDER -> handleDailyReminder(context)
                    ReminderScheduler.ACTION_QUICK_YES -> handleQuickCheckIn(context, AttendanceStatus.PRESENT)
                    ReminderScheduler.ACTION_QUICK_NO -> handleQuickCheckIn(context, AttendanceStatus.ABSENT)
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
        val isNonSchoolDay = runCatching {
            AttendanceRepository(context.applicationContext).isPublishedNonSchoolDay(todayKey)
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

        val yesIntent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_QUICK_YES
        }
        val yesPending = PendingIntent.getBroadcast(
            context,
            8021,
            yesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val noIntent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_QUICK_NO
        }
        val noPending = PendingIntent.getBroadcast(
            context,
            8022,
            noIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(0, "Yes, Attended", yesPending)
            .addAction(0, "No, Missed", noPending)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(ReminderScheduler.NOTIFICATION_ID, notification)
    }

    private suspend fun handleQuickCheckIn(context: Context, status: AttendanceStatus) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(ReminderScheduler.NOTIFICATION_ID)

        val uid = Firebase.auth.currentUser?.uid ?: return
        val todayKey = DateUtils.todayKey()
        val termId = AttendanceCalculator.resolveTermIdForDate(todayKey, SriLankaCalendarDefaults.defaultTerms)
        val repository = AttendanceRepository(context.applicationContext)
        repository.upsertAttendance(
            targetUserId = uid,
            dateKey = todayKey,
            termId = termId,
            status = status,
            subjectTags = emptyList(),
            note = null,
            markedBy = "student"
        )
        SyncScheduler.scheduleBackgroundSync(context.applicationContext)
    }
}
