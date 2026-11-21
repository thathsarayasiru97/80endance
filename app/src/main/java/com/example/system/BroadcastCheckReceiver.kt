package com.example.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Layer 2 of the Triple-Layer Background Notification Engine.
 *
 * Triggered by a self-rescheduling `AlarmManager.setAndAllowWhileIdle` alarm every 2 minutes
 * (and 5 seconds after swiping the app from Recents). Works even if the app process was killed
 * or the user disabled the persistent foreground listener.
 */
class BroadcastCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_CHECK_BROADCASTS) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()

        val wakeLock = runCatching {
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "80endance:BroadcastCheckWakeLock")
                ?.apply {
                    setReferenceCounted(false)
                    acquire(10_000L)
                }
        }.getOrNull()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching {
                    if (FirebaseApp.getApps(appContext).isEmpty()) {
                        FirebaseApp.initializeApp(appContext)
                    }
                }
                val signedIn = runCatching { Firebase.auth.currentUser != null }.getOrDefault(false)
                if (!signedIn) {
                    SyncScheduler.cancelBackgroundBroadcastAlarm(appContext)
                    return@launch
                }

                // Stay safely inside the BroadcastReceiver goAsync (~10s) budget
                val result = withTimeoutOrNull(8_500L) {
                    BackgroundBroadcastChecker.checkAndDeliver(appContext)
                }
                Log.d(
                    TAG,
                    "Alarm check finished: checked=${result?.checkedCount ?: 0}, " +
                        "delivered=${result?.deliveredCount ?: 0}, err=${result?.error}"
                )

                // Re-arm the next 2-minute wakeup + ensure JobScheduler backup is registered
                SyncScheduler.scheduleBackgroundBroadcastAlarm(appContext)
                SyncScheduler.schedulePeriodicNotificationSync(appContext)
            } catch (e: Exception) {
                Log.w(TAG, "Alarm background check failed: ${e.message}")
                runCatching { SyncScheduler.scheduleBackgroundBroadcastAlarm(appContext) }
            } finally {
                runCatching {
                    if (wakeLock?.isHeld == true) wakeLock.release()
                }
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CHECK_BROADCASTS = "com.example.action.CHECK_BROADCASTS"
        private const val TAG = "BroadcastCheckReceiver"
    }
}
