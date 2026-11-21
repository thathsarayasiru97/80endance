package com.example.system

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object SyncScheduler {
    private const val SYNC_JOB_ID = 80801
    private const val PERIODIC_NOTIFICATION_SYNC_JOB_ID = 80802
    private const val PERIODIC_INTERVAL_MS = 15L * 60L * 1000L // 15 min minimum allowed by Android JobScheduler
    private const val TAG = "SyncScheduler"

    fun scheduleBackgroundSync(context: Context) {
        val appContext = context.applicationContext
        val scheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            ?: return
        val component = ComponentName(appContext, AttendanceSyncJobService::class.java)
        val jobInfo = JobInfo.Builder(SYNC_JOB_ID, component)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setBackoffCriteria(30_000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .build()
        val result = scheduler.schedule(jobInfo)
        if (result == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Scheduled background network sync job")
        } else {
            Log.w(TAG, "Failed to schedule background sync job")
        }
    }

    /**
     * Schedules a Play-policy-compliant periodic JobScheduler task (every 15 minutes, persisted across reboots)
     * that checks Firestore for newly released admin announcements and delivers them as device notifications
     * even when the app is closed.
     */
    fun schedulePeriodicNotificationSync(context: Context) {
        val appContext = context.applicationContext
        val scheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            ?: return
        if (scheduler.getPendingJob(PERIODIC_NOTIFICATION_SYNC_JOB_ID) != null) {
            return
        }
        val component = ComponentName(appContext, AttendanceSyncJobService::class.java)
        val builder = JobInfo.Builder(PERIODIC_NOTIFICATION_SYNC_JOB_ID, component)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(PERIODIC_INTERVAL_MS)
            .setPersisted(true)
        val result = runCatching { scheduler.schedule(builder.build()) }.getOrDefault(JobScheduler.RESULT_FAILURE)
        if (result == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Scheduled 15-min periodic background notification sync")
        } else {
            Log.w(TAG, "Could not schedule periodic background notification sync")
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun observeNetworkConnectivity(context: Context): Flow<Boolean> = callbackFlow {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        trySend(isNetworkAvailable(context))

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(isNetworkAvailable(context))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            cm.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to register network callback: ${e.message}")
            trySend(isNetworkAvailable(context))
        }

        awaitClose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
                // Already unregistered
            }
        }
    }.distinctUntilChanged()
}
