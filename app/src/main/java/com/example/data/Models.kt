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

    val GRADES = listOf("Grade 12", "Grade 13")
    val AL_STREAMS = listOf(
        "Physical Science",
        "Bio Science",
        "Commerce",
        "Arts",
        "Technology"
    )
    val EXAM_YEARS = listOf(2026, 2027, 2028)

    fun defaultSubjectsForStream(stream: String): List<String> = when (stream) {
        "Physical Science" -> listOf("Combined Maths", "Physics", "Chemistry")
        "Bio Science" -> listOf("Biology", "Chemistry", "Physics")
        "Commerce" -> listOf("Accounting", "Business Studies", "Economics")
        "Arts" -> listOf("Economics", "Political Science", "Logic")
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
    GENERAL("general", "School Note");

    companion object {
        fun fromRaw(raw: String?): NoticeCategory =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: GENERAL
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

enum class CalendarEventType(val raw: String, val label: String) {
    POYA("poya", "Poya Day"),
    PUBLIC_HOLIDAY("public_holiday", "Public Holiday"),
    TERM_BREAK("term_break", "Term Break"),
    SCHOOL_HOLIDAY("school_holiday", "School Holiday"),
    EXAM("exam", "Exam Date");

    companion object {
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
        return dateKey.length == 10 && dateKey >= range.first && dateKey <= range.second
    }

    val quickOverallPercent: Double
        get() {
            val effectivePresent = presentCount + priorPresentCount + medicalCount + schoolEventCount
            val effectiveAbsent = absentCount + priorAbsentCount
            val total = effectivePresent + effectiveAbsent
            return if (total <= 0) 100.0 else (effectivePresent.toDouble() / total.toDouble()) * 100.0
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val eventTypeEnum: CalendarEventType
        get() = CalendarEventType.fromRaw(eventType)

    val targetTypeEnum: CalendarEventTargetType
        get() = CalendarEventTargetType.fromRaw(targetType)

    val nonSchoolDay: Boolean
        get() = eventTypeEnum != CalendarEventType.EXAM

    val targetDisplayLabel: String?
        get() = when (targetTypeEnum) {
            CalendarEventTargetType.ALL -> null
            else -> targetValue?.trim()?.takeIf { it.isNotEmpty() } ?: targetTypeEnum.label
        }

    fun appliesToStudent(profile: UserProfile?): Boolean {
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
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun examDateForYear(year: Int): String = when (year) {
        2026 -> al2026ExamDate
        2027 -> al2027ExamDate
        2028 -> al2028ExamDate
        else -> "$year-08-10"
    }
}
