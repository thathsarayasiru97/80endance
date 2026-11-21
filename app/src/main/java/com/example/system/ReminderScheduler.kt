package com.example.system

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.R
import java.util.Calendar

object ReminderScheduler {
    const val CHANNEL_ID = "daily_checkin_channel"
    const val NOTIFICATION_ID = 8001
    const val CONFIRMATION_NOTIFICATION_ID = 8002
    const val ACTION_DAILY_REMINDER = "com.example.action.DAILY_REMINDER"
    const val ACTION_QUICK_YES = "com.example.action.QUICK_YES"
    const val ACTION_QUICK_EXCUSED = "com.example.action.QUICK_EXCUSED"
    const val ACTION_QUICK_NO = "com.example.action.QUICK_NO"
    const val EXTRA_SOURCE = "extra_checkin_source"

    private const val REQUEST_CODE_ALARM = 8010
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
            manager.createNotificationChannel(channel)
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
}
