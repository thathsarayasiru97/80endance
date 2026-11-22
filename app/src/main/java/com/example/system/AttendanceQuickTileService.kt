package com.example.system

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.example.MainActivity
import com.example.data.AppConstants
import com.example.data.DateUtils
import java.util.Locale

/**
 * Android Quick Settings Tile (Notification Shade) for 1-tap attendance check-in.
 * - Shows live percentage & today's check-in state right in the Quick Settings panel.
 * - Tapping when today isn't logged yet marks Present immediately in the background
 *   (and shows a silent Undo notification) — or opens the app if already logged / weekend / signed out.
 */
class AttendanceQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTileVisuals()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        refreshTileVisuals()
    }

    override fun onClick() {
        super.onClick()
        val appCtx = applicationContext
        val snapshot = AttendanceWidgetStore.load(appCtx)
        val todayKey = DateUtils.todayKey()
        val isWeekend = DateUtils.isWeekend(todayKey)
        val canQuickMarkPresent = snapshot.signedIn &&
            !isWeekend &&
            snapshot.todayNoSchoolTitle.isNullOrBlank() &&
            snapshot.todayStatusLabel.isNullOrBlank()

        if (canQuickMarkPresent) {
            // Fire quick Present check-in directly via DailyReminderReceiver (with Undo confirmation)
            val quickIntent = Intent(appCtx, DailyReminderReceiver::class.java).apply {
                action = ReminderScheduler.ACTION_QUICK_YES
                putExtra(ReminderScheduler.EXTRA_SOURCE, "quick_tile")
            }
            appCtx.sendBroadcast(quickIntent)

            // Optimistically update tile state immediately for snappy UX
            qsTile?.apply {
                state = Tile.STATE_ACTIVE
                label = "Present ✓"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    subtitle = String.format(Locale.US, "%.1f%% • Logged", snapshot.percentage)
                }
                updateTile()
            }
        } else {
            // Open app directly to check-in or dashboard
            val openIntent = Intent(appCtx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (!isWeekend && snapshot.todayStatusLabel.isNullOrBlank()) {
                    putExtra(AppConstants.EXTRA_OPEN_CHECKIN, true)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pending = PendingIntent.getActivity(
                    appCtx,
                    8501,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                runCatching { startActivityAndCollapse(pending) }
                    .onFailure { Log.w(TAG, "Tile launch failed: ${it.message}") }
            } else {
                @Suppress("DEPRECATION")
                runCatching { startActivityAndCollapse(openIntent) }
                    .onFailure { Log.w(TAG, "Tile launch failed: ${it.message}") }
            }
        }
    }

    private fun refreshTileVisuals() {
        val tile = qsTile ?: return
        val snapshot = AttendanceWidgetStore.load(applicationContext)
        val todayKey = DateUtils.todayKey()
        val isWeekend = DateUtils.isWeekend(todayKey)

        when {
            !snapshot.signedIn -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "80endance"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Sign in"
                }
            }
            !snapshot.todayStatusLabel.isNullOrBlank() -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "${snapshot.todayStatusLabel} ✓"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = String.format(Locale.US, "%.1f%%", snapshot.percentage)
                }
            }
            isWeekend || !snapshot.todayNoSchoolTitle.isNullOrBlank() -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = String.format(Locale.US, "%.1f%%", snapshot.percentage)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = if (isWeekend) "Weekend" else "No School"
                }
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Mark Present"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = String.format(Locale.US, "%.1f%% • Tap", snapshot.percentage)
                }
            }
        }
        tile.updateTile()
    }

    companion object {
        private const val TAG = "AttendanceQuickTile"

        fun requestListeningUpdate(context: Context) {
            runCatching {
                requestListeningState(
                    context.applicationContext,
                    ComponentName(context.applicationContext, AttendanceQuickTileService::class.java)
                )
            }
        }
    }
}
