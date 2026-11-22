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
import com.example.data.AttendanceCalculator
import com.example.data.AttendanceRepository
import com.example.data.DateUtils
import com.example.data.LocalPreferencesStore
import com.example.data.SriLankaCalendarDefaults
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class SundayPrepReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ReminderScheduler.ACTION_SUNDAY_PREP) return
        val pendingResult = goAsync()
        val appCtx = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = LocalPreferencesStore(appCtx).getSnapshot()
                if (prefs.sundayPrepEnabled) {
                    ReminderScheduler.scheduleSundayPrep(appCtx, true)
                } else {
                    return@launch
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        appCtx,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) return@launch
                }

                val uid = Firebase.auth.currentUser?.uid ?: return@launch
                val todayKey = DateUtils.todayKey()
                val repository = AttendanceRepository(appCtx)
                val (profile, records, events) = repository.getQuickWidgetMetrics(uid, todayKey)
                val activeTerm = AttendanceCalculator.resolveActiveTerm(
                    SriLankaCalendarDefaults.defaultTerms,
                    todayKey
                )
                val periodBlocks = repository.getQuickWidgetPeriodBlocks(uid)
                val termStats = AttendanceCalculator.calculateStats(
                    records = records,
                    thresholdPercent = activeTerm.minEligibilityPercent,
                    countExcusedAsPresent = true,
                    includePriorBalance = true,
                    priorPresent = profile?.priorPresentCount ?: 0,
                    priorAbsent = profile?.priorAbsentCount ?: 0,
                    termFilter = activeTerm,
                    lockedRange = profile?.lockedBaselineRange,
                    periodBlocks = periodBlocks
                )
                val digest = AttendanceCalculator.calculateLastWeekDigest(
                    records = records,
                    calendarEvents = events,
                    overallStats = termStats,
                    periodBlocks = periodBlocks,
                    todayKey = todayKey
                )
                val prep = AttendanceCalculator.calculateUpcomingWeekPrep(
                    calendarEvents = events,
                    termStats = termStats,
                    referenceDateKey = todayKey
                )

                val deltaStr = digest.weekOverWeekDelta?.let { d ->
                    val sign = if (d >= 0) "+" else ""
                    String.format(Locale.US, " (%s%.1f%%)", sign, d)
                } ?: ""
                val lastWeekSegment = if (digest.lastWeekSchoolDays > 0) {
                    "Last week: ${digest.lastWeekAttended}/${digest.lastWeekSchoolDays}"
                } else {
                    "Last week: School break"
                }
                val overallSegment = if (termStats.totalCountedDays == 0) {
                    "Overall: Ready to track"
                } else {
                    String.format(Locale.US, "Overall: %.1f%%%s", digest.overallPercent, deltaStr)
                }
                val holidaySegment = digest.nextHolidayDaysAway?.let { days ->
                    "Next holiday in ${days}d"
                } ?: "${prep.schoolDaysCount} school days next week"

                val topLine = "$lastWeekSegment · $overallSegment · $holidaySegment"
                val extraLine = if (prep.offDays.isNotEmpty()) {
                    val labels = prep.offDays.take(2).joinToString(", ") { ev ->
                        "${DateUtils.formatWeekdayName(ev.dateKey).take(3)}: ${ev.title}"
                    }
                    "Coming up: ${prep.schoolDaysCount} school days ($labels)."
                } else if (prep.eligible) {
                    "Coming up: Full 5-day week (${prep.safeSkipsRemaining} safe skip${if (prep.safeSkipsRemaining == 1) "" else "s"} left)."
                } else {
                    "Coming up: Attend next ${prep.daysNeededToRecover} day${if (prep.daysNeededToRecover == 1) "" else "s"} to climb back above ${termStats.thresholdPercent}%."
                }

                val body = "$topLine\n$extraLine"
                ReminderScheduler.ensureNotificationChannel(appCtx)

                val openIntent = Intent(appCtx, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openPending = PendingIntent.getActivity(
                    appCtx,
                    8030,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notif = NotificationCompat.Builder(appCtx, ReminderScheduler.CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Sunday Weekly Digest")
                    .setContentText(topLine)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(openPending)
                    .build()

                val manager = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.notify(ReminderScheduler.SUNDAY_PREP_NOTIFICATION_ID, notif)
            } catch (e: Exception) {
                Log.w("SundayPrepReceiver", "Failed to post Sunday prep briefing: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
