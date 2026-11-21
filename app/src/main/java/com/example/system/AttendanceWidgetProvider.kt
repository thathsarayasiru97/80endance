package com.example.system

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppConstants
import com.example.data.DateUtils
import java.util.Locale

data class WidgetSnapshot(
    val percentage: Double = 100.0,
    val eligible: Boolean = true,
    val thresholdPercent: Int = 80,
    val totalCountedDays: Int = 0,
    val safeSkipsRemaining: Int = 0,
    val daysNeededToRecover: Int = 0,
    val streak: Int = 0,
    val termName: String = "80endance",
    val todayDateKey: String = DateUtils.todayKey(),
    val todayStatusLabel: String? = null,
    val todayNoSchoolTitle: String? = null,
    val signedIn: Boolean = false
)

object AttendanceWidgetStore {
    private const val PREFS_NAME = "eightyendance_widget_snapshot"
    private const val KEY_PERCENT = "percent"
    private const val KEY_ELIGIBLE = "eligible"
    private const val KEY_THRESHOLD = "threshold"
    private const val KEY_TOTAL_DAYS = "total_days"
    private const val KEY_SAFE_SKIPS = "safe_skips"
    private const val KEY_RECOVER_DAYS = "recover_days"
    private const val KEY_STREAK = "streak"
    private const val KEY_TERM_NAME = "term_name"
    private const val KEY_TODAY_KEY = "today_key"
    private const val KEY_TODAY_STATUS = "today_status"
    private const val KEY_NO_SCHOOL = "no_school"
    private const val KEY_SIGNED_IN = "signed_in"

    fun saveAndRefresh(context: Context, snapshot: WidgetSnapshot) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_PERCENT, snapshot.percentage.toFloat())
            .putBoolean(KEY_ELIGIBLE, snapshot.eligible)
            .putInt(KEY_THRESHOLD, snapshot.thresholdPercent)
            .putInt(KEY_TOTAL_DAYS, snapshot.totalCountedDays)
            .putInt(KEY_SAFE_SKIPS, snapshot.safeSkipsRemaining)
            .putInt(KEY_RECOVER_DAYS, snapshot.daysNeededToRecover)
            .putInt(KEY_STREAK, snapshot.streak)
            .putString(KEY_TERM_NAME, snapshot.termName)
            .putString(KEY_TODAY_KEY, snapshot.todayDateKey)
            .putString(KEY_TODAY_STATUS, snapshot.todayStatusLabel)
            .putString(KEY_NO_SCHOOL, snapshot.todayNoSchoolTitle)
            .putBoolean(KEY_SIGNED_IN, snapshot.signedIn)
            .apply()
        AttendanceWidgetProvider.refreshAll(context.applicationContext)
    }

    fun load(context: Context): WidgetSnapshot {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(KEY_TODAY_KEY, "") ?: ""
        val currentToday = DateUtils.todayKey()
        val isSameDay = savedDate == currentToday
        return WidgetSnapshot(
            percentage = prefs.getFloat(KEY_PERCENT, 100f).toDouble(),
            eligible = prefs.getBoolean(KEY_ELIGIBLE, true),
            thresholdPercent = prefs.getInt(KEY_THRESHOLD, 80),
            totalCountedDays = prefs.getInt(KEY_TOTAL_DAYS, 0),
            safeSkipsRemaining = prefs.getInt(KEY_SAFE_SKIPS, 0),
            daysNeededToRecover = prefs.getInt(KEY_RECOVER_DAYS, 0),
            streak = prefs.getInt(KEY_STREAK, 0),
            termName = prefs.getString(KEY_TERM_NAME, "80endance") ?: "80endance",
            todayDateKey = currentToday,
            todayStatusLabel = if (isSameDay) prefs.getString(KEY_TODAY_STATUS, null) else null,
            todayNoSchoolTitle = if (isSameDay) prefs.getString(KEY_NO_SCHOOL, null) else null,
            signedIn = prefs.getBoolean(KEY_SIGNED_IN, false)
        )
    }
}

class AttendanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, AttendanceWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            for (id in ids) {
                updateSingleWidget(context, manager, id)
            }
        }

        internal fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val snap = AttendanceWidgetStore.load(context)
            val todayIsWeekend = DateUtils.isWeekend(snap.todayDateKey)
            val views = RemoteViews(context.packageName, R.layout.widget_attendance)

            views.setTextViewText(R.id.widget_term_label, "80endance • ${snap.termName}")
            views.setTextViewText(R.id.widget_streak_badge, "${snap.streak}d streak")
            views.setTextViewText(
                R.id.widget_percent_text,
                String.format(Locale.US, "%.1f%%", snap.percentage)
            )
            views.setTextColor(
                R.id.widget_percent_text,
                if (snap.eligible) Color.parseColor("#34D399") else Color.parseColor("#FB7185")
            )

            val headline = when {
                !snap.signedIn -> "Open 80endance"
                snap.totalCountedDays == 0 -> "Ready to track"
                snap.eligible -> "Eligible (>= ${snap.thresholdPercent}%)"
                else -> "Below ${snap.thresholdPercent}%"
            }
            val subtext = when {
                !snap.signedIn -> "Sign in to sync your live attendance"
                snap.totalCountedDays == 0 -> "Log your first school day below"
                snap.eligible && snap.safeSkipsRemaining > 0 ->
                    "Safe buffer: ${snap.safeSkipsRemaining} skip${if (snap.safeSkipsRemaining == 1) "" else "s"} left"
                snap.eligible ->
                    "Right at ${snap.thresholdPercent}% — attend next day"
                else ->
                    "Attend next ${snap.daysNeededToRecover} day${if (snap.daysNeededToRecover == 1) "" else "s"} to recover"
            }
            views.setTextViewText(R.id.widget_status_headline, headline)
            views.setTextViewText(R.id.widget_forecast_sub, subtext)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AppConstants.EXTRA_OPEN_CHECKIN, false)
            }
            val openPending = PendingIntent.getActivity(
                context,
                8100,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)

            when {
                !snap.signedIn -> {
                    views.setViewVisibility(R.id.widget_actions_row, View.GONE)
                    views.setViewVisibility(R.id.widget_done_banner, View.VISIBLE)
                    views.setTextViewText(R.id.widget_done_banner, "Tap to sign in & track")
                    views.setOnClickPendingIntent(R.id.widget_done_banner, openPending)
                }

                todayIsWeekend -> {
                    views.setViewVisibility(R.id.widget_actions_row, View.GONE)
                    views.setViewVisibility(R.id.widget_done_banner, View.VISIBLE)
                    views.setTextViewText(R.id.widget_done_banner, "Enjoy your weekend!")
                    views.setOnClickPendingIntent(R.id.widget_done_banner, openPending)
                }

                !snap.todayNoSchoolTitle.isNullOrBlank() && snap.todayStatusLabel == null -> {
                    views.setViewVisibility(R.id.widget_actions_row, View.GONE)
                    views.setViewVisibility(R.id.widget_done_banner, View.VISIBLE)
                    views.setTextViewText(R.id.widget_done_banner, "No School • ${snap.todayNoSchoolTitle}")
                    views.setOnClickPendingIntent(R.id.widget_done_banner, openPending)
                }

                !snap.todayStatusLabel.isNullOrBlank() -> {
                    views.setViewVisibility(R.id.widget_actions_row, View.GONE)
                    views.setViewVisibility(R.id.widget_done_banner, View.VISIBLE)
                    views.setTextViewText(R.id.widget_done_banner, "Today: ${snap.todayStatusLabel} ✓")
                    views.setOnClickPendingIntent(R.id.widget_done_banner, openPending)
                }

                else -> {
                    views.setViewVisibility(R.id.widget_actions_row, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_done_banner, View.GONE)

                    val presentIntent = Intent(context, DailyReminderReceiver::class.java).apply {
                        action = ReminderScheduler.ACTION_QUICK_YES
                        putExtra(ReminderScheduler.EXTRA_SOURCE, "widget")
                    }
                    val presentPending = PendingIntent.getBroadcast(
                        context,
                        8101,
                        presentIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_present, presentPending)

                    val absentIntent = Intent(context, DailyReminderReceiver::class.java).apply {
                        action = ReminderScheduler.ACTION_QUICK_NO
                        putExtra(ReminderScheduler.EXTRA_SOURCE, "widget")
                    }
                    val absentPending = PendingIntent.getBroadcast(
                        context,
                        8102,
                        absentIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_absent, absentPending)
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
