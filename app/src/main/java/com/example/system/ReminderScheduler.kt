package com.example.system

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.DateUtils
import com.example.data.NextDayPollSummary
import java.util.Calendar
import kotlin.math.roundToInt

object ReminderScheduler {
    const val CHANNEL_ID = "daily_checkin_channel"
    const val BROADCAST_CHANNEL_ID = "admin_broadcast_channel"
    const val BACKGROUND_SERVICE_CHANNEL_ID = "background_listener_channel"
    const val DANGER_NUDGE_CHANNEL_ID = "danger_zone_nudge_channel"
    const val POLL_CHANNEL_ID = "next_day_poll_channel"
    const val NOTIFICATION_ID = 8001
    const val CONFIRMATION_NOTIFICATION_ID = 8002
    const val SUNDAY_PREP_NOTIFICATION_ID = 8003
    const val DANGER_NUDGE_NOTIFICATION_ID = 8004
    const val POLL_NOTIFICATION_ID = 8005
    const val POLL_RESULT_NOTIFICATION_ID = 8006
    const val BACKGROUND_SERVICE_NOTIFICATION_ID = 8099
    const val ACTION_DAILY_REMINDER = "com.example.action.DAILY_REMINDER"
    const val ACTION_SUNDAY_PREP = "com.example.action.SUNDAY_PREP"
    const val ACTION_QUICK_YES = "com.example.action.QUICK_YES"
    const val ACTION_QUICK_EXCUSED = "com.example.action.QUICK_EXCUSED"
    const val ACTION_QUICK_NO = "com.example.action.QUICK_NO"
    const val ACTION_SNOOZE_1H = "com.example.action.SNOOZE_1H"
    const val ACTION_QUICK_UNDO = "com.example.action.QUICK_UNDO"
    const val ACTION_UNLOCK_REMINDER = "com.example.action.UNLOCK_REMINDER"
    const val ACTION_NEXT_DAY_POLL_ALARM = "com.example.action.NEXT_DAY_POLL_ALARM"
    const val ACTION_POLL_VOTE_YES = "com.example.action.POLL_VOTE_YES"
    const val ACTION_POLL_VOTE_NO = "com.example.action.POLL_VOTE_NO"
    const val EXTRA_SOURCE = "extra_checkin_source"
    const val EXTRA_PREVIOUS_STATUS = "extra_previous_status"
    const val EXTRA_TARGET_DATE = "extra_target_date"
    const val EXTRA_POLL_TARGET_DATE = "extra_poll_target_date"

