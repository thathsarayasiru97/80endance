package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

object AppConstants {
    const val ADMIN_EMAIL = "thathsarayasiru97@gmail.com"
    const val DEFAULT_SCHOOL_NAME = "Royal College, Colombo"
    const val DEFAULT_ELIGIBILITY_PERCENT = 80
    const val DEFAULT_REMINDER_HOUR = 14
    const val DEFAULT_REMINDER_MINUTE = 0
    const val EXTRA_OPEN_CHECKIN = "extra_open_checkin"
    const val EXTRA_SHORTCUT_ACTION = "extra_shortcut_action"
    const val SHORTCUT_MARK_PRESENT = "shortcut_mark_present"
    const val SHORTCUT_MARK_ABSENT = "shortcut_mark_absent"
    const val SHORTCUT_OPEN_CALENDAR = "shortcut_open_calendar"
    const val SHORTCUT_OPEN_REPORTS = "shortcut_open_reports"

    val GRADES = listOf("Grade 12", "Grade 13")
    val AL_STREAMS = listOf(
        "Physical Science",
        "Bio Science",
        "Commerce",
        "Arts",
        "Technology"
    )
    val EXAM_YEARS = listOf(2026, 2027, 2028)

    fun defaultExamYearForGrade(grade: String): Int = when (grade.trim()) {
        "Grade 12" -> 2028
        "Grade 13" -> 2026
        else -> 2026
    }

    fun defaultSubjectsForStream(stream: String): List<String> = when (stream) {
        "Physical Science" -> listOf("Combined Maths", "Physics", "Chemistry")
        "Bio Science" -> listOf("Biology", "Chemistry", "Physics")
        "Commerce" -> listOf("Accounting", "Business Studies", "Economics")
        "Arts" -> listOf("Economics", "Political Science", "Logic & Scientific Method")
        "Technology" -> listOf("Engineering Tech", "Science for Tech", "ICT")
        else -> listOf("Combined Maths", "Physics", "Chemistry")
    }
}

enum class AttendanceStatus(val raw: String, val label: String, val shortLabel: String) {
    PRESENT("present", "Present", "Present"),
    ABSENT("absent", "Absent", "Absent"),
    MEDICAL("medical", "Medical", "Medical"),
    SCHOOL_EVENT("school_event", "Excused", "Excused"),
    HOLIDAY("holiday", "No School / Holiday", "No School");

    companion object {
        fun fromRaw(raw: String?): AttendanceStatus =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: PRESENT
    }
}

enum class NoticeCategory(val raw: String, val label: String) {
    TEACHER_ABSENT("teacher_absent", "Teacher Absent"),
    SCHOOL_EVENT("school_event", "School Event"),
    SCHEDULE_CHANGE("schedule_change", "Schedule Change"),
    EXAM("exam", "Exam Notice"),
    NORMAL_NOTE("normal_note", "Normal Note"),
    TIP("tip", "Tip ✨"),
    GENERAL("general", "School Note");

    companion object {
        fun fromRaw(raw: String?): NoticeCategory =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: GENERAL
    }
}

enum class AppThemeMode(val raw: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    OLED_BLACK("oled_black", "OLED");

    companion object {
        fun fromRaw(raw: String?): AppThemeMode =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: SYSTEM
    }
}

enum class AppAccentPalette(val raw: String, val label: String) {
    ROYAL_BLUE("royal_blue", "Royal Blue"),
    EMERALD("emerald", "Emerald"),
    CRIMSON("crimson", "Crimson"),
    AMETHYST("amethyst", "Amethyst");

    companion object {
        fun fromRaw(raw: String?): AppAccentPalette =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: ROYAL_BLUE
    }
}

enum class FeedbackType(val raw: String, val label: String) {
    BUG("bug", "Bug Report"),
    SUGGESTION("suggestion", "Feature Idea"),
    CATALOG_REQUEST("catalog_request", "Missing Subject / Class");

    companion object {
        fun fromRaw(raw: String?): FeedbackType =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: SUGGESTION
    }
}

enum class FeedbackStatus(val raw: String, val label: String) {
    OPEN("open", "Open"),
    REVIEWED("reviewed", "Reviewed"),
    RESOLVED("resolved", "Resolved");

    companion object {
        fun fromRaw(raw: String?): FeedbackStatus =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: OPEN
    }
}

