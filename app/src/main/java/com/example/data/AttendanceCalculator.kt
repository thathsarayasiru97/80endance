package com.example.data

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

data class AttendanceStats(
    val percentage: Double,
    val eligible: Boolean,
    val thresholdPercent: Int,
    val presentDays: Int,
    val absentDays: Int,
    val medicalDays: Int,
    val schoolEventDays: Int,
    val holidayDays: Int,
    val priorPresentDays: Int,
    val priorAbsentDays: Int,
    val effectiveAttended: Int,
    val totalCountedDays: Int,
    val safeSkipsRemaining: Int,
    val daysNeededToRecover: Int
)

data class WeekDaySummary(
    val dayLabel: String,
    val dateKey: String,
    val status: AttendanceStatus?,
    val holidayLabel: String?,
    val isToday: Boolean
)

object AttendanceCalculator {

    fun resolveActiveTerm(terms: List<AcademicTerm>, todayKey: String = DateUtils.todayKey()): AcademicTerm {
        val effectiveTerms = terms.ifEmpty { SriLankaCalendarDefaults.defaultTerms }
        return effectiveTerms.firstOrNull { it.active }
            ?: effectiveTerms.firstOrNull { todayKey >= it.startDate && todayKey <= it.endDate }
            ?: effectiveTerms.last()
    }

    fun resolveTermIdForDate(dateKey: String, terms: List<AcademicTerm>): String {
        val effectiveTerms = terms.ifEmpty { SriLankaCalendarDefaults.defaultTerms }
        return effectiveTerms.firstOrNull { dateKey >= it.startDate && dateKey <= it.endDate }?.id
            ?: resolveActiveTerm(effectiveTerms, dateKey).id
    }

    fun calculateStats(
        records: List<AttendanceRecord>,
        thresholdPercent: Int,
        countExcusedAsPresent: Boolean,
        includePriorBalance: Boolean,
        priorPresent: Int,
        priorAbsent: Int,
        termFilter: AcademicTerm? = null
    ): AttendanceStats {
        val filtered = if (termFilter != null) {
            records.filter {
                it.termId == termFilter.id ||
                    (it.dateKey >= termFilter.startDate && it.dateKey <= termFilter.endDate)
            }
        } else {
            records
        }

        var present = 0
        var absent = 0
        var medical = 0
        var schoolEvent = 0
        var holiday = 0

        for (record in filtered) {
            when (record.statusEnum) {
                AttendanceStatus.PRESENT -> present++
                AttendanceStatus.ABSENT -> absent++
                AttendanceStatus.MEDICAL -> medical++
                AttendanceStatus.SCHOOL_EVENT -> schoolEvent++
                AttendanceStatus.HOLIDAY -> holiday++
            }
        }

        val addedPriorPresent = if (includePriorBalance) max(0, priorPresent) else 0
        val addedPriorAbsent = if (includePriorBalance) max(0, priorAbsent) else 0

        val effectiveAttended = present + addedPriorPresent +
            if (countExcusedAsPresent) (medical + schoolEvent) else 0
        val effectiveMissed = absent + addedPriorAbsent +
            if (countExcusedAsPresent) 0 else (medical + schoolEvent)

        val totalCounted = effectiveAttended + effectiveMissed
        val targetFraction = (thresholdPercent.coerceIn(50, 99)) / 100.0

        val percentage = if (totalCounted <= 0) {
            100.0
        } else {
            (effectiveAttended.toDouble() / totalCounted.toDouble()) * 100.0
        }

        val isEligible = totalCounted == 0 || percentage >= thresholdPercent.toDouble()

        val safeSkips = if (totalCounted <= 0 || !isEligible) {
            0
        } else {
            max(0, floor((effectiveAttended / targetFraction) - totalCounted).toInt())
        }

        val daysToRecover = if (isEligible || totalCounted <= 0) {
            0
        } else {
            val numerator = (targetFraction * totalCounted) - effectiveAttended
            val denominator = 1.0 - targetFraction
            max(1, ceil(numerator / denominator).toInt())
        }

        return AttendanceStats(
            percentage = percentage,
            eligible = isEligible,
            thresholdPercent = thresholdPercent,
            presentDays = present,
            absentDays = absent,
            medicalDays = medical,
            schoolEventDays = schoolEvent,
            holidayDays = holiday,
            priorPresentDays = addedPriorPresent,
            priorAbsentDays = addedPriorAbsent,
            effectiveAttended = effectiveAttended,
            totalCountedDays = totalCounted,
            safeSkipsRemaining = safeSkips,
            daysNeededToRecover = daysToRecover
        )
    }

