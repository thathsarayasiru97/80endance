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

    fun formatTime12Hour(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
    }
}