enum class NoticeTargetType(val raw: String, val label: String) {
    ALL("all", "All Students"),
    GRADE("grade", "Grade"),
    STREAM("stream", "A/L Stream"),
    CLASS("class", "Specific Class");

    companion object {
        fun fromRaw(raw: String?): NoticeTargetType =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: ALL
    }
}

enum class CalendarEventTargetType(val raw: String, val label: String) {
    ALL("all", "Everyone"),
    GRADE("grade", "Grade"),
    STREAM("stream", "A/L Stream"),
    CLASS("class", "Specific Class");

    companion object {
        fun fromRaw(raw: String?): CalendarEventTargetType =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: ALL
    }
}

enum class CalendarEventType(
    val raw: String,
    val label: String,
    val isSriLankanHoliday: Boolean
) {
    POYA("poya", "Poya Day", isSriLankanHoliday = true),
    PUBLIC_HOLIDAY("public_holiday", "Public Holiday", isSriLankanHoliday = true),
    STUDY_LEAVE("study_leave", "Exam Break / Study Leave", isSriLankanHoliday = false),
    SCHOOL_HOLIDAY("school_holiday", "No School (School Off)", isSriLankanHoliday = false),
    TERM_BREAK("term_break", "Term Break", isSriLankanHoliday = false),
    SCHOOL_EVENT("school_event", "School Event", isSriLankanHoliday = false),
    EXAM("exam", "Exam / Term Test", isSriLankanHoliday = false);

    companion object {
        val sriLankanHolidayTypes: List<CalendarEventType>
            get() = entries.filter { it.isSriLankanHoliday }

        val schoolUpdateTypes: List<CalendarEventType>
            get() = entries.filterNot { it.isSriLankanHoliday }

        fun fromRaw(raw: String?): CalendarEventType =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: POYA
    }
}

enum class SyncStatus(val label: String) {
    SYNCED("Synced"),
    SYNCING("Syncing…"),
    OFFLINE_CACHED("Offline • Saved locally")
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val nickname: String? = null,
    val email: String = "",
    val schoolName: String = AppConstants.DEFAULT_SCHOOL_NAME,
    val grade: String = "Grade 13",
    val alStream: String = "Physical Science",
    val className: String = "",
    val alExamYear: Int = 2026,
    val subjects: List<String> = emptyList(),
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = AppConstants.DEFAULT_REMINDER_HOUR,
    val reminderMinute: Int = AppConstants.DEFAULT_REMINDER_MINUTE,
    val countExcusedAsPresent: Boolean = true,
    val priorPresentCount: Int = 0,
    val priorAbsentCount: Int = 0,
    val setupCompleted: Boolean = false,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val medicalCount: Int = 0,
    val schoolEventCount: Int = 0,
    val lastCheckInDate: String? = null,
    val photoUrl: String? = null,
    val customAvatarBase64: String? = null,
    val baselineStartDate: String? = null,
    val baselineEndDate: String? = null,
    val baselinePercent: Double? = null,
    val appVersionName: String? = null,
    val appVersionCode: Int? = null,
    val onlineNow: Boolean = false,
    @ServerTimestamp val lastSeenAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val greetingName: String
        get() = nickname?.trim()?.takeIf { it.isNotEmpty() }
            ?: firstName.trim().takeIf { it.isNotEmpty() }
            ?: "Student"

    val fullName: String
        get() = listOf(firstName.trim(), lastName.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifEmpty { email.substringBefore("@") }

    val preferredDisplayName: String
        get() = fullName.ifBlank { greetingName }

    val hasLockedBaseline: Boolean
        get() = !baselineStartDate.isNullOrBlank() &&
            !baselineEndDate.isNullOrBlank() &&
            baselineStartDate.length == 10 &&
            baselineEndDate.length == 10 &&
            baselineStartDate <= baselineEndDate &&
            (priorPresentCount > 0 || priorAbsentCount > 0)

    val lockedBaselineRange: Pair<String, String>?
        get() = if (hasLockedBaseline) baselineStartDate!! to baselineEndDate!! else null

    fun isDateInLockedBaseline(dateKey: String): Boolean {
        val range = lockedBaselineRange ?: return false
        return dateKey.length == 10 && dateKey <= range.second
    }

    val quickOverallPercent: Double
        get() {
            val effectivePresent = presentCount + priorPresentCount + medicalCount + schoolEventCount
            val effectiveAbsent = absentCount + priorAbsentCount
            val total = effectivePresent + effectiveAbsent
            return if (total <= 0) 100.0 else (effectivePresent.toDouble() / total.toDouble()) * 100.0
        }

    /**
     * Treats a student as live online only if [onlineNow] is true and their heartbeat
     * [lastSeenAt] arrived within the last 4 minutes (prevents stale online states).
     */
    fun isCurrentlyOnline(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!onlineNow) return false
        val seenMillis = lastSeenAt?.toDate()?.time ?: return false
        val ageMs = nowMillis - seenMillis
        return ageMs in -60_000L..240_000L
    }
}

