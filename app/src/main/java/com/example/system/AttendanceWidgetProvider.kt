package com.example.system

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppConstants
import com.example.data.AttendanceCalculator
import com.example.data.AttendanceRepository
import com.example.data.AttendanceStatus
import com.example.data.DateUtils
import com.example.data.WeekDaySummary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

data class WidgetWeekDay(
    val shortLabel: String, // "Mon", "Tue", "Wed", "Thu", "Fri"
    val statusCode: String, // "P", "A", "M", "E", "H", "L", "U", "F"
    val isToday: Boolean = false
)

data class WidgetSnapshot(
    val percentage: Double = 100.0,
    val eligible: Boolean = true,
    val thresholdPercent: Int = 80,
    val effectiveAttended: Int = 0,
    val totalCountedDays: Int = 0,
    val safeSkipsRemaining: Int = 0,
    val daysNeededToRecover: Int = 0,
    val streak: Int = 0,
    val termName: String = "80endance",
    val todayDateKey: String = DateUtils.todayKey(),
    val todayStatusLabel: String? = null,
    val todayNoSchoolTitle: String? = null,
    val signedIn: Boolean = false,
    val weekDays: List<WidgetWeekDay> = emptyList(),
    val daysUntilAlExam: Int? = null,
    val alExamYear: Int = 2026,
    val nextHolidayTitle: String? = null,
    val nextHolidayDateShort: String? = null
)

object AttendanceWidgetStore {
    private const val PREFS_NAME = "eightyendance_widget_snapshot"
    private const val KEY_PERCENT = "percent"
    private const val KEY_ELIGIBLE = "eligible"
    private const val KEY_THRESHOLD = "threshold"
    private const val KEY_ATTENDED = "effective_attended"
    private const val KEY_TOTAL_DAYS = "total_days"
    private const val KEY_SAFE_SKIPS = "safe_skips"
    private const val KEY_RECOVER_DAYS = "recover_days"
    private const val KEY_STREAK = "streak"
    private const val KEY_TERM_NAME = "term_name"
    private const val KEY_TODAY_KEY = "today_key"
    private const val KEY_TODAY_STATUS = "today_status"
    private const val KEY_NO_SCHOOL = "no_school"
    private const val KEY_SIGNED_IN = "signed_in"
    private const val KEY_WEEK_DAYS_JSON = "week_days_json"
    private const val KEY_AL_DAYS = "al_days"
    private const val KEY_AL_YEAR = "al_year"
    private const val KEY_NEXT_HOLIDAY_TITLE = "next_holiday_title"
    private const val KEY_NEXT_HOLIDAY_DATE = "next_holiday_date"

    fun toWidgetWeekDays(summary: List<WeekDaySummary>): List<WidgetWeekDay> {
        return summary.take(5).map { day ->
            val code = when {
                day.holidayLabel != null -> "H"
                day.isLocked -> "L"
                day.status == AttendanceStatus.PRESENT -> "P"
                day.status == AttendanceStatus.ABSENT -> "A"
                day.status == AttendanceStatus.MEDICAL -> "M"
                day.status == AttendanceStatus.SCHOOL_EVENT -> "E"
                day.status == AttendanceStatus.HOLIDAY -> "H"
                day.isFuture -> "F"
                else -> "U"
            }
            WidgetWeekDay(
                shortLabel = day.dayLabel,
                statusCode = code,
                isToday = day.isToday
            )
        }
    }

    internal fun serializeWeekDays(days: List<WidgetWeekDay>): String {
        val arr = JSONArray()
        days.take(5).forEach { d ->
            arr.put(
                JSONObject()
                    .put("l", d.shortLabel)
                    .put("s", d.statusCode)
                    .put("t", d.isToday)
            )
        }
        return arr.toString()
    }

