package com.example.system

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.data.BroadcastNotification
import com.example.data.DateUtils
import com.example.data.FirestoreProvider
import com.example.data.LocalPreferencesStore
import com.example.data.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Layer 1 of the Triple-Layer Background Notification Engine.
 *
 * Uses Android's Play-policy-compliant `remoteMessaging` Foreground Service type (no 6-hour timeout
 * on Android 15/16) with a silent `IMPORTANCE_MIN` status notification to maintain a live Firestore
 * snapshot listener for `/broadcast_notifications`.
 *
 * - Delivers custom announcements in 1–2 seconds even after the app is swiped from Recent Apps.
 * - Uses `START_STICKY` so Android restarts it if memory is reclaimed.
 * - In [onTaskRemoved] (swipe-away from Recents), immediately arms a 5-second AlarmManager wakeup
 *   plus the 2-minute periodic AlarmManager and 15-minute JobScheduler backups so even aggressive
 *   OEMs (Xiaomi/Oppo/Vivo/Samsung) still deliver announcements reliably.
 */
class BackgroundNotificationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var broadcastsListener: ListenerRegistration? = null
    private var profileListener: ListenerRegistration? = null
    private var activeUserId: String? = null

    @Volatile
    private var cachedProfile: UserProfile? = null

    private var unlockReceiverRegistered = false
    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_USER_PRESENT) return
            val appCtx = applicationContext
            serviceScope.launch {
                handleAfterSchoolUnlock(appCtx)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        runCatching {
            ContextCompat.registerReceiver(
                this,
                userPresentReceiver,
                IntentFilter(Intent.ACTION_USER_PRESENT),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            unlockReceiverRegistered = true
        }
    }

    private suspend fun handleAfterSchoolUnlock(appCtx: Context) {
        val prefsStore = LocalPreferencesStore(appCtx)
        val prefs = prefsStore.getSnapshot()
        if (!prefs.afterSchoolUnlockPromptEnabled) return
        val todayKey = DateUtils.todayKey()
        if (DateUtils.isWeekend(todayKey)) return
        if (prefs.lastUnlockPromptDate == todayKey) return

        // Only fire on first unlock after 1:45 PM (13:45) and before 10:30 PM
        val (hour, minute) = DateUtils.currentHourMinute()
        val minutesOfDay = hour * 60 + minute
        if (minutesOfDay < (13 * 60 + 45) || minutesOfDay > (22 * 60 + 30)) return

        // Mark fired for today first so subsequent unlocks don't duplicate
        prefsStore.markUnlockPromptFiredForDate(todayKey)
        val triggerIntent = Intent(appCtx, DailyReminderReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_UNLOCK_REMINDER
            putExtra(ReminderScheduler.EXTRA_SOURCE, "after_school_unlock")
        }
        appCtx.sendBroadcast(triggerIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 8+ requires startForeground() immediately when started via startForegroundService()
        val promoted = startForegroundCompat()
        if (!promoted) {
            stopSelf()
            return START_NOT_STICKY
        }

        val appContext = applicationContext
        runCatching {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
        }

        val user = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (user == null) {
            Log.d(TAG, "No signed-in user; stopping BackgroundNotificationService")
            stopSelf()
            return START_NOT_STICKY
        }

        // Keep backup layers armed alongside the live listener
        SyncScheduler.scheduleBackgroundBroadcastAlarm(appContext)
        SyncScheduler.schedulePeriodicNotificationSync(appContext)

        if (activeUserId != user.uid || broadcastsListener == null) {
            attachRealtimeListeners(user.uid)
        }

        // Run an immediate server check on start/restart so nothing sent while offline is missed
        serviceScope.launch {
            runCatching { BackgroundBroadcastChecker.checkAndDeliver(appContext) }
        }

        return START_STICKY
    }

    private fun startForegroundCompat(): Boolean {
        return runCatching {
            val notification = ReminderScheduler.buildBackgroundListenerNotification(applicationContext)
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                ReminderScheduler.BACKGROUND_SERVICE_NOTIFICATION_ID,
                notification,
                serviceType
            )
            true
        }.onFailure { err ->
            Log.w(TAG, "Unable to promote BackgroundNotificationService to foreground: ${err.message}")
        }.getOrDefault(false)
    }

    private fun attachRealtimeListeners(userId: String) {
        detachListeners()
        activeUserId = userId

        val appContext = applicationContext
        val prefsStore = LocalPreferencesStore(appContext)

        // Seed cachedProfile immediately from local DataStore so first snapshot never drops
        serviceScope.launch {
            if (cachedProfile == null) {
                cachedProfile = prefsStore.getSnapshot().toCachedUserProfile()
            }
        }

        val db = runCatching { FirestoreProvider.getInstance(appContext) }.getOrElse { err ->
            Log.w(TAG, "Firestore unavailable for real-time listener: ${err.message}")
            return
        }

        profileListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Profile listener warning: ${error.message}")
                    return@addSnapshotListener
                }
                val profile = snapshot?.toObject(UserProfile::class.java)
                if (profile != null && profile.setupCompleted) {
                    cachedProfile = profile
                    serviceScope.launch {
                        runCatching {
                            prefsStore.saveCachedStudentAudience(
                                userId = userId,
                                grade = profile.grade,
                                stream = profile.alStream,
                                className = profile.className,
                                setupCompleted = true
                            )
                        }
                    }
                }
            }

        broadcastsListener = db.collection("broadcast_notifications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Broadcast listener warning: ${error.message}")
                    return@addSnapshotListener
                }
                val broadcasts = snapshot?.toObjects(BroadcastNotification::class.java).orEmpty()
                if (broadcasts.isEmpty()) return@addSnapshotListener

                serviceScope.launch {
                    val profile = cachedProfile ?: prefsStore.getSnapshot().toCachedUserProfile()
                    BackgroundBroadcastChecker.deliverMatchingBroadcasts(
                        context = appContext,
                        prefsStore = prefsStore,
                        userId = userId,
                        profile = profile,
                        broadcasts = broadcasts
                    )
                    runCatching { prefsStore.markBackgroundCheckNow() }
                }
            }

        Log.d(TAG, "Attached real-time Firestore broadcast listener for user=$userId")
    }

    private fun detachListeners() {
        runCatching { broadcastsListener?.remove() }
        runCatching { profileListener?.remove() }
        broadcastsListener = null
        profileListener = null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val appContext = applicationContext
        Log.d(TAG, "App swiped from Recents — arming immediate 5s safety alarm + periodic backups")
        SyncScheduler.scheduleBackgroundBroadcastAlarm(appContext, delayMs = 5_000L)
        SyncScheduler.schedulePeriodicNotificationSync(appContext)
    }

    override fun onDestroy() {
        isRunning = false
        if (unlockReceiverRegistered) {
            runCatching { unregisterReceiver(userPresentReceiver) }
            unlockReceiverRegistered = false
        }
        detachListeners()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BgNotificationService"

        @Volatile
        var isRunning: Boolean = false
            private set

        /**
         * Safely starts the foreground listener service from foreground app UI.
         * Wrapped in runCatching so Android 12–16 background-start restrictions never crash the app.
         */
        fun start(context: Context): Boolean {
            val appContext = context.applicationContext
            val user = runCatching { Firebase.auth.currentUser }.getOrNull() ?: return false
            if (user.uid.isBlank()) return false
            val intent = Intent(appContext, BackgroundNotificationService::class.java)
            return runCatching {
                ContextCompat.startForegroundService(appContext, intent)
                true
            }.onFailure { err ->
                Log.w(TAG, "Deferred foreground listener start (${err.javaClass.simpleName}): ${err.message}")
            }.getOrDefault(false)
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            runCatching {
                appContext.stopService(Intent(appContext, BackgroundNotificationService::class.java))
            }
            isRunning = false
        }
    }
}
