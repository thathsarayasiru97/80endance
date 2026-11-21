package com.example.data

import kotlin.math.max
import kotlin.math.roundToInt

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
    val isToday: Boolean,
    val isFuture: Boolean = false,
    val isLocked: Boolean = false
)

data class PercentageBaselineEstimate(
    val startDateKey: String,
    val endDateKey: String,
    val totalSchoolDays: Int,
    val presentDays: Int,
    val absentDays: Int,
    val enteredPercent: Double,
    val effectivePercent: Double
)

data class SkipSimulationResult(
    val extraSkips: Int,
    val extraAttends: Int,
    val projectedAttended: Int,
    val projectedTotal: Int,
    val projectedPercent: Double,
    val percentDelta: Double,
    val willStayEligible: Boolean,
    val remainingSafeSkips: Int,
    val daysNeededToRecover: Int
)

data class MonthlyAttendancePoint(
    val yearMonthKey: String,
    val shortLabel: String,
    val fullLabel: String,
    val percentage: Double,
    val attendedDays: Int,
    val totalDays: Int
)

data class WeekdayStat(
    val dayIndex: Int,
    val shortName: String,
    val fullName: String,
    val attendedDays: Int,
    val absentDays: Int,
    val totalDays: Int,
    val percentage: Double
)

data class AttendancePatternInsights(
    val monthlyPoints: List<MonthlyAttendancePoint>,
    val weekdayStats: List<WeekdayStat>,
    val mostSkippedWeekday: WeekdayStat?,
    val bestWeekday: WeekdayStat?,
    val bestMonth: MonthlyAttendancePoint?
)

data class AttendanceMilestone(
    val id: String,
    val title: String,
    val subtitle: String,
    val unlocked: Boolean,
    val progressText: String,
    val progressFraction: Float,
    val iconKey: String
)

