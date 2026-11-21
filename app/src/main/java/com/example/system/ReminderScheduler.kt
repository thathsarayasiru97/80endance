package com.example.system

import android.Manifest
import android.app.AlarmManager
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
import java.util.Calendar

object ReminderScheduler {
    const val CHANNEL_ID = "daily_checkin_channel"
    const val BROADCAST_CHANNEL_ID = "admin_broadcast_channel"
    const val NOTIFICATION_ID = 8001
    const val CONFIRMATION_NOTIFICATION_ID = 8002
    const val SUNDAY_PREP_NOTIFICATION_ID = 8003
    const val ACTION_DAILY_REMINDER = "com.example.action.DAILY_REMINDER"
    const val ACTION_SUNDAY_PREP = "com.example.action.SUNDAY_PREP"
    const val ACTION_QUICK_YES = "com.example.action.QUICK_YES"
    const val ACTION_QUICK_EXCUSED = "com.example.action.QUICK_EXCUSED"
    const val ACTION_QUICK_NO = "com.example.action.QUICK_NO"
    const val EXTRA_SOURCE = "extra_checkin_source"

    private const val REQUEST_CODE_ALARM = 8010
    private const val REQUEST_CODE_SUNDAY_PREP = 8011
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
            }
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(broadcastChannel)
        }
    }

    fun showBroadcastNotification(
        context: Context,
        broadcastId: String,
        title: String,
        message: String
    ) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
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
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        runCatching {
            val notifId = 9100 + (broadcastId.hashCode().and(0x3FF))
            NotificationManagerCompat.from(appContext).notify(notifId, notification)
        }.onFailure { e ->
            Log.w(TAG, "Could not show broadcast notification: ${e.message}")
        }
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
}
