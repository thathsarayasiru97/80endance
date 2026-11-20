package com.example.system

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.example.R
import com.example.data.LocalPreferencesStore
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
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
                // Flush pending offline mutations and warm published notices when back online
                withTimeoutOrNull(12_000L) {
                    db.waitForPendingWrites().await()
                    db.collection("notices").whereEqualTo("published", true).get().await()
                }
                LocalPreferencesStore(applicationContext).markSyncedNow()
                Log.d("AttendanceSyncJob", "Background Firestore sync completed")
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