data class UpcomingWeekPrep(
    val mondayKey: String,
    val fridayKey: String,
    val schoolDaysCount: Int,
    val offDays: List<CalendarEvent>,
    val termPercentage: Double,
    val eligible: Boolean,
    val safeSkipsRemaining: Int,
    val daysNeededToRecover: Int
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
        countExcusedAsPresent: Boolean = true,
        includePriorBalance: Boolean,
        priorPresent: Int,
        priorAbsent: Int,
        termFilter: AcademicTerm? = null,
        monthPrefixFilter: String? = null,
        lockedRange: Pair<String, String>? = null
    ): AttendanceStats {
        val filtered = when {
            !monthPrefixFilter.isNullOrBlank() -> {
                records.filter { it.dateKey.startsWith(monthPrefixFilter) }
            }
            termFilter != null -> {
                records.filter {
                    it.termId == termFilter.id ||
                        (it.dateKey >= termFilter.startDate && it.dateKey <= termFilter.endDate)
                }
            }
            else -> records
        }

        var present = 0
        var absent = 0
        var medical = 0
        var schoolEvent = 0
        var holiday = 0

        for (record in filtered) {
            if (DateUtils.isWeekend(record.dateKey)) continue
            if (includePriorBalance && lockedRange != null &&
                record.dateKey >= lockedRange.first && record.dateKey <= lockedRange.second
            ) {
                continue
            }
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

        // Per college policy, both Excused (sports/clubs/events) and Medical count as Present
        val effectiveAttended = present + addedPriorPresent + medical + schoolEvent
        val effectiveMissed = absent + addedPriorAbsent

        val totalCounted = effectiveAttended + effectiveMissed
        val threshold = thresholdPercent.coerceIn(50, 99)

        val percentage = if (totalCounted <= 0) {
            100.0
        } else {
            (effectiveAttended.toDouble() / totalCounted.toDouble()) * 100.0
        }

        val isEligible = totalCounted == 0 || (effectiveAttended * 100 >= threshold * totalCounted)

        val safeSkips = if (totalCounted <= 0 || !isEligible) {
            0
        } else {
            max(0, ((effectiveAttended * 100) - (threshold * totalCounted)) / threshold)
        }

        val daysToRecover = if (isEligible || totalCounted <= 0) {
            0
        } else {
            val numerator = (threshold * totalCounted) - (effectiveAttended * 100)
            val denominator = 100 - threshold
            max(1, (numerator + denominator - 1) / denominator)
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
                    streak++
                    cursor = DateUtils.addDays(cursor, -1)
                }
                AttendanceStatus.ABSENT -> break
            }
        }
        return streak
    }

    fun calculateWeeklySummary(
        records: List<AttendanceRecord>,
        calendarEvents: List<CalendarEvent>,
        todayKey: String = DateUtils.todayKey(),
        lockedRange: Pair<String, String>? = null
    ): List<WeekDaySummary> {
        val recordMap = records.associateBy { it.dateKey }
        val eventsByDate = calendarEvents.groupBy { it.dateKey }
        return DateUtils.currentWeekMonToFri().map { (label, dateKey) ->
            val nonSchoolEv = eventsByDate[dateKey]?.firstOrNull { it.nonSchoolDay }
            val locked = lockedRange != null && dateKey >= lockedRange.first && dateKey <= lockedRange.second
            WeekDaySummary(
                dayLabel = label,
                dateKey = dateKey,
                status = if (locked) null else recordMap[dateKey]?.statusEnum,
                holidayLabel = nonSchoolEv?.title,
                isToday = dateKey == todayKey,
                isFuture = dateKey > todayKey,
                isLocked = locked
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

    fun filterCalendarEventsForStudent(
        events: List<CalendarEvent>,
        profile: UserProfile?,
        userId: String = profile?.userId.orEmpty()
    ): List<CalendarEvent> {
        return events.filter { it.published && it.appliesToStudent(profile, userId) }
    }

    /**
     * Counts school weekdays in the inclusive range [startDateKey..endDateKey],
     * excluding Saturdays, Sundays, and published non-school calendar events (Poya/holidays).
     */
    fun countSchoolDaysInRange(
        startDateKey: String,
        endDateKey: String,
        calendarEvents: List<CalendarEvent>,
        maxDays: Int = 600
    ): Int {
        val start = DateUtils.parseDateKey(startDateKey) ?: return 0
        val end = DateUtils.parseDateKey(endDateKey) ?: return 0
        if (end.before(start)) return 0
        val nonSchoolDates = calendarEvents.filter { it.nonSchoolDay }.map { it.dateKey }.toSet()
        val cursor = start.clone() as java.util.Calendar
        var count = 0
        var guard = 0
        while (!cursor.after(end) && count < maxDays && guard < 1200) {
            val dow = cursor.get(java.util.Calendar.DAY_OF_WEEK)
            val weekend = dow == java.util.Calendar.SATURDAY || dow == java.util.Calendar.SUNDAY
            if (!weekend) {
                val key = DateUtils.formatDateKey(cursor)
                if (key !in nonSchoolDates) {
                    count++
                }
            }
            cursor.add(java.util.Calendar.DAY_OF_YEAR, 1)
            guard++
        }
        return count
    }

    /**
     * Converts an attendance percentage covering [startDateKey..endDateKey] into integer
     * present & absent counts based on detected school days (or [customSchoolDaysOverride]).
     */
    fun estimateBaselineFromPercentage(
        startDateKey: String,
        endDateKey: String,
        percentage: Double,
        calendarEvents: List<CalendarEvent>,
        customSchoolDaysOverride: Int? = null
    ): PercentageBaselineEstimate {
        val clampedPct = percentage.coerceIn(0.0, 100.0)
        val detectedDays = countSchoolDaysInRange(startDateKey, endDateKey, calendarEvents)
        val totalDays = (customSchoolDaysOverride ?: detectedDays).coerceIn(0, 600)
        val present = if (totalDays <= 0) {
            0
        } else {
            ((clampedPct / 100.0) * totalDays).roundToInt().coerceIn(0, totalDays)
        }
        val absent = (totalDays - present).coerceIn(0, 600)
        val effectivePct = if (totalDays <= 0) 0.0 else (present.toDouble() / totalDays.toDouble()) * 100.0
        return PercentageBaselineEstimate(
            startDateKey = startDateKey,
            endDateKey = endDateKey,
            totalSchoolDays = totalDays,
            presentDays = present,
            absentDays = absent,
            enteredPercent = clampedPct,
            effectivePercent = effectivePct
        )
    }

    /**
     * Finds recent past weekdays (before [todayKey]) that are school days for this student
     * and do not yet have an attendance record (and are not locked inside an imported baseline),
     * so HomeScreen can offer 1-tap catch-up.
     */
    fun findUnloggedRecentSchoolDays(
        records: List<AttendanceRecord>,
        calendarEvents: List<CalendarEvent>,
        activeTermStartDate: String,
        todayKey: String = DateUtils.todayKey(),
        lookbackCalendarDays: Int = 10,
        maxItems: Int = 5,
        lockedRange: Pair<String, String>? = null
    ): List<String> {
        val loggedDates = records.map { it.dateKey }.toSet()
        val nonSchoolDates = calendarEvents.filter { it.nonSchoolDay }.map { it.dateKey }.toSet()
        val missing = mutableListOf<String>()
        for (offset in 1..lookbackCalendarDays) {
            val candidate = DateUtils.addDays(todayKey, -offset)
            if (activeTermStartDate.length == 10 && candidate < activeTermStartDate) break
            if (lockedRange != null && candidate >= lockedRange.first && candidate <= lockedRange.second) continue
            if (DateUtils.isWeekend(candidate)) continue
            if (candidate in nonSchoolDates) continue
            if (candidate !in loggedDates) {
                missing.add(candidate)
                if (missing.size >= maxItems) break
            }
        }
        return missing
    }

    /**
     * Interactive "What-If" simulator projecting Term or Overall attendance after
     * skipping [extraSkips] future school days or attending [extraAttends] future school days.
     */
    fun simulateFutureSkips(
        stats: AttendanceStats,
        extraSkips: Int,
        extraAttends: Int = 0
    ): SkipSimulationResult {
        val skips = max(0, extraSkips)
        val attends = max(0, extraAttends)
        val projectedAttended = stats.effectiveAttended + attends
        val projectedTotal = stats.totalCountedDays + skips + attends
        val threshold = stats.thresholdPercent.coerceIn(50, 99)

        val projectedPercent = if (projectedTotal <= 0) {
            100.0
        } else {
            (projectedAttended.toDouble() / projectedTotal.toDouble()) * 100.0
        }
        val willStayEligible = projectedTotal == 0 || (projectedAttended * 100 >= threshold * projectedTotal)
        val remainingSafeSkips = if (projectedTotal <= 0 || !willStayEligible) {
            0
        } else {
            max(0, ((projectedAttended * 100) - (threshold * projectedTotal)) / threshold)
        }
        val daysNeededToRecover = if (willStayEligible || projectedTotal <= 0) {
            0
        } else {
            val numerator = (threshold * projectedTotal) - (projectedAttended * 100)
            val denominator = 100 - threshold
            max(1, (numerator + denominator - 1) / denominator)
        }

        return SkipSimulationResult(
            extraSkips = skips,
            extraAttends = attends,
            projectedAttended = projectedAttended,
            projectedTotal = projectedTotal,
            projectedPercent = projectedPercent,
            percentDelta = projectedPercent - stats.percentage,
            willStayEligible = willStayEligible,
            remainingSafeSkips = remainingSafeSkips,
            daysNeededToRecover = daysNeededToRecover
        )
    }

    /**
     * Builds monthly attendance trend points (chronological, up to [maxMonths]) and
     * Monday–Friday weekday habit insights from logged school-day records.
     */
    fun calculateAttendanceInsights(
        records: List<AttendanceRecord>,
        maxMonths: Int = 6
    ): AttendancePatternInsights {
        val validRecords = records.filter { rec ->
            rec.dateKey.length == 10 &&
                !DateUtils.isWeekend(rec.dateKey) &&
                rec.statusEnum != AttendanceStatus.HOLIDAY
        }

        val monthlyPoints = validRecords
            .groupBy { it.dateKey.substring(0, 7) }
            .toSortedMap()
            .entries
            .toList()
            .takeLast(maxMonths)
            .mapNotNull { (ym, recs) ->
                val attended = recs.count {
                    it.statusEnum == AttendanceStatus.PRESENT ||
                        it.statusEnum == AttendanceStatus.MEDICAL ||
                        it.statusEnum == AttendanceStatus.SCHOOL_EVENT
                }
                val total = recs.count {
                    it.statusEnum == AttendanceStatus.PRESENT ||
                        it.statusEnum == AttendanceStatus.MEDICAL ||
                        it.statusEnum == AttendanceStatus.SCHOOL_EVENT ||
                        it.statusEnum == AttendanceStatus.ABSENT
                }
                if (total <= 0) null else {
                    MonthlyAttendancePoint(
                        yearMonthKey = ym,
                        shortLabel = DateUtils.formatShortMonth(ym),
                        fullLabel = DateUtils.currentMonthYearLabel("$ym-01"),
                        percentage = (attended.toDouble() / total.toDouble()) * 100.0,
                        attendedDays = attended,
                        totalDays = total
                    )
                }
            }

        val shortNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
        val fullNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        val attendedByDay = IntArray(5)
        val absentByDay = IntArray(5)

        validRecords.forEach { rec ->
            val dow = DateUtils.weekdayIndexMonToFri(rec.dateKey)
            if (dow in 0..4) {
                when (rec.statusEnum) {
                    AttendanceStatus.PRESENT,
                    AttendanceStatus.MEDICAL,
                    AttendanceStatus.SCHOOL_EVENT -> attendedByDay[dow]++
                    AttendanceStatus.ABSENT -> absentByDay[dow]++
                    AttendanceStatus.HOLIDAY -> Unit
                }
            }
        }

        val weekdayStats = (0..4).map { idx ->
            val att = attendedByDay[idx]
            val abs = absentByDay[idx]
            val tot = att + abs
            val pct = if (tot <= 0) 0.0 else (att.toDouble() / tot.toDouble()) * 100.0
            WeekdayStat(
                dayIndex = idx,
                shortName = shortNames[idx],
                fullName = fullNames[idx],
                attendedDays = att,
                absentDays = abs,
                totalDays = tot,
                percentage = pct
            )
        }

        val activeWeekdays = weekdayStats.filter { it.totalDays > 0 }
        val mostSkipped = activeWeekdays
            .filter { it.absentDays > 0 }
            .maxWithOrNull(compareBy<WeekdayStat> { it.absentDays }.thenBy { -it.percentage })
        val bestWeekday = activeWeekdays
            .maxWithOrNull(compareBy<WeekdayStat> { it.percentage }.thenBy { it.totalDays })
        val bestMonth = monthlyPoints
            .maxWithOrNull(compareBy<MonthlyAttendancePoint> { it.percentage }.thenBy { it.totalDays })

        return AttendancePatternInsights(
            monthlyPoints = monthlyPoints,
            weekdayStats = weekdayStats,
            mostSkippedWeekday = mostSkipped,
            bestWeekday = bestWeekday,
            bestMonth = bestMonth
        )
    }

    fun filterNoticesForStudent(notices: List<Notice>, profile: UserProfile?): List<Notice> {
        val published = notices.filter { it.published }
        if (profile == null) return published.filter { it.targetTypeEnum == NoticeTargetType.ALL }

        val userGrade = profile.grade.trim()
        val userStream = profile.alStream.trim()
        val userClass = profile.className.trim()

        return published
            .filter { notice ->
                when (notice.targetTypeEnum) {
                    NoticeTargetType.ALL -> true
                    NoticeTargetType.GRADE ->
                        notice.targetValue?.trim()?.equals(userGrade, ignoreCase = true) == true
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
            appendLine("Medical: ${stats.medicalDays} | Excused: ${stats.schoolEventDays}")
            if (stats.priorPresentDays > 0 || stats.priorAbsentDays > 0) {
                appendLine("Prior Balance Included: +${stats.priorPresentDays} Present / +${stats.priorAbsentDays} Absent")
            }
            appendLine("Counted Days: ${stats.effectiveAttended} / ${stats.totalCountedDays}")
            appendLine("Current Streak: $streak school day(s)")
            appendLine(guidance)
        }
    }

    /**
     * Computes visual milestone badges and unlock progress for the Reports milestone shelf.
     */
    fun calculateMilestones(
        records: List<AttendanceRecord>,
        currentStreak: Int,
        termStats: AttendanceStats,
        overallStats: AttendanceStats
    ): List<AttendanceMilestone> {
        val insights = calculateAttendanceInsights(records, maxMonths = 24)
        val loggedCount = records.count {
            !DateUtils.isWeekend(it.dateKey) && it.statusEnum != AttendanceStatus.HOLIDAY
        }
        val bestPerfectMonth = insights.monthlyPoints
            .filter { it.totalDays >= 5 }
            .maxByOrNull { it.percentage }
        val hasPerfectMonth = bestPerfectMonth != null && bestPerfectMonth.percentage >= 99.95

        val safeZoneUnlocked = termStats.totalCountedDays >= 5 && termStats.eligible
        val cleanSheetDays = overallStats.totalCountedDays
        val totalUnexcusedAbsent = overallStats.absentDays + overallStats.priorAbsentDays
        val cleanSheetUnlocked = cleanSheetDays >= 10 && totalUnexcusedAbsent == 0

        return listOf(
            AttendanceMilestone(
                id = "streak_5",
                title = "5-Day Streak",
                subtitle = "Attend 5 school days in a row",
                unlocked = currentStreak >= 5,
                progressText = if (currentStreak >= 5) "Unlocked ($currentStreak d)" else "$currentStreak / 5 days",
                progressFraction = (currentStreak / 5f).coerceIn(0f, 1f),
                iconKey = "flame"
            ),
            AttendanceMilestone(
                id = "streak_10",
                title = "10-Day Unstoppable",
                subtitle = "Maintain a 10-day consecutive streak",
                unlocked = currentStreak >= 10,
                progressText = if (currentStreak >= 10) "Unlocked ($currentStreak d)" else "$currentStreak / 10 days",
                progressFraction = (currentStreak / 10f).coerceIn(0f, 1f),
                iconKey = "bolt"
            ),
            AttendanceMilestone(
                id = "safe_zone_80",
                title = "${termStats.thresholdPercent}% Safe Zone",
                subtitle = "Hold eligibility across 5+ term days",
                unlocked = safeZoneUnlocked,
                progressText = when {
                    safeZoneUnlocked -> String.format(java.util.Locale.US, "%.1f%% Eligible", termStats.percentage)
                    termStats.totalCountedDays < 5 -> "${termStats.totalCountedDays} / 5 days"
                    else -> String.format(java.util.Locale.US, "%.1f%% / %d%%", termStats.percentage, termStats.thresholdPercent)
                },
                progressFraction = if (termStats.totalCountedDays < 5) {
                    (termStats.totalCountedDays / 5f).coerceIn(0f, 1f)
                } else {
                    (termStats.percentage.toFloat() / termStats.thresholdPercent.toFloat()).coerceIn(0f, 1f)
                },
                iconKey = "shield"
            ),
            AttendanceMilestone(
                id = "perfect_month",
                title = "Perfect Month",
                subtitle = "Hit 100% in any month (5+ school days)",
                unlocked = hasPerfectMonth,
                progressText = when {
                    hasPerfectMonth -> "100% in ${bestPerfectMonth.shortLabel}"
                    bestPerfectMonth != null -> String.format(java.util.Locale.US, "Best: %.0f%% (%s)", bestPerfectMonth.percentage, bestPerfectMonth.shortLabel)
                    else -> "Log 5+ days in a month"
                },
                progressFraction = if (hasPerfectMonth) 1f else ((bestPerfectMonth?.percentage ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f),
                iconKey = "star"
            ),
            AttendanceMilestone(
                id = "consistent_logger",
                title = "Consistent Logger",
                subtitle = "Log 20 or more school days",
                unlocked = loggedCount >= 20,
                progressText = if (loggedCount >= 20) "$loggedCount days logged" else "$loggedCount / 20 days",
                progressFraction = (loggedCount / 20f).coerceIn(0f, 1f),
                iconKey = "calendar"
            ),
            AttendanceMilestone(
                id = "clean_sheet",
                title = "Zero Unexcused",
                subtitle = "0 unexcused absences across 10+ days",
                unlocked = cleanSheetUnlocked,
                progressText = when {
                    cleanSheetUnlocked -> "$cleanSheetDays clean days"
                    totalUnexcusedAbsent > 0 -> "$totalUnexcusedAbsent unexcused"
                    else -> "$cleanSheetDays / 10 days"
                },
                progressFraction = if (totalUnexcusedAbsent > 0) 0f else (cleanSheetDays / 10f).coerceIn(0f, 1f),
                iconKey = "verified"
            )
        )
    }

    /**
     * Summarizes the upcoming Monday–Friday school week relative to [referenceDateKey]
     * (typically Sunday evening) for the Smart Sunday Evening Prep notification.
     */
    fun calculateUpcomingWeekPrep(
        calendarEvents: List<CalendarEvent>,
        termStats: AttendanceStats,
        referenceDateKey: String = DateUtils.todayKey()
    ): UpcomingWeekPrep {
        val cal = DateUtils.parseDateKey(referenceDateKey) ?: java.util.Calendar.getInstance()
        val daysUntilMonday = when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SUNDAY -> 1
            java.util.Calendar.SATURDAY -> 2
            java.util.Calendar.FRIDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.WEDNESDAY -> 5
            java.util.Calendar.TUESDAY -> 6
            else -> 7
        }
        val mondayKey = DateUtils.addDays(referenceDateKey, daysUntilMonday)
        val weekKeys = (0..4).map { DateUtils.addDays(mondayKey, it) }
        val fridayKey = weekKeys.last()

        val eventByDate = calendarEvents
            .filter { it.nonSchoolDay }
            .associateBy { it.dateKey }
        val offDays = weekKeys.mapNotNull { eventByDate[it] }
        val schoolDays = (5 - offDays.size).coerceIn(0, 5)

        return UpcomingWeekPrep(
            mondayKey = mondayKey,
            fridayKey = fridayKey,
            schoolDaysCount = schoolDays,
            offDays = offDays,
            termPercentage = termStats.percentage,
            eligible = termStats.eligible,
            safeSkipsRemaining = termStats.safeSkipsRemaining,
            daysNeededToRecover = termStats.daysNeededToRecover
        )
    }
}