    private const val REQUEST_CODE_ALARM = 8010
    private const val REQUEST_CODE_SUNDAY_PREP = 8011
    private const val REQUEST_CODE_BG_SERVICE_TAP = 8012
    private const val REQUEST_CODE_SNOOZE_ALARM = 8013
    private const val REQUEST_CODE_POLL_ALARM = 8014
    private const val TAG = "ReminderScheduler"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            val broadcastChannel = NotificationChannel(
                BROADCAST_CHANNEL_ID,
                "School & Class Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Custom announcements sent to your grade, stream, or class."
                enableVibration(true)
                enableLights(true)
            }
            val backgroundServiceChannel = NotificationChannel(
                BACKGROUND_SERVICE_CHANNEL_ID,
                context.getString(R.string.bg_service_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = context.getString(R.string.bg_service_channel_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            val dangerNudgeChannel = NotificationChannel(
                DANGER_NUDGE_CHANNEL_ID,
                context.getString(R.string.danger_nudge_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.danger_nudge_channel_desc)
            }
            val pollChannel = NotificationChannel(
                POLL_CHANNEL_ID,
                context.getString(R.string.poll_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.poll_channel_desc)
            }
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(broadcastChannel)
            manager.createNotificationChannel(backgroundServiceChannel)
            manager.createNotificationChannel(dangerNudgeChannel)
            manager.createNotificationChannel(pollChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    fun buildBackgroundListenerNotification(context: Context): Notification {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            REQUEST_CODE_BG_SERVICE_TAP,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(appContext, BACKGROUND_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(appContext.getString(R.string.bg_service_notif_title))
            .setContentText(appContext.getString(R.string.bg_service_notif_body))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    /**
     * Posts a high-priority heads-up announcement notification.
     * Returns true if posted successfully, false if notification permission is missing or blocked.
     */
    fun showBroadcastNotification(
        context: Context,
        broadcastId: String,
        title: String,
        message: String
    ): Boolean {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        if (!hasNotificationPermission(appContext)) {
            Log.w(TAG, "Skipping broadcast notification ($broadcastId): notification permission not granted")
            return false
        }

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            (broadcastId.hashCode() and 0x7FFF) + 9000,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, BROADCAST_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title.ifBlank { appContext.getString(R.string.app_name) })
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        return runCatching {
            val notifId = 9100 + (broadcastId.hashCode().and(0x3FF))
            NotificationManagerCompat.from(appContext).notify(notifId, notification)
            true
        }.onFailure { e ->
            Log.w(TAG, "Could not show broadcast notification: ${e.message}")
        }.getOrDefault(false)
    }

    fun scheduleDailyReminder(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(appContext, DailyReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Daily reminder cancelled")
            return
        }

        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Skip weekends automatically for the next school-day trigger
        while (trigger.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
            trigger.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        ) {
            trigger.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled next daily reminder for ${trigger.time}")
        } catch (e: Exception) {
            Log.w(TAG, "Unable to schedule reminder alarm: ${e.message}")
        }
    }

    /**
     * Schedules the Smart Sunday Evening Prep briefing for the next Sunday at 7:00 PM (19:00).
     */
    fun scheduleSundayPrep(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(appContext, SundayPrepReceiver::class.java).apply {
            action = ACTION_SUNDAY_PREP
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE_SUNDAY_PREP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Sunday evening prep cancelled")
            return
        }

        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val daysUntilSunday = (Calendar.SUNDAY - get(Calendar.DAY_OF_WEEK) + 7) % 7
            add(Calendar.DAY_OF_YEAR, daysUntilSunday)
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 7)
            }
        }

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled next Sunday prep briefing for ${trigger.time}")
        } catch (e: Exception) {
            Log.w(TAG, "Unable to schedule Sunday prep alarm: ${e.message}")
        }
    }

    /**
     * Schedules a one-off snooze reminder [delayMinutes] from now (default 60 minutes).
     */
    fun scheduleSnoozeReminder(context: Context, delayMinutes: Int = 60) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(appContext, DailyReminderReceiver::class.java).apply {
            action = ACTION_UNLOCK_REMINDER
            putExtra(EXTRA_SOURCE, "snooze")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE_SNOOZE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + delayMinutes.coerceIn(5, 240) * 60_000L
        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            Log.d(TAG, "Scheduled 1h snooze reminder for +${delayMinutes}m")
        }.onFailure { e ->
            Log.w(TAG, "Unable to schedule snooze alarm: ${e.message}")
        }
    }

