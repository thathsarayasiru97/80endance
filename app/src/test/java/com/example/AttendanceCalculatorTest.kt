package com.example

import com.example.data.AttendanceCalculator
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStatus
import com.example.data.CalendarEvent
import com.example.data.CalendarEventType
import com.example.data.DateUtils
import com.example.data.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceCalculatorTest {

    @Test
    fun excusedAndMedicalAlwaysCountAsPresent() {
        val records = listOf(
            AttendanceRecord(dateKey = "2026-05-04", status = AttendanceStatus.PRESENT.raw),      // Mon
            AttendanceRecord(dateKey = "2026-05-05", status = AttendanceStatus.SCHOOL_EVENT.raw), // Tue (Excused)
            AttendanceRecord(dateKey = "2026-05-06", status = AttendanceStatus.MEDICAL.raw),      // Wed (Medical)
            AttendanceRecord(dateKey = "2026-05-07", status = AttendanceStatus.ABSENT.raw)        // Thu
        )

        val stats = AttendanceCalculator.calculateStats(
            records = records,
            thresholdPercent = 80,
            countExcusedAsPresent = false, // Even if false is passed, Excused & Medical must count as Present
            includePriorBalance = false,
            priorPresent = 0,
            priorAbsent = 0
        )

        assertEquals(1, stats.presentDays)
        assertEquals(1, stats.schoolEventDays)
        assertEquals(1, stats.medicalDays)
        assertEquals(1, stats.absentDays)
        assertEquals(3, stats.effectiveAttended)
        assertEquals(4, stats.totalCountedDays)
        assertEquals(75.0, stats.percentage, 0.01)
        assertFalse(stats.eligible)
    }

    @Test
    fun userProfileQuickOverallPercentAlwaysCountsExcusedAndMedical() {
        val profile = UserProfile(
            presentCount = 10,
            schoolEventCount = 3,
            medicalCount = 2,
            absentCount = 5,
            countExcusedAsPresent = false
        )
        // (10 + 3 + 2) / 20 = 75.0%
        assertEquals(75.0, profile.quickOverallPercent, 0.01)
    }

    @Test
    fun weekendDatesAreIgnoredInStatsAndFlaggedByDateUtils() {
        assertTrue(DateUtils.isWeekend("2026-05-09"))  // Saturday
        assertTrue(DateUtils.isWeekend("2026-05-10"))  // Sunday
        assertFalse(DateUtils.isWeekend("2026-05-11")) // Monday

        val records = listOf(
            AttendanceRecord(dateKey = "2026-05-08", status = AttendanceStatus.PRESENT.raw), // Fri
            AttendanceRecord(dateKey = "2026-05-09", status = AttendanceStatus.ABSENT.raw),  // Sat (ignored)
            AttendanceRecord(dateKey = "2026-05-10", status = AttendanceStatus.ABSENT.raw),  // Sun (ignored)
            AttendanceRecord(dateKey = "2026-05-11", status = AttendanceStatus.PRESENT.raw)  // Mon
        )

        val stats = AttendanceCalculator.calculateStats(
            records = records,
            thresholdPercent = 80,
            includePriorBalance = false,
            priorPresent = 0,
            priorAbsent = 0
        )
        assertEquals(2, stats.totalCountedDays)
        assertEquals(2, stats.effectiveAttended)
        assertEquals(100.0, stats.percentage, 0.01)
    }

    @Test
    fun monthPrefixFilterScopesToSingleMonthAndExcludesPriorBalance() {
        val records = listOf(
            AttendanceRecord(dateKey = "2026-04-28", status = AttendanceStatus.ABSENT.raw),
            AttendanceRecord(dateKey = "2026-05-04", status = AttendanceStatus.PRESENT.raw),
            AttendanceRecord(dateKey = "2026-05-05", status = AttendanceStatus.SCHOOL_EVENT.raw),
            AttendanceRecord(dateKey = "2026-05-06", status = AttendanceStatus.MEDICAL.raw),
            AttendanceRecord(dateKey = "2026-05-07", status = AttendanceStatus.PRESENT.raw)
        )

        val mayStats = AttendanceCalculator.calculateStats(
            records = records,
            thresholdPercent = 80,
            includePriorBalance = false,
            priorPresent = 50,
            priorAbsent = 20,
            monthPrefixFilter = "2026-05-"
        )

        assertEquals(4, mayStats.totalCountedDays)
        assertEquals(4, mayStats.effectiveAttended)
        assertEquals(100.0, mayStats.percentage, 0.01)
        assertTrue(mayStats.eligible)
    }

    @Test
    fun streakContinuesAcrossExcusedAndMedicalDays() {
        val records = listOf(
            AttendanceRecord(dateKey = "2026-05-04", status = AttendanceStatus.PRESENT.raw),
            AttendanceRecord(dateKey = "2026-05-05", status = AttendanceStatus.SCHOOL_EVENT.raw),
            AttendanceRecord(dateKey = "2026-05-06", status = AttendanceStatus.MEDICAL.raw),
            AttendanceRecord(dateKey = "2026-05-07", status = AttendanceStatus.PRESENT.raw)
        )

        val streak = AttendanceCalculator.calculateCurrentStreak(
            records = records,
            calendarEvents = emptyList(),
            countExcusedAsPresent = false,
            todayKey = "2026-05-07"
        )
        assertEquals(4, streak)
    }

    @Test
    fun expandDateRangeSkipsWeekendsWhenRequested() {
        // 2026-05-07 (Thu) .. 2026-05-12 (Tue) -> skips Sat 09 and Sun 10
        val weekdays = DateUtils.expandDateRange("2026-05-07", "2026-05-12", skipWeekends = true)
        assertEquals(listOf("2026-05-07", "2026-05-08", "2026-05-11", "2026-05-12"), weekdays)

        val allDays = DateUtils.expandDateRange("2026-05-07", "2026-05-12", skipWeekends = false)
        assertEquals(6, allDays.size)
    }

    @Test
    fun classAndGradeSpecificNoSchoolEventsFilterCorrectly() {
        val student13M2 = UserProfile(grade = "Grade 13", alStream = "Physical Science", className = "13M2")
        val student12B1 = UserProfile(grade = "Grade 12", alStream = "Bio Science", className = "12B1")

        val events = listOf(
            com.example.data.CalendarEvent(
                id = "all_1",
                title = "Poya Day",
                dateKey = "2026-05-01",
                eventType = com.example.data.CalendarEventType.POYA.raw,
                targetType = com.example.data.CalendarEventTargetType.ALL.raw
            ),
            com.example.data.CalendarEvent(
                id = "grade13_1",
                title = "G13 Study Leave",
                dateKey = "2026-05-04",
                eventType = com.example.data.CalendarEventType.SCHOOL_HOLIDAY.raw,
                targetType = com.example.data.CalendarEventTargetType.GRADE.raw,
                targetValue = "Grade 13"
            ),
            com.example.data.CalendarEvent(
                id = "class13m2_1",
                title = "13M2 Lab Maintenance",
                dateKey = "2026-05-05",
                eventType = com.example.data.CalendarEventType.SCHOOL_HOLIDAY.raw,
                targetType = com.example.data.CalendarEventTargetType.CLASS.raw,
                targetValue = "13M2"
            )
        )

        val for13M2 = AttendanceCalculator.filterCalendarEventsForStudent(events, student13M2)
        val for12B1 = AttendanceCalculator.filterCalendarEventsForStudent(events, student12B1)

        assertEquals(3, for13M2.size)
        assertEquals(1, for12B1.size)
        assertEquals("all_1", for12B1.first().id)
    }

    @Test
    fun findUnloggedRecentSchoolDaysSkipsWeekendsHolidaysAndLoggedDays() {
        val records = listOf(
            AttendanceRecord(dateKey = "2026-05-11", status = AttendanceStatus.PRESENT.raw) // Mon logged
        )
        val events = listOf(
            com.example.data.CalendarEvent(
                id = "hol",
                title = "Exam Study Leave",
                dateKey = "2026-05-12", // Tue No School
                eventType = com.example.data.CalendarEventType.SCHOOL_HOLIDAY.raw
            )
        )
        // Today = Thu 2026-05-14; Wed 13 unlogged, Tue 12 holiday, Mon 11 logged, Sun 10 & Sat 9 weekend, Fri 8 unlogged
        val missed = AttendanceCalculator.findUnloggedRecentSchoolDays(
            records = records,
            calendarEvents = events,
            activeTermStartDate = "2026-05-08",
            todayKey = "2026-05-14",
            lookbackCalendarDays = 7,
            maxItems = 5
        )
        assertEquals(listOf("2026-05-13", "2026-05-08"), missed)
    }

    @Test
    fun whatIfSkipSimulatorProjectsEligibilityAndRecovery() {
        val baseStats = AttendanceCalculator.calculateStats(
            records = emptyList(),
            thresholdPercent = 80,
            includePriorBalance = true,
            priorPresent = 16,
            priorAbsent = 4 // 16 / 20 = 80.0%
        )
        // Skipping 2 days -> 16 / 22 = 72.7% (< 80%), needs 8 straight days to recover (24 / 30 = 80%)
        val afterSkips = AttendanceCalculator.simulateFutureSkips(baseStats, extraSkips = 2)
        assertFalse(afterSkips.willStayEligible)
        assertEquals(72.72, afterSkips.projectedPercent, 0.05)
        assertEquals(8, afterSkips.daysNeededToRecover)

        // Attending 5 straight days -> 21 / 25 = 84.0% (>= 80%), 1 safe skip left (21 / 26 = 80.7%)
        val afterAttends = AttendanceCalculator.simulateFutureSkips(baseStats, extraSkips = 0, extraAttends = 5)
        assertTrue(afterAttends.willStayEligible)
        assertEquals(84.0, afterAttends.projectedPercent, 0.05)
        assertEquals(1, afterAttends.remainingSafeSkips)
    }

    @Test
    fun attendanceInsightsComputeMonthlyTrendAndMostSkippedWeekday() {
        val records = listOf(
            AttendanceRecord(dateKey = "2026-04-06", status = AttendanceStatus.PRESENT.raw), // Mon
            AttendanceRecord(dateKey = "2026-04-10", status = AttendanceStatus.ABSENT.raw),  // Fri
            AttendanceRecord(dateKey = "2026-05-04", status = AttendanceStatus.PRESENT.raw), // Mon
            AttendanceRecord(dateKey = "2026-05-05", status = AttendanceStatus.SCHOOL_EVENT.raw), // Tue
            AttendanceRecord(dateKey = "2026-05-08", status = AttendanceStatus.ABSENT.raw)   // Fri
        )
        val insights = AttendanceCalculator.calculateAttendanceInsights(records)
        assertEquals(2, insights.monthlyPoints.size)
        assertEquals("2026-04", insights.monthlyPoints[0].yearMonthKey)
        assertEquals(50.0, insights.monthlyPoints[0].percentage, 0.01)
        assertEquals("2026-05", insights.monthlyPoints[1].yearMonthKey)
        assertEquals(66.66, insights.monthlyPoints[1].percentage, 0.05)
        assertEquals("Friday", insights.mostSkippedWeekday?.fullName)
        assertEquals(2, insights.mostSkippedWeekday?.absentDays)
    }

    @Test
    fun countSchoolDaysInRangeAndPercentageBaselineExcludeWeekendsAndHolidays() {
        val events = listOf(
            CalendarEvent(
                id = "poya_may",
                title = "Vesak Poya",
                dateKey = "2026-05-06", // Wed
                eventType = CalendarEventType.POYA.raw,
                academicYear = 2026
            )
        )
        // Mon 2026-05-04 .. Fri 2026-05-15 = 10 weekdays minus 1 Poya (May 6) = 9 school days
        val schoolDays = AttendanceCalculator.countSchoolDaysInRange(
            startDateKey = "2026-05-04",
            endDateKey = "2026-05-15",
            calendarEvents = events
        )
        assertEquals(9, schoolDays)

        val estimate = AttendanceCalculator.estimateBaselineFromPercentage(
            startDateKey = "2026-05-04",
            endDateKey = "2026-05-15",
            percentage = 80.0,
            calendarEvents = events,
            customSchoolDaysOverride = 20
        )
        assertEquals(20, estimate.totalSchoolDays)
        assertEquals(16, estimate.presentDays)
        assertEquals(4, estimate.absentDays)
        assertEquals(80.0, estimate.effectivePercent, 0.01)
    }

    @Test
    fun lockedBaselineRangePreventsDoubleCountingAndExcludesCatchUp() {
        val locked = "2026-05-04" to "2026-05-11"
        val records = listOf(
            // Inside locked range (must NOT be double-counted alongside priorPresent/priorAbsent)
            AttendanceRecord(dateKey = "2026-05-08", status = AttendanceStatus.ABSENT.raw),
            // After locked range (MUST be counted)
            AttendanceRecord(dateKey = "2026-05-12", status = AttendanceStatus.PRESENT.raw)
        )
        val stats = AttendanceCalculator.calculateStats(
            records = records,
            thresholdPercent = 80,
            includePriorBalance = true,
            priorPresent = 16,
            priorAbsent = 4,
            lockedRange = locked
        )
        assertEquals(17, stats.effectiveAttended)
        assertEquals(21, stats.totalCountedDays)

        // Catch-up for Thu 2026-05-14 should find Wed 13 only (Tue 12 logged; Mon 11 & Fri 8 locked)
        val missed = AttendanceCalculator.findUnloggedRecentSchoolDays(
            records = records,
            calendarEvents = emptyList(),
            activeTermStartDate = "2026-05-04",
            todayKey = "2026-05-14",
            lookbackCalendarDays = 10,
            maxItems = 5,
            lockedRange = locked
        )
        assertEquals(listOf("2026-05-13"), missed)
    }

    @Test
    fun calculateMilestonesUnlocksStreaksPerfectMonthAndCleanSheet() {
        // 10 consecutive weekdays of Present/Excused in May 2026
        val dates = listOf(
            "2026-05-04", "2026-05-05", "2026-05-06", "2026-05-07", "2026-05-08",
            "2026-05-11", "2026-05-12", "2026-05-13", "2026-05-14", "2026-05-15"
        )
        val records = dates.mapIndexed { index, key ->
            AttendanceRecord(
                dateKey = key,
                status = if (index == 2) AttendanceStatus.SCHOOL_EVENT.raw else AttendanceStatus.PRESENT.raw
            )
        }
        val stats = AttendanceCalculator.calculateStats(
            records = records,
            thresholdPercent = 80,
            includePriorBalance = false,
            priorPresent = 0,
            priorAbsent = 0
        )

        val milestones = AttendanceCalculator.calculateMilestones(
            records = records,
            currentStreak = 10,
            termStats = stats,
            overallStats = stats
        )

        val byId = milestones.associateBy { it.id }
        assertTrue(byId["streak_5"]!!.unlocked)
        assertTrue(byId["streak_10"]!!.unlocked)
        assertTrue(byId["safe_zone_80"]!!.unlocked)
        assertTrue(byId["perfect_month"]!!.unlocked)
        assertFalse(byId["consistent_logger"]!!.unlocked) // 10 < 20
        assertEquals(0.5f, byId["consistent_logger"]!!.progressFraction, 0.01f)
        assertTrue(byId["clean_sheet"]!!.unlocked)
    }

    @Test
    fun calculateUpcomingWeekPrepCountsSchoolDaysAndHolidaysFromSunday() {
        // 2026-05-10 is Sunday -> upcoming Mon..Fri is 2026-05-11 .. 2026-05-15
        val events = listOf(
            CalendarEvent(
                id = "poya",
                title = "Vesak Poya",
                dateKey = "2026-05-13", // Wednesday
                eventType = CalendarEventType.POYA.raw,
                academicYear = 2026
            )
        )
        val termStats = AttendanceCalculator.calculateStats(
            records = emptyList(),
            thresholdPercent = 80,
            includePriorBalance = true,
            priorPresent = 18,
            priorAbsent = 2 // 90% -> 2 safe skips
        )

        val prep = AttendanceCalculator.calculateUpcomingWeekPrep(
            calendarEvents = events,
            termStats = termStats,
            referenceDateKey = "2026-05-10"
        )

        assertEquals("2026-05-11", prep.mondayKey)
        assertEquals("2026-05-15", prep.fridayKey)
        assertEquals(4, prep.schoolDaysCount)
        assertEquals(1, prep.offDays.size)
        assertEquals("Vesak Poya", prep.offDays.first().title)
        assertTrue(prep.eligible)
        assertEquals(2, prep.safeSkipsRemaining)
    }

    @Test
    fun userPresenceOnlineWindowAndLastSeenFormatting() {
        val now = 1_750_000_000_000L
        val freshTimestamp = com.google.firebase.Timestamp(java.util.Date(now - 60_000L)) // 1 min ago
        val staleTimestamp = com.google.firebase.Timestamp(java.util.Date(now - 10 * 60_000L)) // 10 min ago

        val onlineUser = UserProfile(onlineNow = true, lastSeenAt = freshTimestamp)
        val staleUser = UserProfile(onlineNow = true, lastSeenAt = staleTimestamp)
        val offlineUser = UserProfile(onlineNow = false, lastSeenAt = freshTimestamp)

        assertTrue(onlineUser.isCurrentlyOnline(nowMillis = now))
        assertFalse(staleUser.isCurrentlyOnline(nowMillis = now))
        assertFalse(offlineUser.isCurrentlyOnline(nowMillis = now))

        assertEquals("Online now", DateUtils.formatLastSeen(now - 60_000L, isOnline = true, nowMillis = now))
        assertEquals("Last seen 10m ago", DateUtils.formatLastSeen(now - 10 * 60_000L, isOnline = false, nowMillis = now))
        assertEquals("Last seen 2h ago", DateUtils.formatLastSeen(now - 2 * 3_600_000L, isOnline = false, nowMillis = now))
        assertEquals("Not seen yet", DateUtils.formatLastSeen(null, isOnline = false, nowMillis = now))
    }

    @Test
    fun broadcastNotificationMatchesMultipleAudiences() {
        val student13M2 = UserProfile(
            id = "uid_yasiru",
            userId = "uid_yasiru",
            grade = "Grade 13",
            alStream = "Physical Science",
            className = "13M2"
        )
        val student12B1 = UserProfile(
            id = "uid_nimal",
            userId = "uid_nimal",
            grade = "Grade 12",
            alStream = "Bio Science",
            className = "12B1"
        )

        val everyoneAlert = com.example.data.BroadcastNotification(targetAll = true)
        assertTrue(everyoneAlert.matchesStudent(student13M2, "uid_yasiru"))
        assertTrue(everyoneAlert.matchesStudent(student12B1, "uid_nimal"))

        val combinedAlert = com.example.data.BroadcastNotification(
            targetAll = false,
            targetGrades = listOf("Grade 12"),
            targetClasses = listOf("13m2") // case-insensitive match
        )
        assertTrue(combinedAlert.matchesStudent(student13M2, "uid_yasiru"))
        assertTrue(combinedAlert.matchesStudent(student12B1, "uid_nimal"))

        val specificStudentAlert = com.example.data.BroadcastNotification(
            targetAll = false,
            targetUserIds = listOf("uid_yasiru")
        )
        assertTrue(specificStudentAlert.matchesStudent(student13M2, "uid_yasiru"))
        assertFalse(specificStudentAlert.matchesStudent(student12B1, "uid_nimal"))
    }

    @Test
    fun defaultExamYearMatchesGrade12And13() {
        assertEquals(2028, com.example.data.AppConstants.defaultExamYearForGrade("Grade 12"))
        assertEquals(2026, com.example.data.AppConstants.defaultExamYearForGrade("Grade 13"))
    }

    @Test
    fun academicCatalogSupportsBucketsClassesAndMutations() {
        val base = com.example.data.AcademicCatalog.defaultCatalog()
        val bio12Classes = base.classesFor("Grade 12", "Bio Science")
        assertTrue(bio12Classes.contains("12S1"))
        assertTrue(bio12Classes.contains("12S2"))
        assertTrue(base.subjectsFor("Physical Science", com.example.data.AcademicCatalog.BUCKET_3).contains("ICT"))

        val updated = base
            .withAddedSubject("Physical Science", com.example.data.AcademicCatalog.BUCKET_3, "Higher Mathematics")
            .withRemovedSubject("Physical Science", com.example.data.AcademicCatalog.BUCKET_3, "ICT")
            .withAddedClass("Grade 12", "Bio Science", "12S7")
            .withRemovedClass("Grade 12", "Bio Science", "12S6")

        val updatedBucket3 = updated.subjectsFor("Physical Science", com.example.data.AcademicCatalog.BUCKET_3)
        assertTrue(updatedBucket3.contains("Higher Mathematics"))
        assertFalse(updatedBucket3.contains("ICT"))

        val updatedClasses = updated.classesFor("Grade 12", "Bio Science")
        assertTrue(updatedClasses.contains("12S7"))
        assertFalse(updatedClasses.contains("12S6"))
    }

    @Test
    fun multiAudienceCalendarEventIsStrictlyIsolatedToTargetStudents() {
        val student12S1 = UserProfile(
            id = "u_bio",
            userId = "u_bio",
            grade = "Grade 12",
            alStream = "Bio Science",
            className = "12S1"
        )
        val student13M2 = UserProfile(
            id = "u_maths",
            userId = "u_maths",
            grade = "Grade 13",
            alStream = "Physical Science",
            className = "13M2"
        )

        val targetedEvent = CalendarEvent(
            id = "ev_bio_only",
            title = "Biology Practical Workshop",
            dateKey = "2026-06-15",
            eventType = CalendarEventType.SCHOOL_EVENT.raw,
            description = "There might be no teaching during periods 4-6 for 12S1.",
            targetAll = false,
            targetClasses = listOf("12S1"),
            audienceSummary = "12S1"
        )
        val secondEventSameDay = CalendarEvent(
            id = "ev_school_wide",
            title = "Morning Assembly",
            dateKey = "2026-06-15",
            eventType = CalendarEventType.SCHOOL_EVENT.raw,
            targetAll = true,
            audienceSummary = "All Students"
        )

        val allEvents = listOf(targetedEvent, secondEventSameDay)
        val bioVisible = AttendanceCalculator.filterCalendarEventsForStudent(allEvents, student12S1, "u_bio")
        val mathsVisible = AttendanceCalculator.filterCalendarEventsForStudent(allEvents, student13M2, "u_maths")

        assertEquals(2, bioVisible.size)
        assertEquals(1, mathsVisible.size)
        assertEquals("ev_school_wide", mathsVisible.first().id)
    }

    @Test
    fun studyLeaveIsNonSchoolDayButNotSriLankanHoliday() {
        val studyLeave = CalendarEvent(
            id = "ev_exam_break",
            title = "Term Test Study Break",
            dateKey = "2026-05-12",
            eventType = CalendarEventType.STUDY_LEAVE.raw,
            description = "No school between Physics and Chemistry term test papers."
        )
        val schoolOff = CalendarEvent(
            id = "ev_school_off",
            title = "Sports Meet Prep Off",
            dateKey = "2026-05-14",
            eventType = CalendarEventType.SCHOOL_HOLIDAY.raw
        )
        val poya = CalendarEvent(
            id = "ev_vesak",
            title = "Vesak Full Moon Poya Day",
            dateKey = "2026-05-20",
            eventType = CalendarEventType.POYA.raw
        )

        assertTrue(studyLeave.nonSchoolDay)
        assertFalse(studyLeave.isSriLankanHoliday)
        assertTrue(schoolOff.nonSchoolDay)
        assertFalse(schoolOff.isSriLankanHoliday)
        assertTrue(poya.nonSchoolDay)
        assertTrue(poya.isSriLankanHoliday)

        val nextHoliday = AttendanceCalculator.nextUpcomingPoyaOrHoliday(
            calendarEvents = listOf(studyLeave, schoolOff, poya),
            todayKey = "2026-05-10"
        )
        assertEquals("ev_vesak", nextHoliday?.id)
    }

    @Test
    fun estimateBaselineFromDaysComputesExactPercentageAndCounts() {
        val estimate = AttendanceCalculator.estimateBaselineFromDays(
            startDateKey = "2026-01-05",
            endDateKey = "2026-05-15",
            daysSchoolHeld = 50,
            daysAttended = 42
        )
        assertEquals(50, estimate.totalSchoolDays)
        assertEquals(42, estimate.presentDays)
        assertEquals(8, estimate.absentDays)
        assertEquals(84.0, estimate.enteredPercent, 0.01)
        assertEquals(84.0, estimate.effectivePercent, 0.01)
    }

    @Test
    fun lockedBaselineLocksAllDaysOnOrBeforeEndDate() {
        val profile = UserProfile(
            priorPresentCount = 42,
            priorAbsentCount = 8,
            baselineStartDate = "2026-05-01",
            baselineEndDate = "2026-05-15",
            baselinePercent = 84.0
        )
        assertTrue(profile.hasLockedBaseline)
        assertTrue(profile.isDateInLockedBaseline("2026-04-10")) // Before start date -> still locked
        assertTrue(profile.isDateInLockedBaseline("2026-05-01")) // On start date -> locked
        assertTrue(profile.isDateInLockedBaseline("2026-05-10")) // Inside -> locked
        assertTrue(profile.isDateInLockedBaseline("2026-05-15")) // On end date ("Up Until This Day") -> locked
        assertFalse(profile.isDateInLockedBaseline("2026-05-16")) // Day after -> editable
    }

    @Test
    fun customPeriodBlockCalculatesMissedDaysPreventsDoubleCountAndLocksCalendar() {
        // October 2026 has 22 weekdays; suppose 1 Poya day -> 21 school days, missed 1 -> 20 present, 1 absent
        val events = listOf(
            CalendarEvent(
                id = "oct_poya",
                title = "Vap Poya",
                dateKey = "2026-10-26",
                eventType = CalendarEventType.POYA.raw
            )
        )
        val est = AttendanceCalculator.estimatePeriodFromMissedDays(
            startDateKey = "2026-10-01",
            endDateKey = "2026-10-31",
            missedDays = 1,
            calendarEvents = events
        )
        assertEquals(21, est.totalSchoolDays)
        assertEquals(20, est.presentDays)
        assertEquals(1, est.absentDays)
        assertEquals(95.23, est.effectivePercent, 0.05)

        val block = com.example.data.AttendancePeriodBlock(
            id = "oct_block",
            userId = "u1",
            startDate = "2026-10-01",
            endDate = "2026-10-31",
            totalSchoolDays = 21,
            presentDays = 20,
            absentDays = 1,
            percentage = est.effectivePercent,
            entryMode = com.example.data.AttendancePeriodEntryMode.MISSED_DAYS.raw,
            label = "October"
        )
        val records = listOf(
            // Overlapping inside Oct block -> MUST be excluded to avoid double-counting
            AttendanceRecord(dateKey = "2026-10-05", status = AttendanceStatus.ABSENT.raw),
            // Outside block in November -> MUST be counted
            AttendanceRecord(dateKey = "2026-11-02", status = AttendanceStatus.PRESENT.raw)
        )

        val stats = AttendanceCalculator.calculateStats(
            records = records,
            thresholdPercent = 80,
            includePriorBalance = false,
            priorPresent = 0,
            priorAbsent = 0,
            periodBlocks = listOf(block)
        )
        assertEquals(20, stats.periodPresentDays)
        assertEquals(1, stats.periodAbsentDays)
        assertEquals(21, stats.effectiveAttended)
        assertEquals(22, stats.totalCountedDays)

        // Catch-up for Tue 2026-11-03 should skip locked Oct dates (Oct 30, Oct 29...) and already-logged Nov 2
        val unlogged = AttendanceCalculator.findUnloggedRecentSchoolDays(
            records = records,
            calendarEvents = events,
            activeTermStartDate = "2026-09-01",
            todayKey = "2026-11-03",
            lookbackCalendarDays = 7,
            maxItems = 5,
            periodBlocks = listOf(block)
        )
        assertTrue(unlogged.isEmpty())
    }

    @Test
    fun cohortPulseAggregatesClassStreamGradeAndSchoolAnonymously() {
        val myProfile = UserProfile(
            grade = "Grade 13",
            alStream = "Physical Science",
            className = "13M1"
        )
        val summaries = listOf(
            com.example.data.StudentTrendSummary(
                id = "u1", userId = "u1", totalAttended = 9, totalAbsent = 1,
                grade = "Grade 13", alStream = "Physical Science", className = "13M1"
            ),
            com.example.data.StudentTrendSummary(
                id = "u2", userId = "u2", totalAttended = 7, totalAbsent = 3,
                grade = "Grade 13", alStream = "Physical Science", className = "13M1"
            ),
            com.example.data.StudentTrendSummary(
                id = "u3", userId = "u3", totalAttended = 10, totalAbsent = 0,
                grade = "Grade 13", alStream = "Physical Science", className = "13M2"
            ),
            com.example.data.StudentTrendSummary(
                id = "u4", userId = "u4", totalAttended = 6, totalAbsent = 4,
                grade = "Grade 13", alStream = "Bio Science", className = "13B1"
            ),
            com.example.data.StudentTrendSummary(
                id = "u5", userId = "u5", totalAttended = 8, totalAbsent = 2,
                grade = "Grade 12", alStream = "Commerce", className = "12C1"
            )
        )
        val pulse = AttendanceCalculator.calculateCohortPulse(
            summaries = summaries,
            profile = myProfile,
            timeframe = com.example.data.TrendsTimeframe.ALL_TIME
        )
        assertEquals(80.0, pulse.classItem?.percentage ?: 0.0, 0.05) // 16/20
        assertEquals(2, pulse.classItem?.contributorCount)
        assertEquals(86.67, pulse.streamItem?.percentage ?: 0.0, 0.1) // 26/30
        assertEquals(3, pulse.streamItem?.contributorCount)
        assertEquals(80.0, pulse.gradeItem?.percentage ?: 0.0, 0.05) // 32/40
        assertEquals(4, pulse.gradeItem?.contributorCount)
        assertEquals(80.0, pulse.schoolItem?.percentage ?: 0.0, 0.05) // 40/50
        assertEquals(5, pulse.schoolItem?.contributorCount)
    }

    @Test
    fun previewCheckInImpactComputesBeforeAfterAndSafeSkips() {
        val term = com.example.data.AcademicTerm(
            id = "t1", name = "Term 1", academicYear = 2026,
            startDate = "2026-01-01", endDate = "2026-12-31", minEligibilityPercent = 80
        )
        val schoolDays = listOf(
            "2026-05-04", "2026-05-05", "2026-05-06", "2026-05-07", "2026-05-08",
            "2026-05-11", "2026-05-12", "2026-05-13"
        )
        val records = schoolDays.map { dk ->
            AttendanceRecord(dateKey = dk, termId = "t1", status = AttendanceStatus.PRESENT.raw)
        } + listOf(
            AttendanceRecord(dateKey = "2026-05-14", termId = "t1", status = AttendanceStatus.ABSENT.raw)
        ) // 8/9 = 88.89%

        val absentPreview = AttendanceCalculator.previewCheckInImpact(
            records = records,
            dateKey = "2026-05-15",
            proposedStatus = AttendanceStatus.ABSENT,
            activeTerm = term,
            thresholdPercent = 80,
            profile = null
        )
        assertEquals(88.89, absentPreview.currentPercent, 0.05)
        assertEquals(80.0, absentPreview.projectedPercent, 0.05)
        assertTrue(absentPreview.willStayEligible)
        assertEquals(0, absentPreview.remainingSafeSkips)

        val presentPreview = AttendanceCalculator.previewCheckInImpact(
            records = records,
            dateKey = "2026-05-15",
            proposedStatus = AttendanceStatus.PRESENT,
            activeTerm = term,
            thresholdPercent = 80,
            profile = null
        )
        assertEquals(90.0, presentPreview.projectedPercent, 0.05)
        assertTrue(presentPreview.percentDelta > 0.0)
        assertEquals(1, presentPreview.remainingSafeSkips)
    }

    @Test
    fun dangerZoneAndPerfectWeekDetectionWorkAccurately() {
        val edgeStats = com.example.data.AttendanceStats(
            percentage = 80.0, eligible = true, thresholdPercent = 80,
            presentDays = 8, absentDays = 2, medicalDays = 0, schoolEventDays = 0, holidayDays = 0,
            priorPresentDays = 0, priorAbsentDays = 0,
            effectiveAttended = 8, totalCountedDays = 10, safeSkipsRemaining = 0, daysNeededToRecover = 0
        )
        val comfortableStats = edgeStats.copy(
            presentDays = 19, absentDays = 1, effectiveAttended = 19,
            totalCountedDays = 20, percentage = 95.0, safeSkipsRemaining = 3
        )
        val alreadyBelowStats = edgeStats.copy(
            presentDays = 7, absentDays = 3, effectiveAttended = 7,
            totalCountedDays = 10, percentage = 70.0, eligible = false, daysNeededToRecover = 5
        )
        assertTrue(AttendanceCalculator.isDangerZone(edgeStats))
        assertFalse(AttendanceCalculator.isDangerZone(comfortableStats))
        assertFalse(AttendanceCalculator.isDangerZone(alreadyBelowStats))

        val perfectWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri").mapIndexed { idx, lbl ->
            com.example.data.WeekDaySummary(
                dayLabel = lbl,
                dateKey = "2026-11-0${idx + 2}",
                status = if (idx == 2) AttendanceStatus.MEDICAL else AttendanceStatus.PRESENT,
                holidayLabel = null,
                isToday = idx == 4,
                isFuture = false
            )
        }
        assertTrue(AttendanceCalculator.isPerfectFiveOfFiveWeek(perfectWeek))
        assertFalse(AttendanceCalculator.isPerfectFiveOfFiveWeek(perfectWeek.dropLast(1) + perfectWeek.last().copy(status = AttendanceStatus.ABSENT)))
    }

    @Test
    fun verifyAgainstClassRegisterFlagsExactAndMismatch() {
        val stats = com.example.data.AttendanceStats(
            percentage = (64.0 / 78.0) * 100.0, eligible = true, thresholdPercent = 80,
            presentDays = 64, absentDays = 14, medicalDays = 0, schoolEventDays = 0, holidayDays = 0,
            priorPresentDays = 0, priorAbsentDays = 0,
            effectiveAttended = 64, totalCountedDays = 78, safeSkipsRemaining = 2, daysNeededToRecover = 0
        )
        val exact = AttendanceCalculator.verifyAgainstClassRegister(stats, officialAttended = 64, officialTotal = 78)
        assertTrue(exact.isExactMatch)
        assertEquals(0, exact.attendedDiff)
        assertEquals(0, exact.totalDiff)

        val mismatch = AttendanceCalculator.verifyAgainstClassRegister(stats, officialAttended = 62, officialTotal = 78)
        assertFalse(mismatch.isExactMatch)
        assertEquals(2, mismatch.attendedDiff)
        assertEquals(0, mismatch.totalDiff)
    }

    @Test
    fun alJourneyTimelineAndWeeklyDigestAndPillFormatterComputeExpectedOutputs() {
        val settings = com.example.data.AppSettings()
        val timeline = AttendanceCalculator.calculateAlJourneyTimeline(
            examYear = 2026,
            settings = settings,
            terms = com.example.data.SriLankaCalendarDefaults.defaultTerms,
            todayKey = "2026-05-01"
        )
        assertTrue(timeline.daysRemaining > 0)
        assertTrue(timeline.progressFraction in 0f..1f)
        assertTrue(timeline.milestones.any { it.label == "100d Left" })
        assertTrue(timeline.milestones.any { it.isExamDay })

        val digestRecords = listOf(
            AttendanceRecord(dateKey = "2026-11-09", status = AttendanceStatus.PRESENT.raw), // Mon
            AttendanceRecord(dateKey = "2026-11-10", status = AttendanceStatus.PRESENT.raw), // Tue
            AttendanceRecord(dateKey = "2026-11-11", status = AttendanceStatus.PRESENT.raw), // Wed
            AttendanceRecord(dateKey = "2026-11-12", status = AttendanceStatus.ABSENT.raw),  // Thu
            AttendanceRecord(dateKey = "2026-11-13", status = AttendanceStatus.PRESENT.raw)  // Fri
        )
        val overall = com.example.data.AttendanceStats(
            percentage = 80.0, eligible = true, thresholdPercent = 80,
            presentDays = 4, absentDays = 1, medicalDays = 0, schoolEventDays = 0, holidayDays = 0,
            priorPresentDays = 0, priorAbsentDays = 0,
            effectiveAttended = 4, totalCountedDays = 5, safeSkipsRemaining = 0, daysNeededToRecover = 0
        )
        val futurePoya = CalendarEvent(
            id = "p1", title = "Il Poya", dateKey = "2026-11-24", eventType = CalendarEventType.POYA.raw
        )
        val digest = AttendanceCalculator.calculateLastWeekDigest(
            records = digestRecords,
            calendarEvents = listOf(futurePoya),
            overallStats = overall,
            todayKey = "2026-11-15" // Sunday
        )
        assertEquals(4, digest.lastWeekAttended)
        assertEquals(5, digest.lastWeekSchoolDays)
        assertEquals("Il Poya", digest.nextHolidayTitle)
        assertEquals(9, digest.nextHolidayDaysAway)

        val pill = com.example.system.AttendancePillWidgetProvider.formatPillSegments(
            com.example.system.WidgetSnapshot(
                signedIn = true,
                percentage = 86.4,
                eligible = true,
                totalCountedDays = 50,
                safeSkipsRemaining = 4,
                daysUntilAlExam = 214
            )
        )
        assertEquals("86.4%", pill.first)
        assertEquals("Safe (+4d)", pill.second)
        assertEquals("214d to A/L", pill.third)
    }

    @Test
    fun nextDayPollWindowEnforces8PmTo5AmAndSkipsWeekendsAndHolidays() {
        val vesakPoya = CalendarEvent(
            id = "poya_1",
            title = "Vesak Poya",
            dateKey = "2026-11-24", // Tuesday
            eventType = CalendarEventType.POYA.raw
        )

        // Sunday Nov 22 at 20:00 -> open for Monday Nov 23 (school day)
        val sunEvening = AttendanceCalculator.resolveNextDayPollWindow(
            calendarEvents = listOf(vesakPoya),
            todayKey = "2026-11-22",
            hourOfDay = 20
        )
        assertTrue(sunEvening.isOpen)
        assertEquals("2026-11-23", sunEvening.targetDateKey)
        assertTrue(sunEvening.isTomorrowTarget)

        // Monday Nov 23 at 02:30 -> still open for Monday Nov 23 ("Today")
        val monEarlyMorning = AttendanceCalculator.resolveNextDayPollWindow(
            calendarEvents = listOf(vesakPoya),
            todayKey = "2026-11-23",
            hourOfDay = 2
        )
        assertTrue(monEarlyMorning.isOpen)
        assertEquals("2026-11-23", monEarlyMorning.targetDateKey)
        assertFalse(monEarlyMorning.isTomorrowTarget)

        // Monday Nov 23 at 05:00 -> closed (cutoff is 5:00 AM)
        val monCutoff = AttendanceCalculator.resolveNextDayPollWindow(
            calendarEvents = listOf(vesakPoya),
            todayKey = "2026-11-23",
            hourOfDay = 5
        )
        assertFalse(monCutoff.isOpen)

        // Monday Nov 23 at 21:00 -> tomorrow Nov 24 is Vesak Poya (non-school day) -> closed
        val beforePoya = AttendanceCalculator.resolveNextDayPollWindow(
            calendarEvents = listOf(vesakPoya),
            todayKey = "2026-11-23",
            hourOfDay = 21
        )
        assertFalse(beforePoya.isOpen)
        assertFalse(beforePoya.isTargetSchoolDay)

        // Friday Nov 27 at 20:30 -> tomorrow Nov 28 is Saturday -> closed
        val friEvening = AttendanceCalculator.resolveNextDayPollWindow(
            calendarEvents = emptyList(),
            todayKey = "2026-11-27",
            hourOfDay = 20
        )
        assertFalse(friEvening.isOpen)
        assertFalse(friEvening.isTargetSchoolDay)
    }

    @Test
    fun nextDayPollSummaryCalculatesComingNotComingAndClassBreakdown() {
        val targetDate = "2026-11-23"
        val votes = listOf(
            com.example.data.NextDaySchoolVote(
                id = "${targetDate}_u1", userId = "u1", targetDateKey = targetDate,
                going = true, className = "13M2"
            ),
            com.example.data.NextDaySchoolVote(
                id = "${targetDate}_u2", userId = "u2", targetDateKey = targetDate,
                going = true, className = "13M2"
            ),
            com.example.data.NextDaySchoolVote(
                id = "${targetDate}_u3", userId = "u3", targetDateKey = targetDate,
                going = false, className = "13M2"
            ),
            com.example.data.NextDaySchoolVote(
                id = "${targetDate}_u4", userId = "u4", targetDateKey = targetDate,
                going = true, className = "13B1"
            ),
            // Different date should be ignored
            com.example.data.NextDaySchoolVote(
                id = "2026-11-25_u5", userId = "u5", targetDateKey = "2026-11-25",
                going = false, className = "13M2"
            )
        )

        val summary = AttendanceCalculator.calculateNextDayPollSummary(
            votes = votes,
            currentUserId = "u1",
            myClassName = "13M2",
            targetDateKey = targetDate
        )
        assertEquals(4, summary.totalVotes)
        assertEquals(3, summary.goingCount)
        assertEquals(1, summary.notGoingCount)
        assertEquals(75.0, summary.goingPercent, 0.01)
        assertEquals(25.0, summary.notGoingPercent, 0.01)
        assertEquals(true, summary.myVoteGoing)
        assertEquals(2, summary.myClassGoingCount)
        assertEquals(3, summary.myClassTotalCount)
        assertEquals(66.67, summary.myClassGoingPercent ?: 0.0, 0.1)
    }

    @Test
    fun customSchoolStartDateOverridesDefaultJourneyStartAndTipCategoryResolves() {
        val customSettings = com.example.data.AppSettings(schoolStartDate = "2026-02-09")
        val timeline = AttendanceCalculator.calculateAlJourneyTimeline(
            examYear = 2028,
            settings = customSettings,
            terms = com.example.data.SriLankaCalendarDefaults.defaultTerms,
            todayKey = "2026-05-01"
        )
        assertEquals("2026-02-09", timeline.startDateKey)
        assertEquals("2026-02-09", timeline.milestones.first().dateKey)
        assertEquals("Tip ✨", com.example.data.NoticeCategory.TIP.label)
        assertEquals(com.example.data.NoticeCategory.TIP, com.example.data.NoticeCategory.fromRaw("tip"))
    }

    @Test
    fun duplicateRecordsForSameDateAreDeduplicatedKeepingLatest() {
        val older = AttendanceRecord(
            id = "dup1",
            userId = "u1",
            dateKey = "2026-05-04",
            status = AttendanceStatus.ABSENT.raw,
            updatedAt = com.google.firebase.Timestamp(1000L, 0)
        )
        val newer = AttendanceRecord(
            id = "dup2",
            userId = "u1",
            dateKey = "2026-05-04",
            status = AttendanceStatus.PRESENT.raw,
            updatedAt = com.google.firebase.Timestamp(2000L, 0)
        )
        val other = AttendanceRecord(
            id = "day2",
            userId = "u1",
            dateKey = "2026-05-05",
            status = AttendanceStatus.PRESENT.raw
        )

        val stats = AttendanceCalculator.calculateStats(
            records = listOf(older, newer, other),
            thresholdPercent = 80,
            includePriorBalance = false,
            priorPresent = 0,
            priorAbsent = 0
        )
        assertEquals(2, stats.totalCountedDays)
        assertEquals(2, stats.presentDays)
        assertEquals(0, stats.absentDays)
        assertEquals(100.0, stats.percentage, 0.01)
    }

    @Test
    fun baselineAndOverlappingPeriodBlocksNeverDoubleCount() {
        // Baseline locks everything up to 2026-03-31 (40P / 10A)
        val lockedRange = "2026-01-15" to "2026-03-31"
        // Block 1 overlaps baseline (starts 2026-03-10 -> 2026-04-10) -> MUST be dropped
        val overlappingBaselineBlock = com.example.data.AttendancePeriodBlock(
            id = "blk_overlap_base",
            userId = "u1",
            startDate = "2026-03-10",
            endDate = "2026-04-10",
            totalSchoolDays = 20,
            presentDays = 18,
            absentDays = 2,
            percentage = 90.0
        )
        // Block 2 (April 1..15) and Block 3 (April 10..20) overlap each other -> only one kept
        val aprilBlockA = com.example.data.AttendancePeriodBlock(
            id = "blk_apr_a",
            userId = "u1",
            startDate = "2026-04-01",
            endDate = "2026-04-15",
            totalSchoolDays = 10,
            presentDays = 9,
            absentDays = 1,
            percentage = 90.0,
            updatedAt = com.google.firebase.Timestamp(1000L, 0)
        )
        val aprilBlockB = com.example.data.AttendancePeriodBlock(
            id = "blk_apr_b",
            userId = "u1",
            startDate = "2026-04-10",
            endDate = "2026-04-20",
            totalSchoolDays = 8,
            presentDays = 7,
            absentDays = 1,
            percentage = 87.5,
            updatedAt = com.google.firebase.Timestamp(2000L, 0)
        )
        // Daily records: one inside baseline (dropped), one inside kept April block (dropped), one clean in May (kept)
        val records = listOf(
            AttendanceRecord(dateKey = "2026-02-10", status = AttendanceStatus.PRESENT.raw),
            AttendanceRecord(dateKey = "2026-04-14", status = AttendanceStatus.ABSENT.raw),
            AttendanceRecord(dateKey = "2026-05-04", status = AttendanceStatus.PRESENT.raw)
        )

        val cleanBlocks = AttendanceCalculator.deduplicatePeriodBlocks(
            periodBlocks = listOf(overlappingBaselineBlock, aprilBlockA, aprilBlockB),
            lockedRange = lockedRange
        )
        assertEquals(1, cleanBlocks.size)
        assertEquals("blk_apr_b", cleanBlocks.first().id)

        val stats = AttendanceCalculator.calculateStats(
            records = records,
            thresholdPercent = 80,
            includePriorBalance = true,
            priorPresent = 40,
            priorAbsent = 10,
            lockedRange = lockedRange,
            periodBlocks = listOf(overlappingBaselineBlock, aprilBlockA, aprilBlockB)
        )
        // Expected: baseline (40P/10A) + blk_apr_b (7P/1A) + May 4 (1P) = 48P / 11A = 59 total
        assertEquals(48, stats.effectiveAttended)
        assertEquals(59, stats.totalCountedDays)
        assertEquals(7, stats.periodPresentDays)
        assertEquals(1, stats.periodAbsentDays)
    }

    @Test
    fun lockedRecordsDoNotLeakIntoMonthScopeAndBaselineDoesNotInflateOtherTerms() {
        val term1 = com.example.data.AcademicTerm(
            id = "term_1_2026",
            name = "Term 1",
            academicYear = 2026,
            startDate = "2026-01-05",
            endDate = "2026-04-10",
            minEligibilityPercent = 80
        )
        val term2 = com.example.data.AcademicTerm(
            id = "term_2_2026",
            name = "Term 2",
            academicYear = 2026,
            startDate = "2026-04-20",
            endDate = "2026-08-14",
            minEligibilityPercent = 80
        )
        // Baseline locked strictly inside Term 1 (Jan 5 -> Mar 20)
        val lockedRange = "2026-01-05" to "2026-03-20"
        val records = listOf(
            // Stale record inside locked baseline in March -> must NOT leak into March Month stats
            AttendanceRecord(dateKey = "2026-03-10", termId = "term_1_2026", status = AttendanceStatus.ABSENT.raw),
            // Clean unlocked March record after baseline end -> MUST count in March
            AttendanceRecord(dateKey = "2026-03-23", termId = "term_1_2026", status = AttendanceStatus.PRESENT.raw),
            // Record on Term 1 date but wrongly stamped with term_2_2026 -> must NOT count in Term 2
            AttendanceRecord(dateKey = "2026-03-24", termId = "term_2_2026", status = AttendanceStatus.PRESENT.raw),
            // Clean Term 2 record
            AttendanceRecord(dateKey = "2026-05-04", termId = "term_2_2026", status = AttendanceStatus.PRESENT.raw)
        )

        // Month scope (March 2026) with includePriorBalance = false (multi-month baseline):
        // stale 2026-03-10 inside lockedRange must still be excluded!
        val marchStats = AttendanceCalculator.calculateStats(
            records = records,
            thresholdPercent = 80,
            includePriorBalance = false,
            priorPresent = 30,
            priorAbsent = 5,
            monthPrefixFilter = "2026-03",
            lockedRange = lockedRange
        )
        assertEquals(2, marchStats.totalCountedDays) // Only 2026-03-23 and 2026-03-24
        assertEquals(2, marchStats.presentDays)
        assertEquals(0, marchStats.absentDays)

        // Term 2 stats: must NOT include Term 1 baseline AND must NOT include mismatched-termId March record
        val term2Stats = AttendanceCalculator.calculateStats(
            records = records,
            thresholdPercent = 80,
            includePriorBalance = true,
            priorPresent = 30,
            priorAbsent = 5,
            termFilter = term2,
            lockedRange = lockedRange
        )
        assertEquals(1, term2Stats.totalCountedDays) // Only 2026-05-04
        assertEquals(0, term2Stats.priorPresentDays)
        assertEquals(0, term2Stats.priorAbsentDays)

        // Overall stats: includes Term 1 baseline (30P/5A) + unlocked daily records (3P) = 33P / 5A = 38 total
        val overallStats = AttendanceCalculator.calculateStats(
            records = records,
            thresholdPercent = 80,
            includePriorBalance = true,
            priorPresent = 30,
            priorAbsent = 5,
            termFilter = null,
            lockedRange = lockedRange
        )
        assertEquals(33, overallStats.effectiveAttended)
        assertEquals(38, overallStats.totalCountedDays)
    }

    @Test
    fun nonSchoolDayAndPreSchoolStartRecordsAreExcludedEverywhere() {
        val vesakPoya = CalendarEvent(
            id = "poya_vesak",
            title = "Vesak Poya",
            dateKey = "2026-05-20",
            eventType = CalendarEventType.POYA.raw
        )
        val records = listOf(
            // Before schoolStartDate (2026-02-09) -> excluded
            AttendanceRecord(dateKey = "2026-01-20", status = AttendanceStatus.ABSENT.raw),
            // On published non-school Poya day -> excluded
            AttendanceRecord(dateKey = "2026-05-20", status = AttendanceStatus.ABSENT.raw),
            // Valid school day -> included
            AttendanceRecord(dateKey = "2026-05-21", status = AttendanceStatus.PRESENT.raw)
        )

        val stats = AttendanceCalculator.calculateStats(
            records = records,
            thresholdPercent = 80,
            includePriorBalance = false,
            priorPresent = 0,
            priorAbsent = 0,
            calendarEvents = listOf(vesakPoya),
            schoolStartDate = "2026-02-09"
        )
        assertEquals(1, stats.totalCountedDays)
        assertEquals(1, stats.presentDays)
        assertEquals(0, stats.absentDays)

        val personalInsights = AttendanceCalculator.calculatePersonalInsightsForScope(
            records = records,
            timeframe = com.example.data.TrendsTimeframe.ALL_TIME,
            calendarEvents = listOf(vesakPoya),
            schoolStartDate = "2026-02-09"
        )
        assertEquals(1, personalInsights.totalCountedDays)
        assertEquals(1, personalInsights.totalAttendedDays)
    }

    @Test
    fun trendSummaryInsightsAndMilestonesPreventDoubleCountingWithLockedBaseline() {
        val lockedRange = "2026-01-15" to "2026-03-31"
        val profile = UserProfile(
            userId = "u1",
            grade = "Grade 12",
            alStream = "Physical Science",
            className = "12M1",
            priorPresentCount = 40,
            priorAbsentCount = 10,
            baselineStartDate = "2026-01-15",
            baselineEndDate = "2026-03-31",
            baselinePercent = 80.0
        )
        val records = listOf(
            // Inside locked baseline -> MUST NOT be counted on top of priorPresent/priorAbsent
            AttendanceRecord(dateKey = "2026-02-10", status = AttendanceStatus.PRESENT.raw),
            AttendanceRecord(dateKey = "2026-02-11", status = AttendanceStatus.ABSENT.raw),
            // Outside baseline -> counted once
            AttendanceRecord(dateKey = "2026-05-04", status = AttendanceStatus.PRESENT.raw),
            AttendanceRecord(dateKey = "2026-05-05", status = AttendanceStatus.PRESENT.raw)
        )

        val summary = AttendanceCalculator.buildStudentTrendSummary(
            userId = "u1",
            records = records,
            periodBlocks = emptyList(),
            priorPresent = profile.priorPresentCount,
            priorAbsent = profile.priorAbsentCount,
            grade = profile.grade,
            alStream = profile.alStream,
            className = profile.className,
            lockedRange = lockedRange
        )
        // Expected: prior (40P/10A) + unlocked daily (2P/0A) = 42P / 10A (NOT 43P/11A!)
        assertEquals(42, summary.totalAttended)
        assertEquals(10, summary.totalAbsent)

        val personal = AttendanceCalculator.calculatePersonalInsightsForScope(
            records = records,
            timeframe = com.example.data.TrendsTimeframe.ALL_TIME,
            lockedRange = lockedRange
        )
        // Personal daily trends exclude lockedRange records -> only 2 May days
        assertEquals(2, personal.totalCountedDays)
        assertEquals(2, personal.totalAttendedDays)
    }

    @Test
    fun communityInsightsAndCohortPulseDeduplicateDuplicateUserSummaries() {
        val profile = UserProfile(grade = "Grade 13", alStream = "Physical Science", className = "13M1")
        val dup1 = com.example.data.StudentTrendSummary(
            id = "u1",
            userId = "u1",
            totalAttended = 8,
            totalAbsent = 2,
            grade = "Grade 13",
            alStream = "Physical Science",
            className = "13M1",
            monthlyTotalsJson = """{"2026-05":[8,10]}"""
        )
        val dup1Duplicate = dup1.copy(id = "u1_stale")
        val user2 = com.example.data.StudentTrendSummary(
            id = "u2",
            userId = "u2",
            totalAttended = 10,
            totalAbsent = 0,
            grade = "Grade 13",
            alStream = "Physical Science",
            className = "13M1",
            monthlyTotalsJson = """{"2026-05":[10,10]}"""
        )

        val community = AttendanceCalculator.calculateCommunityInsights(
            summaries = listOf(dup1, dup1Duplicate, user2),
            timeframe = com.example.data.TrendsTimeframe.ALL_TIME
        )
        assertEquals(2, community.contributorCount)
        assertEquals(18, community.totalAttendedDays)
        assertEquals(20, community.totalCountedDays)

        val pulse = AttendanceCalculator.calculateCohortPulse(
            summaries = listOf(dup1, dup1Duplicate, user2),
            profile = profile,
            timeframe = com.example.data.TrendsTimeframe.ALL_TIME
        )
        assertEquals(2, pulse.classItem?.contributorCount)
        assertEquals(90.0, pulse.classItem?.percentage ?: 0.0, 0.05) // 18/20, NOT 26/30
    }

    @Test
    fun weeklyDigestIgnoresLockedRecordsWhenComputingDelta() {
        val lockedRange = "2026-01-15" to "2026-11-11"
        val records = listOf(
            // Mon..Wed inside locked baseline -> must NOT be subtracted from overallStats again
            AttendanceRecord(dateKey = "2026-11-09", status = AttendanceStatus.PRESENT.raw),
            AttendanceRecord(dateKey = "2026-11-10", status = AttendanceStatus.PRESENT.raw),
            AttendanceRecord(dateKey = "2026-11-11", status = AttendanceStatus.PRESENT.raw),
            // Thu..Fri unlocked
            AttendanceRecord(dateKey = "2026-11-12", status = AttendanceStatus.PRESENT.raw),
            AttendanceRecord(dateKey = "2026-11-13", status = AttendanceStatus.ABSENT.raw)
        )
        val overallStats = AttendanceCalculator.calculateStats(
            records = records,
            thresholdPercent = 80,
            includePriorBalance = true,
            priorPresent = 40,
            priorAbsent = 10,
            lockedRange = lockedRange
        ) // 41/52 = 78.85%; prior before Thu..Fri = 40/50 = 80.0%
        val digest = AttendanceCalculator.calculateLastWeekDigest(
            records = records,
            calendarEvents = emptyList(),
            overallStats = overallStats,
            todayKey = "2026-11-15",
            lockedRange = lockedRange
        )
        assertEquals(1, digest.lastWeekAttended) // Only unlocked Thu
        assertEquals(-1.15, digest.weekOverWeekDelta ?: 0.0, 0.1) // 78.85% - 80.0%
    }

    @Test
    fun scheduledNoticesAreHiddenUntilScheduledTimestampArrives() {
        val profile = UserProfile(grade = "Grade 13", alStream = "Physical Science", className = "13M1")
        val nowMs = 1_760_000_000_000L
        val liveNotice = com.example.data.Notice(
            id = "n_live",
            title = "Live Notice",
            message = "Visible right now",
            published = true,
            scheduledAtMillis = null
        )
        val pastScheduledNotice = com.example.data.Notice(
            id = "n_past_sched",
            title = "Already Due Notice",
            message = "Scheduled 10 minutes ago",
            published = true,
            scheduledAtMillis = nowMs - 600_000L
        )
        val futureScheduledNotice = com.example.data.Notice(
            id = "n_future_sched",
            title = "Tomorrow Morning Notice",
            message = "Scheduled for tomorrow 7 AM",
            published = true,
            scheduledAtMillis = nowMs + 3_600_000L
        )

        val beforeDue = AttendanceCalculator.filterNoticesForStudent(
            notices = listOf(liveNotice, pastScheduledNotice, futureScheduledNotice),
            profile = profile,
            nowMillis = nowMs
        )
        assertEquals(listOf("n_past_sched", "n_live"), beforeDue.map { it.id })

        val afterDue = AttendanceCalculator.filterNoticesForStudent(
            notices = listOf(liveNotice, pastScheduledNotice, futureScheduledNotice),
            profile = profile,
            nowMillis = nowMs + 3_600_001L
        )
        assertEquals(listOf("n_future_sched", "n_past_sched", "n_live"), afterDue.map { it.id })
    }

    @Test
    fun dynamicGreetingAndContextAdaptsToTimeHolidayStreakAndRemainingSchoolDays() {
        val vesakPoya = CalendarEvent(
            id = "poya_vesak",
            title = "Vesak Poya",
            dateKey = "2026-05-12",
            eventType = CalendarEventType.POYA.raw
        )
        val weekly = listOf(
            com.example.data.WeekDaySummary("Mon", "2026-05-11", status = null, holidayLabel = null, isToday = true, isFuture = false),
            com.example.data.WeekDaySummary("Tue", "2026-05-12", status = null, holidayLabel = "Vesak Poya", isToday = false, isFuture = true),
            com.example.data.WeekDaySummary("Wed", "2026-05-13", status = null, holidayLabel = null, isToday = false, isFuture = true),
            com.example.data.WeekDaySummary("Thu", "2026-05-14", status = null, holidayLabel = null, isToday = false, isFuture = true),
            com.example.data.WeekDaySummary("Fri", "2026-05-15", status = null, holidayLabel = null, isToday = false, isFuture = true)
        )

        // 1. Holiday tomorrow takes priority
        val poyaTomorrowCtx = AttendanceCalculator.buildDynamicGreetingAndContext(
            greetingName = "Yasiru",
            todayKey = "2026-05-11",
            hourOfDay = 14,
            todayIsWeekend = false,
            todayRecord = null,
            currentStreak = 5,
            weeklySummary = weekly,
            nextHoliday = vesakPoya
        )
        assertEquals("Good afternoon, Yasiru", poyaTomorrowCtx.greetingTitle)
        assertEquals("Vesak Poya tomorrow — enjoy the break", poyaTomorrowCtx.contextBannerText)
        assertTrue(poyaTomorrowCtx.isHolidayTomorrow)

        // 2. Active streak >= 3 on unlogged school day (when tomorrow is not a holiday)
        val streakCtx = AttendanceCalculator.buildDynamicGreetingAndContext(
            greetingName = "Yasiru",
            todayKey = "2026-05-13",
            hourOfDay = 9,
            todayIsWeekend = false,
            todayRecord = null,
            currentStreak = 5,
            weeklySummary = weekly,
            nextHoliday = null
        )
        assertEquals("Good morning, Yasiru", streakCtx.greetingTitle)
        assertTrue(streakCtx.isStreakAlert)
        assertTrue(streakCtx.contextBannerText.contains("5-day streak on the line today"))

        // 3. Remaining school days this week when today is already logged
        val loggedMonday = AttendanceRecord(dateKey = "2026-05-11", status = AttendanceStatus.PRESENT.raw)
        val schoolDaysLeftCtx = AttendanceCalculator.buildDynamicGreetingAndContext(
            greetingName = "Yasiru",
            todayKey = "2026-05-11",
            hourOfDay = 15,
            todayIsWeekend = false,
            todayRecord = loggedMonday,
            currentStreak = 2,
            weeklySummary = weekly,
            nextHoliday = null
        )
        assertEquals("Good afternoon, Yasiru", schoolDaysLeftCtx.greetingTitle)
        // Tue is Vesak Poya, so Wed/Thu/Fri = 3 school days left this week
        assertEquals("Good afternoon · 3 school days left this week", schoolDaysLeftCtx.contextBannerText)
    }

    @Test
    fun batchCountdownMilestonesResolveFor300_200_100_50_30Days() {
        listOf(300, 200, 100, 50, 30).forEach { milestone ->
            val resolved = AttendanceCalculator.resolveBatchCountdownMilestone(
                daysUntilAlExam = milestone,
                examYear = 2027
            )
            assertEquals(milestone, resolved?.milestoneDays)
            assertEquals("${milestone}D MILESTONE", resolved?.badgeText)
            assertTrue(resolved?.headline?.contains("2027") == true)
        }
        // Within 2-day post-milestone celebratory window (e.g. 99d left for 100d milestone)
        val windowMatch = AttendanceCalculator.resolveBatchCountdownMilestone(
            daysUntilAlExam = 99,
            examYear = 2027
        )
        assertEquals(100, windowMatch?.milestoneDays)

        // Non-milestone day returns null
        val nonMilestone = AttendanceCalculator.resolveBatchCountdownMilestone(
            daysUntilAlExam = 145,
            examYear = 2027
        )
        assertEquals(null, nonMilestone)
    }

    @Test
    fun dateUtilsScheduledDateTimeRoundTripsCleanly() {
        val millis = com.example.data.DateUtils.parseDateTimeMillis("2026-11-26", "07:30")
        assertTrue(millis != null && millis > 0L)
        val (dateKey, time24h) = com.example.data.DateUtils.toDateKeyAndTime24h(millis!!)
        assertEquals("2026-11-26", dateKey)
        assertEquals("07:30", time24h)
        assertEquals(null, com.example.data.DateUtils.parseDateTimeMillis("2026-11-26", "25:99"))
    }
}