@Keep
@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val termId: String = "term_3_2026",
    val status: String = AttendanceStatus.PRESENT.raw,
    val subjectTags: List<String> = emptyList(),
    val note: String? = null,
    val markedBy: String = "student",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusEnum: AttendanceStatus
        get() = AttendanceStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class Notice(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = NoticeCategory.GENERAL.raw,
    val pinned: Boolean = false,
    val targetType: String = NoticeTargetType.ALL.raw,
    val targetValue: String? = null,
    val eventDate: String? = null,
    val priority: String = "normal",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: NoticeCategory
        get() = NoticeCategory.fromRaw(category)

    val targetTypeEnum: NoticeTargetType
        get() = NoticeTargetType.fromRaw(targetType)

    val urgentFlag: Boolean
        get() = priority.equals("urgent", ignoreCase = true)
}

@Keep
@IgnoreExtraProperties
data class AcademicTerm(
    @DocumentId val id: String = "",
    val name: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val academicYear: Int = 2026,
    val minEligibilityPercent: Int = AppConstants.DEFAULT_ELIGIBILITY_PERCENT,
    val active: Boolean = false,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CalendarEvent(
    @DocumentId val id: String = "",
    val title: String = "",
    val dateKey: String = "",
    val eventType: String = CalendarEventType.POYA.raw,
    val academicYear: Int = 2026,
    val description: String? = null,
    val published: Boolean = true,
    val targetType: String = CalendarEventTargetType.ALL.raw,
    val targetValue: String? = null,
    val targetAll: Boolean? = null,
    val targetGrades: List<String> = emptyList(),
    val targetStreams: List<String> = emptyList(),
    val targetClasses: List<String> = emptyList(),
    val targetUserIds: List<String> = emptyList(),
    val audienceSummary: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val eventTypeEnum: CalendarEventType
        get() = CalendarEventType.fromRaw(eventType)

    val targetTypeEnum: CalendarEventTargetType
        get() = CalendarEventTargetType.fromRaw(targetType)

    val nonSchoolDay: Boolean
        get() = eventTypeEnum != CalendarEventType.EXAM && eventTypeEnum != CalendarEventType.SCHOOL_EVENT

    val isSriLankanHoliday: Boolean
        get() = eventTypeEnum.isSriLankanHoliday

    val hasMultiAudienceTargeting: Boolean
        get() = targetAll != null ||
            targetGrades.isNotEmpty() ||
            targetStreams.isNotEmpty() ||
            targetClasses.isNotEmpty() ||
            targetUserIds.isNotEmpty()

    val isTargetedToSpecificGroup: Boolean
        get() = if (hasMultiAudienceTargeting) {
            targetAll != true
        } else {
            targetTypeEnum != CalendarEventTargetType.ALL
        }

    val targetDisplayLabel: String?
        get() {
            if (hasMultiAudienceTargeting) {
                if (targetAll == true) return null
                audienceSummary?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
                val parts = buildList {
                    if (targetGrades.isNotEmpty()) add(targetGrades.joinToString(", "))
                    if (targetStreams.isNotEmpty()) add(targetStreams.joinToString(", "))
                    if (targetClasses.isNotEmpty()) add(targetClasses.joinToString(", "))
                    if (targetUserIds.isNotEmpty()) add("${targetUserIds.size} student${if (targetUserIds.size == 1) "" else "s"}")
                }
                return parts.joinToString(" • ").ifEmpty { "Selected Group" }
            }
            return when (targetTypeEnum) {
                CalendarEventTargetType.ALL -> null
                else -> targetValue?.trim()?.takeIf { it.isNotEmpty() } ?: targetTypeEnum.label
            }
        }

    fun appliesToStudent(profile: UserProfile?, userId: String = profile?.userId.orEmpty()): Boolean {
        if (hasMultiAudienceTargeting) {
            if (targetAll == true) return true
            if (userId.isNotBlank() && targetUserIds.any { it == userId }) return true
            if (profile == null) return false
            val grade = profile.grade.trim()
            val stream = profile.alStream.trim()
            val cls = profile.className.trim()
            return (grade.isNotEmpty() && targetGrades.any { it.trim().equals(grade, ignoreCase = true) }) ||
                (stream.isNotEmpty() && targetStreams.any { it.trim().equals(stream, ignoreCase = true) }) ||
                (cls.isNotEmpty() && targetClasses.any { it.trim().equals(cls, ignoreCase = true) })
        }
        return when (targetTypeEnum) {
            CalendarEventTargetType.ALL -> true
            CalendarEventTargetType.GRADE ->
                profile != null &&
                    !targetValue.isNullOrBlank() &&
                    profile.grade.trim().equals(targetValue.trim(), ignoreCase = true)
            CalendarEventTargetType.STREAM ->
                profile != null &&
                    !targetValue.isNullOrBlank() &&
                    profile.alStream.trim().equals(targetValue.trim(), ignoreCase = true)
            CalendarEventTargetType.CLASS ->
                profile != null &&
                    !targetValue.isNullOrBlank() &&
                    profile.className.trim().equals(targetValue.trim(), ignoreCase = true)
        }
    }
}

@Keep
@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "global",
    val defaultEligibilityPercent: Int = AppConstants.DEFAULT_ELIGIBILITY_PERCENT,
    val defaultReminderHour: Int = AppConstants.DEFAULT_REMINDER_HOUR,
    val defaultReminderMinute: Int = AppConstants.DEFAULT_REMINDER_MINUTE,
    val currentAcademicYear: Int = 2026,
    val al2026ExamDate: String = "2026-11-25",
    val al2027ExamDate: String = "2027-08-09",
    val al2028ExamDate: String = "2028-08-07",
    val nextDayPollEnabled: Boolean = false,
    val schoolStartDate: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val validSchoolStartDate: String?
        get() = schoolStartDate?.trim()?.takeIf { it.matches(DATE_KEY_REGEX) }

    fun effectiveSchoolStartDate(examYear: Int? = null): String {
        validSchoolStartDate?.let { return it }
        val startYear = ((examYear ?: 2028) - 2).coerceAtLeast(2024)
        return "$startYear-01-15"
    }

    fun examDateForYear(year: Int): String = when (year) {
        2026 -> al2026ExamDate
        2027 -> al2027ExamDate
        2028 -> al2028ExamDate
        else -> "$year-08-10"
    }

    companion object {
        private val DATE_KEY_REGEX = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
    }
}

