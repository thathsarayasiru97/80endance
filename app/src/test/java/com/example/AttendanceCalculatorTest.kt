package com.example

import com.example.data.AttendanceCalculator
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStatus
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
}
