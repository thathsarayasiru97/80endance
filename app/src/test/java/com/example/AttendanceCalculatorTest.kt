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
}

