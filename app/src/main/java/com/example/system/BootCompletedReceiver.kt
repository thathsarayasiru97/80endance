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
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
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
                SyncScheduler.schedulePeriodicNotificationSync(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
