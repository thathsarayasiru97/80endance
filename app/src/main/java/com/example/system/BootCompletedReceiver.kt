package com.example.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.LocalPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val prefs = LocalPreferencesStore(appContext).getSnapshot()
                if (prefs.enabled) {
                    ReminderScheduler.scheduleDailyReminder(
                        appContext,
                        true,
                        prefs.hour,
                        prefs.minute
                    )
                }
                if (prefs.sundayPrepEnabled) {
                    ReminderScheduler.scheduleSundayPrep(appContext, true)
                }
                // Re-arm background notification backups after reboot or APK update
                // (Do NOT start ForegroundService from boot/package-replaced on Android 12+ to obey background FGS restrictions)
                SyncScheduler.schedulePeriodicNotificationSync(appContext)
                SyncScheduler.scheduleBackgroundBroadcastAlarm(appContext, delayMs = 15_000L)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
