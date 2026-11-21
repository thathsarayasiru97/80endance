package com.example.system

import android.content.Context
import android.util.Log
import com.example.data.BroadcastNotification
import com.example.data.FirestoreProvider
import com.example.data.LocalPreferencesStore
import com.example.data.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Source
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class BackgroundCheckResult(
    val checkedCount: Int,
    val deliveredCount: Int,
    val permissionMissing: Boolean = false,
    val error: String? = null
)

/**
 * Single Mutex-protected engine responsible for checking Firestore for custom admin
 * announcements and delivering each matching announcement strictly ONCE on this device.
 *
 * Shared by:
 *  - Layer 1: [BackgroundNotificationService] (real-time foreground listener)
 *  - Layer 2: [BroadcastCheckReceiver] (2-minute Doze-aware AlarmManager wakeup)
 *  - Layer 3: [AttendanceSyncJobService] (15-minute persisted JobScheduler backup)
 *  - In-app Foreground ViewModel & Settings "Check Now" button
 */
object BackgroundBroadcastChecker {
    private const val TAG = "BgBroadcastChecker"
    private const val MAX_BROADCAST_AGE_MS = 7L * 86_400_000L

    private val deliveryMutex = Mutex()

    suspend fun checkAndDeliver(context: Context): BackgroundCheckResult {
        val appContext = context.applicationContext
        runCatching {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
        }

        val user = runCatching { Firebase.auth.currentUser }.getOrNull()
            ?: return BackgroundCheckResult(0, 0, error = "Not signed in")

        val prefsStore = LocalPreferencesStore(appContext)
        val cachedPrefs = prefsStore.getSnapshot()

        // Best-effort auth token refresh for cold background processes after prolonged Doze
        runCatching {
            withTimeoutOrNull(4_000L) {
                user.getIdToken(false).await()
            }
        }

        val db = runCatching { FirestoreProvider.getInstance(appContext) }
            .getOrElse { err ->
                Log.w(TAG, "Could not initialize Firestore in background: ${err.message}")
                return BackgroundCheckResult(0, 0, error = err.message ?: "Firestore unavailable")
            }

        // Resolve student audience profile (live Firestore first, instant fallback to local DataStore cache)
        val liveProfile = runCatching {
            withTimeoutOrNull(5_000L) {
                db.collection("users").document(user.uid)
                    .get(Source.SERVER)
                    .await()
                    .toObject(UserProfile::class.java)
            }
        }.getOrNull() ?: runCatching {
            withTimeoutOrNull(3_000L) {
                db.collection("users").document(user.uid)
                    .get()
                    .await()
                    .toObject(UserProfile::class.java)
            }
        }.getOrNull()

        if (liveProfile != null && liveProfile.setupCompleted) {
            runCatching {
                prefsStore.saveCachedStudentAudience(
                    userId = user.uid,
                    grade = liveProfile.grade,
                    stream = liveProfile.alStream,
                    className = liveProfile.className,
                    setupCompleted = true
                )
            }
        }

        val effectiveProfile = liveProfile ?: cachedPrefs.toCachedUserProfile()
        if (effectiveProfile == null || !effectiveProfile.setupCompleted) {
            return BackgroundCheckResult(0, 0, error = "Student setup not completed")
        }

        // Fetch latest broadcast notifications directly from server first so background checks never see stale cache
        val broadcasts = runCatching {
            withTimeoutOrNull(8_000L) {
                db.collection("broadcast_notifications")
                    .get(Source.SERVER)
                    .await()
                    .toObjects(BroadcastNotification::class.java)
            }
        }.getOrNull() ?: runCatching {
            withTimeoutOrNull(4_000L) {
                db.collection("broadcast_notifications")
                    .get()
                    .await()
                    .toObjects(BroadcastNotification::class.java)
            }
        }.getOrNull()

        if (broadcasts == null) {
            return BackgroundCheckResult(0, 0, error = "Could not reach server (offline or timed out)")
        }

        val result = deliverMatchingBroadcasts(
            context = appContext,
            prefsStore = prefsStore,
            userId = user.uid,
            profile = effectiveProfile,
            broadcasts = broadcasts
        )
        runCatching { prefsStore.markBackgroundCheckNow() }
        return result
    }

    /**
     * Thread-safe, deduplicated delivery gate. Re-reads [LocalPreferencesStore] inside [deliveryMutex]
     * so concurrent triggers (e.g. real-time service + ViewModel + AlarmManager) never double-notify.
     */
    suspend fun deliverMatchingBroadcasts(
        context: Context,
        prefsStore: LocalPreferencesStore,
        userId: String,
        profile: UserProfile?,
        broadcasts: List<BroadcastNotification>,
        maxAgeMillis: Long = MAX_BROADCAST_AGE_MS
    ): BackgroundCheckResult {
        if (userId.isBlank() || profile?.setupCompleted != true) {
            return BackgroundCheckResult(broadcasts.size, 0)
        }
        val appContext = context.applicationContext
        if (!ReminderScheduler.hasNotificationPermission(appContext)) {
            return BackgroundCheckResult(broadcasts.size, 0, permissionMissing = true)
        }

        return deliveryMutex.withLock {
            val prefs = prefsStore.getSnapshot()
            val alreadyHandled = prefs.notifiedBroadcastIds + prefs.seenBroadcastIds
            val nowMs = System.currentTimeMillis()

            val freshMatching = broadcasts.filter { bc ->
                bc.id.isNotBlank() &&
                    bc.id !in alreadyHandled &&
                    bc.matchesStudent(profile, userId) &&
                    run {
                        val createdMs = bc.createdAt?.toDate()?.time
                        createdMs == null || (nowMs - createdMs) <= maxAgeMillis
                    }
            }

            if (freshMatching.isEmpty()) {
                return@withLock BackgroundCheckResult(broadcasts.size, 0)
            }

            val postedIds = mutableListOf<String>()
            freshMatching.forEach { bc ->
                val posted = ReminderScheduler.showBroadcastNotification(
                    context = appContext,
                    broadcastId = bc.id,
                    title = bc.title,
                    message = bc.message
                )
                if (posted) {
                    postedIds.add(bc.id)
                }
            }

            if (postedIds.isNotEmpty()) {
                prefsStore.markBroadcastsNotified(postedIds)
                Log.d(TAG, "Delivered ${postedIds.size} announcement notification(s)")
            }
            BackgroundCheckResult(
                checkedCount = broadcasts.size,
                deliveredCount = postedIds.size
            )
        }
    }
}
