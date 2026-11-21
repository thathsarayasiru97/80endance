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
                val termStats = AttendanceCalculator.calculateStats(
                    records = records,
                    thresholdPercent = activeTerm.minEligibilityPercent,
                    countExcusedAsPresent = true,
                    includePriorBalance = true,
                    priorPresent = profile?.priorPresentCount ?: 0,
                    priorAbsent = profile?.priorAbsentCount ?: 0,
                    termFilter = activeTerm,
                    lockedRange = profile?.lockedBaselineRange
                )
                val prep = AttendanceCalculator.calculateUpcomingWeekPrep(
                    calendarEvents = events,
                    termStats = termStats,
                    referenceDateKey = todayKey
                )

                val offDayText = if (prep.offDays.isEmpty()) {
                    "Full 5-day school week ahead."
                } else {
                    val labels = prep.offDays.take(2).joinToString(", ") { ev ->
                        val dayShort = DateUtils.formatWeekdayName(ev.dateKey).take(3)
                        "$dayShort: ${ev.title}"
                    }
                    "${prep.schoolDaysCount} school day${if (prep.schoolDaysCount == 1) "" else "s"} ahead ($labels)."
                }

                val standingText = if (termStats.totalCountedDays == 0) {
                    "Ready to track your ${activeTerm.name} attendance."
                } else if (prep.eligible) {
                    String.format(
                        Locale.US,
                        "Term: %.1f%% — %d safe skip%s left.",
                        prep.termPercentage,
                        prep.safeSkipsRemaining,
                        if (prep.safeSkipsRemaining == 1) "" else "s"
                    )
                } else {
                    String.format(
                        Locale.US,
                        "Term: %.1f%% — attend next %d day%s to recover.",
                        prep.termPercentage,
                        prep.daysNeededToRecover,
                        if (prep.daysNeededToRecover == 1) "" else "s"
                    )
                }

                val body = "$offDayText $standingText"
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
                    .setContentTitle("Sunday Week-Ahead Prep")
                    .setContentText(body)
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