    /**
     * Calm, once-a-day Danger-Zone Smart Nudge fired only when attendance sits right on the 80% edge.
     */
    fun showDangerZoneNudgeNotification(
        context: Context,
        percentage: Double,
        thresholdPercent: Int,
        safeSkipsRemaining: Int
    ): Boolean {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        if (!hasNotificationPermission(appContext)) return false

        val body = if (safeSkipsRemaining <= 0) {
            String.format(
                java.util.Locale.US,
                "Heads up — you're at %.1f%%. 1 more absence drops you below %d%%.",
                percentage,
                thresholdPercent
            )
        } else {
            String.format(
                java.util.Locale.US,
                "Heads up — you're at %.1f%% with only %d safe skip left before %d%%.",
                percentage,
                safeSkipsRemaining,
                thresholdPercent
            )
        }

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            appContext,
            8041,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(appContext, DANGER_NUDGE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("80% Edge Heads-Up")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()

        return runCatching {
            NotificationManagerCompat.from(appContext).notify(DANGER_NUDGE_NOTIFICATION_ID, notif)
            true
        }.getOrDefault(false)
    }

    /**
     * Schedules the daily 8:00 PM (20:00) alarm for the Next-Day School Poll when globally enabled.
     */
    fun scheduleNextDayPollAlarm(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(appContext, DailyReminderReceiver::class.java).apply {
            action = ACTION_NEXT_DAY_POLL_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE_POLL_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Next-day poll 8:00 PM alarm cancelled")
            return
        }

        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled next-day poll alarm for ${trigger.time}")
        }.onFailure { e ->
            Log.w(TAG, "Unable to schedule next-day poll alarm: ${e.message}")
        }
    }

    fun showNextDayPollPromptNotification(
        context: Context,
        targetDateKey: String,
        isTomorrowTarget: Boolean
    ): Boolean {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        if (!hasNotificationPermission(appContext)) return false

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            appContext,
            8030,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val yesIntent = Intent(appContext, DailyReminderReceiver::class.java).apply {
            action = ACTION_POLL_VOTE_YES
            putExtra(EXTRA_POLL_TARGET_DATE, targetDateKey)
        }
        val yesPending = PendingIntent.getBroadcast(
            appContext,
            8031,
            yesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val noIntent = Intent(appContext, DailyReminderReceiver::class.java).apply {
            action = ACTION_POLL_VOTE_NO
            putExtra(EXTRA_POLL_TARGET_DATE, targetDateKey)
        }
        val noPending = PendingIntent.getBroadcast(
            appContext,
            8032,
            noIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isTomorrowTarget) {
            "Going to school tomorrow?"
        } else {
            "Going to school today?"
        }
        val body = "Quick roll-call for ${DateUtils.formatReadableShort(targetDateKey)} (open until 5:00 AM). Tap Yes or No to see live turnout."

        val notif = NotificationCompat.Builder(appContext, POLL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(0, appContext.getString(R.string.notif_poll_yes), yesPending)
            .addAction(0, appContext.getString(R.string.notif_poll_no), noPending)
            .build()

        return runCatching {
            NotificationManagerCompat.from(appContext).notify(POLL_NOTIFICATION_ID, notif)
            true
        }.getOrDefault(false)
    }

    fun showNextDayPollAnalysisNotification(
        context: Context,
        summary: NextDayPollSummary,
        userVotedGoing: Boolean
    ): Boolean {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        if (!hasNotificationPermission(appContext)) return false

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            appContext,
            8033,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val goingPct = if (summary.totalVotes > 0) summary.goingPercent.roundToInt() else if (userVotedGoing) 100 else 0
        val notGoingPct = (100 - goingPct).coerceIn(0, 100)
        val classPart = if (summary.myClassTotalCount > 0) {
            " • Class: ${summary.myClassGoingCount}/${summary.myClassTotalCount} coming"
        } else {
            ""
        }
        val headline = "Coming: $goingPct% (${summary.goingCount}) • Not Coming: $notGoingPct% (${summary.notGoingCount})"
        val detail = "$headline\nYou voted ${if (userVotedGoing) "Coming" else "Not Coming"} (${summary.totalVotes} student(s)$classPart). Open app to switch anytime before 5:00 AM."

        val notif = NotificationCompat.Builder(appContext, POLL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("School Roll-Call Live • ${DateUtils.formatReadableShort(summary.targetDateKey)}")
            .setContentText(headline)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()

        return runCatching {
            NotificationManagerCompat.from(appContext).notify(POLL_RESULT_NOTIFICATION_ID, notif)
            true
        }.getOrDefault(false)
    }
}

