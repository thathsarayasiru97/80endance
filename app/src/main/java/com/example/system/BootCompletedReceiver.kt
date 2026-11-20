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
                val prefs = LocalPreferencesStore(context.applicationContext).getSnapshot()
                if (prefs.enabled) {
                    ReminderScheduler.scheduleDailyReminder(
                        context.applicationContext,
                        true,
                        prefs.hour,
                        prefs.minute
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
