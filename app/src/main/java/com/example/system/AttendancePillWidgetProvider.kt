package com.example.system

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import java.util.Locale

/**
 * Ultra-Slim 4×1 Glass Pill Home Screen Widget.
 * Displays a compact translucent pill right under the home screen clock:
 * e.g., `● 86.4%  ·  Safe (+4d)  ·  214d to A/L`
 */
class AttendancePillWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateSinglePillWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun refreshAll(context: Context) {
            val appCtx = context.applicationContext
            val manager = AppWidgetManager.getInstance(appCtx) ?: return
            val component = ComponentName(appCtx, AttendancePillWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            for (id in ids) {
                updateSinglePillWidget(appCtx, manager, id)
            }
        }

        internal fun formatPillSegments(snapshot: WidgetSnapshot): Triple<String, String, String> {
            if (!snapshot.signedIn) {
                return Triple("80endance", "Tap to sign in", "A/L Tracker")
            }
            val pctText = if (snapshot.totalCountedDays == 0) {
                "--%"
            } else {
                String.format(Locale.US, "%.1f%%", snapshot.percentage)
            }
            val statusText = when {
                snapshot.totalCountedDays == 0 -> "Ready"
                snapshot.eligible -> "Safe (+${snapshot.safeSkipsRemaining}d)"
                else -> "Need +${snapshot.daysNeededToRecover}d"
            }
            val alText = snapshot.daysUntilAlExam?.let { "${it}d to A/L" }
                ?: "A/L ${snapshot.alExamYear}"
            return Triple(pctText, statusText, alText)
        }

        private fun updateSinglePillWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int
        ) {
            val snapshot = AttendanceWidgetStore.load(context)
            val views = RemoteViews(context.packageName, R.layout.widget_attendance_pill)
            val (pctText, statusText, alText) = formatPillSegments(snapshot)

            val dotColor = when {
                !snapshot.signedIn || snapshot.totalCountedDays == 0 -> Color.parseColor("#93C5FD")
                snapshot.eligible && snapshot.safeSkipsRemaining <= 1 -> Color.parseColor("#FBBF24")
                snapshot.eligible -> Color.parseColor("#34D399")
                else -> Color.parseColor("#FB7185")
            }
            val statusColor = when {
                !snapshot.signedIn || snapshot.totalCountedDays == 0 -> Color.parseColor("#CBD5E1")
                snapshot.eligible && snapshot.safeSkipsRemaining <= 1 -> Color.parseColor("#FDE68A")
                snapshot.eligible -> Color.parseColor("#6EE7B7")
                else -> Color.parseColor("#FDA4AF")
            }

            views.setTextViewText(R.id.pill_percent_text, pctText)
            views.setTextViewText(R.id.pill_status_text, statusText)
            views.setTextViewText(R.id.pill_al_text, alText)
            views.setTextColor(R.id.pill_status_dot, dotColor)
            views.setTextColor(R.id.pill_status_text, statusColor)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                8400 + widgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.pill_widget_root, openPending)
            manager.updateAppWidget(widgetId, views)
        }
    }
}
