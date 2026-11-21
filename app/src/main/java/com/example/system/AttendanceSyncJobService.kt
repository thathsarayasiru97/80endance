package com.example.system

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.example.R
import com.example.data.BroadcastNotification
import com.example.data.LocalPreferencesStore
import com.example.data.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class AttendanceSyncJobService : JobService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        val user = Firebase.auth.currentUser
        if (user == null) {
            jobFinished(params, false)
            return false
        }

        serviceScope.launch {
            var needsReschedule = false
            try {
                val databaseId = applicationContext.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val prefsStore = LocalPreferencesStore(applicationContext)

                withTimeoutOrNull(18_000L) {
                    // 1. Flush any pending offline attendance writes
                    runCatching { db.waitForPendingWrites().await() }

                    // 2. Fetch the signed-in student's profile & latest broadcast announcements from server
                    val profile = runCatching {
                        db.collection("users").document(user.uid).get().await()
                            .toObject(UserProfile::class.java)
                    }.getOrNull()

                    val broadcasts = runCatching {
                        db.collection("broadcast_notifications")
                            .get(Source.SERVER)
                            .await()
                            .toObjects(BroadcastNotification::class.java)
                    }.getOrElse {
                        runCatching {
                            db.collection("broadcast_notifications")
                                .get()
                                .await()
                                .toObjects(BroadcastNotification::class.java)
                        }.getOrDefault(emptyList())
                    }

                    // 3. Deliver any unseen, matching announcements once (deduplicated via notifiedBroadcastIds)
                    if (profile?.setupCompleted == true && broadcasts.isNotEmpty()) {
                        val prefs = prefsStore.getSnapshot()
                        val alreadyNotified = prefs.notifiedBroadcastIds + prefs.seenBroadcastIds
                        val nowMs = System.currentTimeMillis()
                        val freshMatching = broadcasts.filter { bc ->
                            bc.id.isNotBlank() &&
                                bc.id !in alreadyNotified &&
                                bc.matchesStudent(profile, user.uid) &&
                                run {
                                    val createdMs = bc.createdAt?.toDate()?.time
                                    createdMs == null || (nowMs - createdMs) <= 7L * 86_400_000L
                                }
                        }

                        if (freshMatching.isNotEmpty()) {
                            // Persist notified IDs FIRST so they never trigger twice even if app opens right after
                            prefsStore.markBroadcastsNotified(freshMatching.map { it.id })
                            freshMatching.forEach { bc ->
                                ReminderScheduler.showBroadcastNotification(
                                    context = applicationContext,
                                    broadcastId = bc.id,
                                    title = bc.title,
                                    message = bc.message
                                )
                            }
                            Log.d("AttendanceSyncJob", "Delivered ${freshMatching.size} background announcement(s)")
                        }
                    }

                    // 4. Warm published notices cache
                    runCatching {
                        db.collection("notices").whereEqualTo("published", true).get().await()
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