    /**
     * Computes current consecutive school-day attendance streak, skipping weekends,
     * Sri Lankan Poya/public holidays, and logged 'holiday' days without breaking the streak.
     */
    fun calculateCurrentStreak(
        records: List<AttendanceRecord>,
        calendarEvents: List<CalendarEvent>,
        countExcusedAsPresent: Boolean,
        todayKey: String = DateUtils.todayKey()
    ): Int {
        if (records.isEmpty()) return 0
        val recordMap = records.associateBy { it.dateKey }
        val nonSchoolDates = calendarEvents.filter { it.nonSchoolDay }.map { it.dateKey }.toSet()

        var cursor = todayKey
        // If today has not been logged yet, start streak count from yesterday so it doesn't show 0 mid-day.
        if (cursor !in recordMap) {
            cursor = DateUtils.addDays(cursor, -1)
        }

        var streak = 0
        var lookbackLimit = 180
        while (lookbackLimit > 0) {
            lookbackLimit--
            if (DateUtils.isWeekend(cursor) || cursor in nonSchoolDates) {
                cursor = DateUtils.addDays(cursor, -1)
                continue
            }
            val rec = recordMap[cursor] ?: break
            when (rec.statusEnum) {
                AttendanceStatus.PRESENT -> {
                    streak++
                    cursor = DateUtils.addDays(cursor, -1)
                }
                AttendanceStatus.HOLIDAY -> {
                    cursor = DateUtils.addDays(cursor, -1)
                }
                AttendanceStatus.MEDICAL, AttendanceStatus.SCHOOL_EVENT -> {
                    if (countExcusedAsPresent) {
                        streak++
                        cursor = DateUtils.addDays(cursor, -1)
                    } else {
                        break
                    }
                }
                AttendanceStatus.ABSENT -> break
            }
        }
        return streak
    }

    fun calculateWeeklySummary(
        records: List<AttendanceRecord>,
        calendarEvents: List<CalendarEvent>,
        todayKey: String = DateUtils.todayKey()
    ): List<WeekDaySummary> {
        val recordMap = records.associateBy { it.dateKey }
        val eventMap = calendarEvents.associateBy { it.dateKey }
        return DateUtils.currentWeekMonToFri().map { (label, dateKey) ->
            val ev = eventMap[dateKey]
            WeekDaySummary(
                dayLabel = label,
                dateKey = dateKey,
                status = recordMap[dateKey]?.statusEnum,
                holidayLabel = ev?.takeIf { it.nonSchoolDay }?.title,
                isToday = dateKey == todayKey
            )
        }
    }

    fun nextUpcomingPoyaOrHoliday(
        calendarEvents: List<CalendarEvent>,
        todayKey: String = DateUtils.todayKey()
    ): CalendarEvent? {
        return calendarEvents
            .filter { it.dateKey >= todayKey && it.nonSchoolDay }
            .minByOrNull { it.dateKey }
    }

    fun daysUntilAlExam(examYear: Int, settings: AppSettings, todayKey: String = DateUtils.todayKey()): Int {
        val examDate = settings.examDateForYear(examYear)
        return max(0, DateUtils.daysBetween(todayKey, examDate))
    }

    fun isSchoolDay(
        dateKey: String,
        calendarEvents: List<CalendarEvent>
    ): Boolean {
        if (DateUtils.isWeekend(dateKey)) return false
        val holiday = calendarEvents.firstOrNull { it.dateKey == dateKey && it.nonSchoolDay }
        return holiday == null
    }

    fun filterNoticesForStudent(notices: List<Notice>, profile: UserProfile?): List<Notice> {
        val published = notices.filter { it.published }
        if (profile == null) return published.filter { it.targetTypeEnum == NoticeTargetType.ALL }

        val userStream = profile.alStream.trim()
        val userClass = profile.className.trim()

        return published
            .filter { notice ->
                when (notice.targetTypeEnum) {
                    NoticeTargetType.ALL -> true
                    NoticeTargetType.STREAM ->
                        notice.targetValue?.trim()?.equals(userStream, ignoreCase = true) == true
                    NoticeTargetType.CLASS ->
                        notice.targetValue?.trim()?.equals(userClass, ignoreCase = true) == true
                }
            }
            .sortedWith(
                compareByDescending<Notice> { it.pinned }
                    .thenByDescending { it.urgentFlag }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
    }

    fun buildShareableReport(
        profile: UserProfile,
        scopeTitle: String,
        stats: AttendanceStats,
        streak: Int
    ): String {
        val statusText = if (stats.eligible) "ELIGIBLE (>= ${stats.thresholdPercent}%)" else "AT RISK (< ${stats.thresholdPercent}%)"
        val guidance = if (stats.eligible) {
            "Safe buffer: up to ${stats.safeSkipsRemaining} more skip(s) available."
        } else {
            "Recovery needed: attend the next ${stats.daysNeededToRecover} consecutive school day(s)."
        }
        return buildString {
            appendLine("80endance — GCE A/L Attendance Report")
            appendLine("Student: ${profile.fullName} (${profile.className} • ${profile.alStream})")
            appendLine("School: ${profile.schoolName}")
            appendLine("Scope: $scopeTitle")
            appendLine("--------------------------------")
            appendLine("Attendance: ${String.format("%.1f", stats.percentage)}% — $statusText")
            appendLine("Present: ${stats.presentDays} | Absent: ${stats.absentDays}")
            appendLine("Medical Excused: ${stats.medicalDays} | School Events: ${stats.schoolEventDays}")
            if (stats.priorPresentDays > 0 || stats.priorAbsentDays > 0) {
                appendLine("Prior Balance Included: +${stats.priorPresentDays} Present / +${stats.priorAbsentDays} Absent")
            }
            appendLine("Counted Days: ${stats.effectiveAttended} / ${stats.totalCountedDays}")
            appendLine("Current Streak: $streak school day(s)")
            appendLine(guidance)
        }
    }
}
