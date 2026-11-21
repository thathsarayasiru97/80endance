package com.example.system

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.example.data.FirestoreProvider
import com.example.data.LocalPreferencesStore
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Layer 3 of the Triple-Layer Background Notification Engine + Offline Write Flusher.
 *
 * Checks Firestore for newly released announcements FIRST via [BackgroundBroadcastChecker]
 * (so pending writes never stall announcement delivery), re-arms the 2-minute AlarmManager wakeup,
 * then flushes pending offline attendance writes and warms the notices cache.
 */
class AttendanceSyncJobService : JobService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        val appContext = applicationContext
        runCatching {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
        }
        val user = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (user == null) {
            jobFinished(params, false)
            return false
        }

        serviceScope.launch {
            var needsReschedule = false
            try {
                val prefsStore = LocalPreferencesStore(appContext)

                withTimeoutOrNull(20_000L) {
                    // 1. Check & deliver matching announcements FIRST before waitForPendingWrites can block
                    val checkResult = BackgroundBroadcastChecker.checkAndDeliver(appContext)
                    Log.d(
                        "AttendanceSyncJob",
                        "Broadcast check: checked=${checkResult.checkedCount}, " +
                            "delivered=${checkResult.deliveredCount}, err=${checkResult.error}"
                    )

                    // 2. Keep Layer 2 AlarmManager wakeup armed
                    SyncScheduler.scheduleBackgroundBroadcastAlarm(appContext)

                    // 3. Flush any pending offline attendance writes & warm published notices cache
                    val db = FirestoreProvider.getInstance(appContext)
                    runCatching {
                        withTimeoutOrNull(5_000L) {
                            db.waitForPendingWrites().await()
                        }
                    }
                    runCatching {
                        withTimeoutOrNull(5_000L) {
                            db.collection("notices").whereEqualTo("published", true).get().await()
                        }
                    }
                }
                prefsStore.markSyncedNow()
                Log.d("AttendanceSyncJob", "Background Firestore sync & notification check completed")
            } catch (e: Exception) {
                Log.w("AttendanceSyncJob", "Background sync deferred: ${e.message}")
                needsReschedule = true
            } finally {
                jobFinished(params, needsReschedule)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        serviceScope.cancel()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