    internal fun deserializeWeekDays(json: String?): List<WidgetWeekDay> {
        if (json.isNullOrBlank()) return defaultEmptyWeek()
        return runCatching {
            val arr = JSONArray(json)
            val list = mutableListOf<WidgetWeekDay>()
            for (i in 0 until arr.length().coerceAtMost(5)) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    WidgetWeekDay(
                        shortLabel = obj.optString("l", DEFAULT_LABELS.getOrElse(i) { "Day" }),
                        statusCode = obj.optString("s", "F"),
                        isToday = obj.optBoolean("t", false)
                    )
                )
            }
            if (list.size == 5) list else defaultEmptyWeek()
        }.getOrElse { defaultEmptyWeek() }
    }

    private val DEFAULT_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri")

    private fun defaultEmptyWeek(): List<WidgetWeekDay> =
        DEFAULT_LABELS.map { WidgetWeekDay(shortLabel = it, statusCode = "F", isToday = false) }

    fun saveAndRefresh(context: Context, snapshot: WidgetSnapshot) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_PERCENT, snapshot.percentage.toFloat())
            .putBoolean(KEY_ELIGIBLE, snapshot.eligible)
            .putInt(KEY_THRESHOLD, snapshot.thresholdPercent)
            .putInt(KEY_ATTENDED, snapshot.effectiveAttended)
            .putInt(KEY_TOTAL_DAYS, snapshot.totalCountedDays)
            .putInt(KEY_SAFE_SKIPS, snapshot.safeSkipsRemaining)
            .putInt(KEY_RECOVER_DAYS, snapshot.daysNeededToRecover)
            .putInt(KEY_STREAK, snapshot.streak)
            .putString(KEY_TERM_NAME, snapshot.termName)
            .putString(KEY_TODAY_KEY, snapshot.todayDateKey)
            .putString(KEY_TODAY_STATUS, snapshot.todayStatusLabel)
            .putString(KEY_NO_SCHOOL, snapshot.todayNoSchoolTitle)
            .putBoolean(KEY_SIGNED_IN, snapshot.signedIn)
            .putString(KEY_WEEK_DAYS_JSON, serializeWeekDays(snapshot.weekDays))
            .putInt(KEY_AL_DAYS, snapshot.daysUntilAlExam ?: -1)
            .putInt(KEY_AL_YEAR, snapshot.alExamYear)
            .putString(KEY_NEXT_HOLIDAY_TITLE, snapshot.nextHolidayTitle)
            .putString(KEY_NEXT_HOLIDAY_DATE, snapshot.nextHolidayDateShort)
            .apply()
        AttendanceWidgetProvider.refreshAll(context.applicationContext)
    }

    fun load(context: Context): WidgetSnapshot {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(KEY_TODAY_KEY, "") ?: ""
        val currentToday = DateUtils.todayKey()
        val isSameDay = savedDate == currentToday
        val alDaysRaw = prefs.getInt(KEY_AL_DAYS, -1)
        return WidgetSnapshot(
            percentage = prefs.getFloat(KEY_PERCENT, 100f).toDouble(),
            eligible = prefs.getBoolean(KEY_ELIGIBLE, true),
            thresholdPercent = prefs.getInt(KEY_THRESHOLD, 80),
            effectiveAttended = prefs.getInt(KEY_ATTENDED, 0),
            totalCountedDays = prefs.getInt(KEY_TOTAL_DAYS, 0),
            safeSkipsRemaining = prefs.getInt(KEY_SAFE_SKIPS, 0),
            daysNeededToRecover = prefs.getInt(KEY_RECOVER_DAYS, 0),
            streak = prefs.getInt(KEY_STREAK, 0),
            termName = prefs.getString(KEY_TERM_NAME, "80endance") ?: "80endance",
            todayDateKey = currentToday,
            todayStatusLabel = if (isSameDay) prefs.getString(KEY_TODAY_STATUS, null) else null,
            todayNoSchoolTitle = if (isSameDay) prefs.getString(KEY_NO_SCHOOL, null) else null,
            signedIn = prefs.getBoolean(KEY_SIGNED_IN, false),
            weekDays = deserializeWeekDays(prefs.getString(KEY_WEEK_DAYS_JSON, null)),
            daysUntilAlExam = alDaysRaw.takeIf { it >= 0 },
            alExamYear = prefs.getInt(KEY_AL_YEAR, 2026),
            nextHolidayTitle = prefs.getString(KEY_NEXT_HOLIDAY_TITLE, null),
            nextHolidayDateShort = prefs.getString(KEY_NEXT_HOLIDAY_DATE, null)
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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateSingleWidget(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_REFRESH) {
            val pending = goAsync()
            val appCtx = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    refreshFromCloudBestEffort(appCtx)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.example.eightyendance.ACTION_WIDGET_REFRESH"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, AttendanceWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            for (id in ids) {
                updateSingleWidget(context, manager, id)
            }
        }

        private suspend fun refreshFromCloudBestEffort(context: Context) {
            val uid = Firebase.auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                refreshAll(context)
                return
            }
            val todayKey = DateUtils.todayKey()
            val repo = AttendanceRepository(context)
            val (profile, records, events) = repo.getQuickWidgetMetrics(uid, todayKey)
            val periodBlocks = repo.getQuickWidgetPeriodBlocks(uid)
            val (settings, terms) = repo.getQuickWidgetSettingsAndTerms()
            val activeTerm = AttendanceCalculator.resolveActiveTerm(terms, todayKey)
            val lockedRange = profile?.lockedBaselineRange
            val stats = AttendanceCalculator.calculateStats(
                records = records,
                thresholdPercent = activeTerm.minEligibilityPercent,
                countExcusedAsPresent = true,
                includePriorBalance = true,
                priorPresent = profile?.priorPresentCount ?: 0,
                priorAbsent = profile?.priorAbsentCount ?: 0,
                termFilter = activeTerm,
                lockedRange = lockedRange,
                periodBlocks = periodBlocks
            )
            val streak = AttendanceCalculator.calculateCurrentStreak(
                records = records,
                calendarEvents = events,
                countExcusedAsPresent = true,
                todayKey = todayKey
            )
            val weekly = AttendanceCalculator.calculateWeeklySummary(
                records = records,
                calendarEvents = events,
                todayKey = todayKey,
                lockedRange = lockedRange,
                periodBlocks = periodBlocks
            )
            val nextHoliday = AttendanceCalculator.nextUpcomingPoyaOrHoliday(events, todayKey)
            val examYear = profile?.alExamYear ?: settings.currentAcademicYear
            val daysToExam = AttendanceCalculator.daysUntilAlExam(examYear, settings, todayKey)
            val todayRec = records.firstOrNull { it.dateKey == todayKey }
            val todayNoSchool = events.firstOrNull { it.dateKey == todayKey && it.nonSchoolDay }

            AttendanceWidgetStore.saveAndRefresh(
                context,
                WidgetSnapshot(
                    percentage = stats.percentage,
                    eligible = stats.eligible,
                    thresholdPercent = stats.thresholdPercent,
                    effectiveAttended = stats.effectiveAttended,
                    totalCountedDays = stats.totalCountedDays,
                    safeSkipsRemaining = stats.safeSkipsRemaining,
                    daysNeededToRecover = stats.daysNeededToRecover,
                    streak = streak,
                    termName = activeTerm.name,
                    todayDateKey = todayKey,
                    todayStatusLabel = todayRec?.statusEnum?.label,
                    todayNoSchoolTitle = todayNoSchool?.title,
                    signedIn = true,
                    weekDays = AttendanceWidgetStore.toWidgetWeekDays(weekly),
                    daysUntilAlExam = daysToExam,
                    alExamYear = examYear,
                    nextHolidayTitle = nextHoliday?.title,
                    nextHolidayDateShort = nextHoliday?.dateKey?.let { DateUtils.formatReadableShort(it) }
                )
            )
        }

        internal fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            overrideOptions: Bundle? = null
        ) {
            val snap = AttendanceWidgetStore.load(context)
            val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val sizeMapping = mapOf(
                    SizeF(110f, 110f) to buildCompactViews(context, snap),
                    SizeF(230f, 130f) to buildStandardViews(context, snap),
                    SizeF(230f, 195f) to buildExpandedViews(context, snap)
                )
                RemoteViews(sizeMapping)
            } else {
                val options = overrideOptions ?: appWidgetManager.getAppWidgetOptions(appWidgetId)
                val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250) ?: 250
                val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140) ?: 140
                when {
                    minWidth < 210 || minHeight < 120 -> buildCompactViews(context, snap)
                    minHeight >= 190 -> buildExpandedViews(context, snap)
                    else -> buildStandardViews(context, snap)
                }
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        internal fun buildCompactViews(context: Context, snap: WidgetSnapshot): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_attendance_compact)
            val todayIsWeekend = DateUtils.isWeekend(snap.todayDateKey)
            val openPending = buildOpenAppPendingIntent(context)
            views.setOnClickPendingIntent(R.id.widget_root, openPending)

            views.setTextViewText(R.id.widget_term_label, "80endance")
            views.setTextViewText(R.id.widget_streak_badge, "${snap.streak}d")
            views.setTextViewText(
                R.id.widget_percent_text,
                String.format(Locale.US, "%.1f%%", snap.percentage)
            )
            views.setTextColor(
                R.id.widget_percent_text,
                if (snap.eligible) Color.parseColor("#34D399") else Color.parseColor("#FB7185")
            )
            val sub = when {
                !snap.signedIn -> "Tap to sign in"
                snap.totalCountedDays == 0 -> "Ready to track"
                snap.eligible && snap.safeSkipsRemaining > 0 -> "${snap.safeSkipsRemaining} skips left"
                snap.eligible -> "At ${snap.thresholdPercent}% goal"
                else -> "Need +${snap.daysNeededToRecover}d"
            }
            views.setTextViewText(R.id.widget_forecast_sub, sub)

            val pillText = when {
                !snap.signedIn -> "Sign in"
                todayIsWeekend -> "Weekend"
                !snap.todayNoSchoolTitle.isNullOrBlank() && snap.todayStatusLabel == null -> "No School"
                !snap.todayStatusLabel.isNullOrBlank() -> "${snap.todayStatusLabel} ✓"
                else -> "Check In"
            }
            views.setTextViewText(R.id.widget_done_banner, pillText)
            views.setOnClickPendingIntent(R.id.widget_done_banner, openPending)
            return views
        }

        internal fun buildStandardViews(context: Context, snap: WidgetSnapshot): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_attendance)
            bindSharedHeaderAndStats(context, views, snap, includeCountedDays = false)

            val dayViewIds = intArrayOf(
                R.id.widget_day_0,
                R.id.widget_day_1,
                R.id.widget_day_2,
                R.id.widget_day_3,
                R.id.widget_day_4
            )
            val week = snap.weekDays.ifEmpty {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri").map { WidgetWeekDay(it, "F") }
            }
            week.take(5).forEachIndexed { idx, day ->
                val initial = day.shortLabel.take(1)
                val glyph = glyphForCode(day.statusCode)
                views.setTextViewText(dayViewIds[idx], "$initial $glyph")
                views.setInt(dayViewIds[idx], "setBackgroundResource", bgResForDay(day))
                views.setTextColor(dayViewIds[idx], textColorForDay(day))
            }

            bindBottomActions(context, views, snap)
            return views
        }

        internal fun buildExpandedViews(context: Context, snap: WidgetSnapshot): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_attendance_expanded)
            bindSharedHeaderAndStats(context, views, snap, includeCountedDays = true)

            val pctInt = snap.percentage.roundToInt().coerceIn(0, 100)
            if (snap.eligible) {
                views.setViewVisibility(R.id.widget_progress_ok, View.VISIBLE)
                views.setViewVisibility(R.id.widget_progress_alert, View.GONE)
                views.setProgressBar(R.id.widget_progress_ok, 100, pctInt, false)
            } else {
                views.setViewVisibility(R.id.widget_progress_ok, View.GONE)
                views.setViewVisibility(R.id.widget_progress_alert, View.VISIBLE)
                views.setProgressBar(R.id.widget_progress_alert, 100, pctInt, false)
            }

            val boxIds = intArrayOf(
                R.id.widget_day_box_0,
                R.id.widget_day_box_1,
                R.id.widget_day_box_2,
                R.id.widget_day_box_3,
                R.id.widget_day_box_4
            )
            val labelIds = intArrayOf(
                R.id.widget_day_label_0,
                R.id.widget_day_label_1,
                R.id.widget_day_label_2,
                R.id.widget_day_label_3,
                R.id.widget_day_label_4
            )
            val glyphIds = intArrayOf(
                R.id.widget_day_glyph_0,
                R.id.widget_day_glyph_1,
                R.id.widget_day_glyph_2,
                R.id.widget_day_glyph_3,
                R.id.widget_day_glyph_4
            )
            val week = snap.weekDays.ifEmpty {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri").map { WidgetWeekDay(it, "F") }
            }
            var attendedThisWeek = 0
            var schoolDaysThisWeek = 0
            week.take(5).forEachIndexed { idx, day ->
                if (day.statusCode != "H") schoolDaysThisWeek++
                if (day.statusCode in setOf("P", "M", "E")) attendedThisWeek++
                views.setTextViewText(labelIds[idx], day.shortLabel)
                views.setTextViewText(glyphIds[idx], glyphForCode(day.statusCode))
                views.setInt(boxIds[idx], "setBackgroundResource", bgResForDay(day))
                views.setTextColor(glyphIds[idx], textColorForDay(day))
            }
            views.setTextViewText(
                R.id.widget_week_tally,
                "$attendedThisWeek/$schoolDaysThisWeek attended"
            )

            if (!snap.nextHolidayTitle.isNullOrBlank()) {
                val datePart = snap.nextHolidayDateShort?.let { " • $it" }.orEmpty()
                views.setViewVisibility(R.id.widget_upcoming_strip, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_upcoming_strip,
                    "Next Holiday: ${snap.nextHolidayTitle}$datePart"
                )
            } else {
                views.setViewVisibility(R.id.widget_upcoming_strip, View.GONE)
            }

            bindBottomActions(context, views, snap)
            return views
        }

        private fun bindSharedHeaderAndStats(
            context: Context,
            views: RemoteViews,
            snap: WidgetSnapshot,
            includeCountedDays: Boolean
        ) {
            val openPending = buildOpenAppPendingIntent(context)
            views.setOnClickPendingIntent(R.id.widget_root, openPending)
            views.setOnClickPendingIntent(R.id.widget_refresh_btn, buildRefreshPendingIntent(context))

            views.setTextViewText(R.id.widget_term_label, "80endance • ${snap.termName}")
            views.setTextViewText(R.id.widget_streak_badge, "${snap.streak}d streak")

            if (snap.daysUntilAlExam != null) {
                views.setViewVisibility(R.id.widget_al_badge, View.VISIBLE)
                val shortYear = snap.alExamYear % 100
                views.setTextViewText(R.id.widget_al_badge, "${shortYear}AL • ${snap.daysUntilAlExam}d")
            } else {
                views.setViewVisibility(R.id.widget_al_badge, View.GONE)
            }

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

            if (includeCountedDays) {
                views.setTextViewText(
                    R.id.widget_counted_days,
                    "${snap.effectiveAttended} of ${snap.totalCountedDays} counted school days"
                )
            }
        }

        private fun bindBottomActions(
            context: Context,
            views: RemoteViews,
            snap: WidgetSnapshot
        ) {
            val todayIsWeekend = DateUtils.isWeekend(snap.todayDateKey)
            val openPending = buildOpenAppPendingIntent(context)
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
        }

        private fun buildOpenAppPendingIntent(context: Context): PendingIntent {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AppConstants.EXTRA_OPEN_CHECKIN, false)
            }
            return PendingIntent.getActivity(
                context,
                8100,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun buildRefreshPendingIntent(context: Context): PendingIntent {
            val refreshIntent = Intent(context, AttendanceWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_REFRESH
            }
            return PendingIntent.getBroadcast(
                context,
                8103,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun glyphForCode(code: String): String = when (code) {
            "P" -> "✓"
            "A" -> "✕"
            "M" -> "M"
            "E" -> "E"
            "H" -> "★"
            "L" -> "●"
            "U" -> "•"
            else -> "—"
        }

        private fun bgResForDay(day: WidgetWeekDay): Int = when {
            day.statusCode in setOf("P", "M", "E") -> R.drawable.widget_day_present
            day.statusCode == "A" -> R.drawable.widget_day_absent
            day.statusCode == "H" -> R.drawable.widget_day_off
            day.statusCode == "L" -> R.drawable.widget_day_locked
            day.isToday -> R.drawable.widget_day_today
            else -> R.drawable.widget_day_neutral
        }

        private fun textColorForDay(day: WidgetWeekDay): Int = when {
            day.statusCode in setOf("P", "M", "E") -> Color.parseColor("#6EE7B7")
            day.statusCode == "A" -> Color.parseColor("#FDA4AF")
            day.statusCode == "H" -> Color.parseColor("#FDE68A")
            day.statusCode == "L" -> Color.parseColor("#C7D2FE")
            day.isToday -> Color.parseColor("#BAE6FD")
            else -> Color.parseColor("#99FFFFFF")
        }
    }
}
