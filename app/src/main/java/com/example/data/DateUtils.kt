package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object DateUtils {
    private val isoFormat get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val readableDayFormat get() = SimpleDateFormat("EEE, d MMM", Locale.US)
    private val fullDateFormat get() = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US)
    private val monthYearFormat get() = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val shortMonthFormat get() = SimpleDateFormat("MMM", Locale.US)
    private val monthNameFormat get() = SimpleDateFormat("MMMM", Locale.US)
    private val weekdayFormat get() = SimpleDateFormat("EEEE", Locale.US)

    fun todayKey(): String = formatDateKey(Calendar.getInstance())

    fun formatDateKey(cal: Calendar): String = isoFormat.format(cal.time)

    fun formatDateKey(year: Int, monthOneBased: Int, day: Int): String =
        String.format(Locale.US, "%04d-%02d-%02d", year, monthOneBased, day)

    fun parseDateKey(dateKey: String): Calendar? {
        if (dateKey.length != 10) return null
        return try {
            val date: Date = isoFormat.parse(dateKey) ?: return null
            Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun formatReadableShort(dateKey: String): String {
        val cal = parseDateKey(dateKey) ?: return dateKey
        return readableDayFormat.format(cal.time)
    }

    fun formatReadableFull(dateKey: String): String {
        val cal = parseDateKey(dateKey) ?: return dateKey
        return fullDateFormat.format(cal.time)
    }

    fun formatMonthYear(year: Int, monthOneBased: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOneBased - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return monthYearFormat.format(cal.time)
    }

    fun currentYearMonth(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    fun currentMonthPrefix(referenceDateKey: String = todayKey()): String {
        return if (referenceDateKey.length >= 7) {
            referenceDateKey.substring(0, 7) + "-"
        } else {
            val (y, m) = currentYearMonth()
            String.format(Locale.US, "%04d-%02d-", y, m)
        }
    }

    fun currentMonthName(referenceDateKey: String = todayKey()): String {
        val cal = parseDateKey(referenceDateKey) ?: Calendar.getInstance()
        return monthNameFormat.format(cal.time)
    }

    fun currentMonthYearLabel(referenceDateKey: String = todayKey()): String {
        val cal = parseDateKey(referenceDateKey) ?: Calendar.getInstance()
        return monthYearFormat.format(cal.time)
    }

    fun formatWeekdayName(dateKey: String): String {
        val cal = parseDateKey(dateKey) ?: return ""
        return weekdayFormat.format(cal.time)
    }

    fun currentHourMinute(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
    }

    fun isWeekend(dateKey: String): Boolean {
        val cal = parseDateKey(dateKey) ?: return false
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }

    fun addDays(dateKey: String, delta: Int): String {
        val cal = parseDateKey(dateKey) ?: return dateKey
        cal.add(Calendar.DAY_OF_YEAR, delta)
        return formatDateKey(cal)
    }

    fun daysBetween(startDateKey: String, endDateKey: String): Int {
        val start = parseDateKey(startDateKey) ?: return 0
        val end = parseDateKey(endDateKey) ?: return 0
        val diffMs = end.timeInMillis - start.timeInMillis
        return (diffMs.toDouble() / 86_400_000.0).roundToInt()
    }

    /** Returns the most recent Friday strictly before [referenceDateKey]. */
    fun lastFridayKey(referenceDateKey: String = todayKey()): String {
        val cal = parseDateKey(referenceDateKey) ?: Calendar.getInstance()
        val delta = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> -1
            Calendar.SUNDAY -> -2
            Calendar.MONDAY -> -3
            Calendar.TUESDAY -> -4
            Calendar.WEDNESDAY -> -5
            Calendar.THURSDAY -> -6
            else -> -7 // Friday -> previous week's Friday
        }
        cal.add(Calendar.DAY_OF_YEAR, delta)
        return formatDateKey(cal)
    }

    /** Returns the last calendar day of the previous month relative to [referenceDateKey]. */
    fun endOfPreviousMonthKey(referenceDateKey: String = todayKey()): String {
        val cal = parseDateKey(referenceDateKey) ?: Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return formatDateKey(cal)
    }

    /**
     * Expands an inclusive [startDateKey..endDateKey] date range into ISO date keys,
     * optionally skipping Saturdays and Sundays, bounded to [maxDays].
     */
    fun expandDateRange(
        startDateKey: String,
        endDateKey: String,
        skipWeekends: Boolean = true,
        maxDays: Int = 60
    ): List<String> {
        val start = parseDateKey(startDateKey) ?: return emptyList()
        val end = parseDateKey(endDateKey) ?: return emptyList()
        if (end.before(start)) return emptyList()
        val result = mutableListOf<String>()
        val cursor = start.clone() as Calendar
        var steps = 0
        while (!cursor.after(end) && result.size < maxDays && steps < 180) {
            val dow = cursor.get(Calendar.DAY_OF_WEEK)
            val weekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
            if (!skipWeekends || !weekend) {
                result.add(formatDateKey(cursor))
            }
            cursor.add(Calendar.DAY_OF_YEAR, 1)
            steps++
        }
        return result
    }

    /** Returns 0 for Monday .. 4 for Friday, or -1 for weekend/invalid. */
    fun weekdayIndexMonToFri(dateKey: String): Int {
        val cal = parseDateKey(dateKey) ?: return -1
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            else -> -1
        }
    }

    /** Formats "2026-10" into "Oct" (or "Oct '26" if [includeYear]). */
    fun formatShortMonth(yearMonthKey: String, includeYear: Boolean = false): String {
        val cal = parseDateKey("$yearMonthKey-01") ?: return yearMonthKey
        val month = shortMonthFormat.format(cal.time)
        return if (includeYear) "$month '${yearMonthKey.substring(2, 4)}" else month
    }

    fun daysInMonth(year: Int, monthOneBased: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOneBased - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /** Returns 0 for Monday .. 6 for Sunday for the 1st of the given month. */
    fun firstDayOfWeekMonZero(year: Int, monthOneBased: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOneBased - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> 6
        }
    }

    /** Returns the Mon..Fri date keys for the week containing today. */
    fun currentWeekMonToFri(): List<Pair<String, String>> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val offsetToMon = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> -1
            Calendar.WEDNESDAY -> -2
            Calendar.THURSDAY -> -3
            Calendar.FRIDAY -> -4
            Calendar.SATURDAY -> -5
            else -> -6
        }
        cal.add(Calendar.DAY_OF_YEAR, offsetToMon)
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
        return labels.map { label ->
            val key = formatDateKey(cal)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            label to key
        }
    }

    /** Returns Calendar.DAY_OF_WEEK (1=Sunday, 2=Monday .. 7=Saturday) for [dateKey]. */
    fun dayOfWeekNumber(dateKey: String): Int {
        val cal = parseDateKey(dateKey) ?: Calendar.getInstance()
        return cal.get(Calendar.DAY_OF_WEEK)
    }

    /** Returns the ISO date key for the Monday of the week containing [dateKey] (Mon..Sun week). */
    fun mondayOfWeek(dateKey: String): String {
        val cal = parseDateKey(dateKey) ?: Calendar.getInstance()
        val offsetToMon = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> -1
            Calendar.WEDNESDAY -> -2
            Calendar.THURSDAY -> -3
            Calendar.FRIDAY -> -4
            Calendar.SATURDAY -> -5
            else -> -6
        }
        cal.add(Calendar.DAY_OF_YEAR, offsetToMon)
        return formatDateKey(cal)
    }

    fun formatTime12Hour(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
    }

    fun formatClockTime(millis: Long): String {
        val fmt = SimpleDateFormat("h:mm a", Locale.US)
        return fmt.format(Date(millis))
    }

    fun formatLastSeen(
        timestampMillis: Long?,
        isOnline: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        if (isOnline) return "Online now"
        if (timestampMillis == null || timestampMillis <= 0L) return "Not seen yet"
        val diffMs = (nowMillis - timestampMillis).coerceAtLeast(0L)
        val minutes = diffMs / 60_000L
        val hours = diffMs / 3_600_000L
        val days = diffMs / 86_400_000L
        return when {
            minutes < 1L -> "Last seen just now"
            minutes < 60L -> "Last seen ${minutes}m ago"
            hours < 24L -> "Last seen ${hours}h ago"
            days < 7L -> "Last seen ${days}d ago"
            else -> {
                val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.US)
                "Last seen ${fmt.format(Date(timestampMillis))}"
            }
        }
    }
}