@Keep
@IgnoreExtraProperties
data class FeedbackReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val className: String? = null,
    val type: String = FeedbackType.SUGGESTION.raw,
    val title: String = "",
    val message: String = "",
    val appVersion: String = "",
    val status: String = FeedbackStatus.OPEN.raw,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val typeEnum: FeedbackType
        get() = FeedbackType.fromRaw(type)

    val statusEnum: FeedbackStatus
        get() = FeedbackStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class CommunityStats(
    @DocumentId val id: String = "community",
    val totalAccounts: Int = 0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BroadcastNotification(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetAll: Boolean = true,
    val targetGrades: List<String> = emptyList(),
    val targetStreams: List<String> = emptyList(),
    val targetClasses: List<String> = emptyList(),
    val targetUserIds: List<String> = emptyList(),
    val audienceSummary: String = "All Students",
    val sentByEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun matchesStudent(profile: UserProfile?, userId: String): Boolean {
        if (targetAll) return true
        if (userId.isNotBlank() && targetUserIds.any { it == userId }) return true
        if (profile == null) return false
        val grade = profile.grade.trim()
        val stream = profile.alStream.trim()
        val cls = profile.className.trim()
        return (grade.isNotEmpty() && targetGrades.any { it.trim().equals(grade, ignoreCase = true) }) ||
            (stream.isNotEmpty() && targetStreams.any { it.trim().equals(stream, ignoreCase = true) }) ||
            (cls.isNotEmpty() && targetClasses.any { it.trim().equals(cls, ignoreCase = true) })
    }
}

@Keep
@IgnoreExtraProperties
data class AcademicCatalogDoc(
    @DocumentId val id: String = "academic_catalog",
    val streamSubjectBucketsJson: String = "",
    val gradeStreamClassesJson: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCatalog(): AcademicCatalog = AcademicCatalog.fromFirestoreJson(
        streamSubjectBucketsJson = streamSubjectBucketsJson,
        gradeStreamClassesJson = gradeStreamClassesJson
    )
}

enum class AttendancePeriodEntryMode(val raw: String, val label: String) {
    MISSED_DAYS("missed_days", "Days Missed"),
    ATTENDED_DAYS("attended_days", "Days Attended"),
    PERCENTAGE("percentage", "Percentage %");

    companion object {
        fun fromRaw(raw: String?): AttendancePeriodEntryMode =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: MISSED_DAYS
    }
}

@Keep
@IgnoreExtraProperties
data class AttendancePeriodBlock(
    @DocumentId val id: String = "",
    val userId: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val totalSchoolDays: Int = 0,
    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val percentage: Double = 100.0,
    val entryMode: String = AttendancePeriodEntryMode.MISSED_DAYS.raw,
    val label: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val entryModeEnum: AttendancePeriodEntryMode
        get() = AttendancePeriodEntryMode.fromRaw(entryMode)

    fun containsDate(dateKey: String): Boolean =
        dateKey.length == 10 &&
            startDate.length == 10 &&
            endDate.length == 10 &&
            dateKey >= startDate &&
            dateKey <= endDate

    fun overlapsRange(otherStart: String, otherEnd: String): Boolean =
        startDate.length == 10 &&
            endDate.length == 10 &&
            otherStart.length == 10 &&
            otherEnd.length == 10 &&
            startDate <= otherEnd &&
            otherStart <= endDate
}

/**
 * Privacy-safe, PII-free numeric attendance trend summary for a single student.
 * Read by signed-in students solely to compute accurate school-wide Monthly & All-Time
 * Trends & Patterns across all accounts without exposing names, emails, or individual day notes.
 */
@Keep
@IgnoreExtraProperties
data class StudentTrendSummary(
    @DocumentId val id: String = "",
    val userId: String = "",
    val monthlyTotalsJson: String = "{}",
    val monthlyWeekdayJson: String = "{}",
    val allTimeWeekdayAttended: List<Int> = listOf(0, 0, 0, 0, 0),
    val allTimeWeekdayAbsent: List<Int> = listOf(0, 0, 0, 0, 0),
    val totalAttended: Int = 0,
    val totalAbsent: Int = 0,
    val grade: String? = null,
    val alStream: String? = null,
    val className: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val totalCounted: Int get() = totalAttended + totalAbsent
}

enum class PersonalMarkerCategory(val raw: String, val label: String) {
    EXAM("exam", "Term Test / Exam"),
    PRACTICAL("practical", "Practical / Lab"),
    DEADLINE("deadline", "SBA / Deadline"),
    TUITION("tuition", "Revision / Class"),
    OTHER("other", "Personal Note");

    companion object {
        fun fromRaw(raw: String?): PersonalMarkerCategory =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: OTHER
    }
}

@Keep
@IgnoreExtraProperties
data class PersonalAcademicMarker(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val title: String = "",
    val category: String = PersonalMarkerCategory.EXAM.raw,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: PersonalMarkerCategory
        get() = PersonalMarkerCategory.fromRaw(category)
}

enum class CelebrationType {
    PERFECT_WEEK,
    STREAK_MILESTONE,
    ELIGIBILITY_RECOVERY
}

data class CelebrationEvent(
    val type: CelebrationType,
    val title: String,
    val subtitle: String,
    val badgeLabel: String
)

data class UndoableCheckInAction(
    val id: Long = System.nanoTime(),
    val message: String,
    val dateKey: String,
    val previousRecord: AttendanceRecord?
)

@Keep
@IgnoreExtraProperties
data class NextDaySchoolVote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetDateKey: String = "",
    val going: Boolean = true,
    val grade: String? = null,
    val alStream: String? = null,
    val className: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)



