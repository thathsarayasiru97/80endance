package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AcademicTerm
import com.example.data.AppSettings
import com.example.data.AttendanceCalculator
import com.example.data.AttendanceRecord
import com.example.data.AttendanceRepository
import com.example.data.AttendanceStatus
import com.example.data.CalendarEvent
import com.example.data.CalendarEventTargetType
import com.example.data.CalendarEventType
import com.example.data.DateUtils
import com.example.data.FeedbackReport
import com.example.data.FeedbackStatus
import com.example.data.Notice
import com.example.data.NoticeCategory
import com.example.data.NoticeTargetType
import com.example.data.SriLankaCalendarDefaults
import com.example.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminUiState(
    val students: List<UserProfile> = emptyList(),
    val notices: List<Notice> = emptyList(),
    val terms: List<AcademicTerm> = SriLankaCalendarDefaults.defaultTerms,
    val calendarEvents: List<CalendarEvent> = SriLankaCalendarDefaults.defaultEvents,
    val feedbackItems: List<FeedbackReport> = emptyList(),
    val appSettings: AppSettings = AppSettings(),
    val selectedStudent: UserProfile? = null,
    val selectedStudentRecords: List<AttendanceRecord> = emptyList(),
    val isBusy: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _selectedStudentId = MutableStateFlow<String?>(null)
    private val _isBusy = MutableStateFlow(false)
    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    private fun <T> Flow<T>.safeStream(fallback: T, name: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w("AdminVM", "Error loading $name", firestoreError ?: error)
            emit(fallback)
        }

    private val selectedRecordsFlow: Flow<List<AttendanceRecord>> =
        _selectedStudentId.flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeAttendance(uid).safeStream(emptyList(), "selectedAttendance")
            }
        }

    val uiState: StateFlow<AdminUiState> = combine(
        repository.observeAllStudentsAdmin().safeStream(emptyList(), "students"),
        repository.observeAllNoticesAdmin().safeStream(emptyList(), "notices"),
        repository.observePublishedTerms().safeStream(emptyList(), "terms"),
        repository.observePublishedCalendarEvents().safeStream(emptyList(), "events"),
        combine(
            repository.observeAppSettings().safeStream(AppSettings(), "settings"),
            repository.observeAllFeedbackAdmin().safeStream(emptyList(), "feedback"),
            _selectedStudentId,
            selectedRecordsFlow,
            _isBusy
        ) { settings, feedback, selectedId, selectedRecords, busy ->
            AdminExtras(settings, feedback, selectedId, selectedRecords, busy)
        }
    ) { students, notices, terms, events, extras ->
        val selectedStudent = students.firstOrNull { it.userId == extras.selectedId }
        AdminUiState(
            students = students,
            notices = notices,
            terms = terms.ifEmpty { SriLankaCalendarDefaults.defaultTerms },
            calendarEvents = events.ifEmpty { SriLankaCalendarDefaults.defaultEvents },
            feedbackItems = extras.feedback,
            appSettings = extras.settings,
            selectedStudent = selectedStudent,
            selectedStudentRecords = extras.selectedRecords,
            isBusy = extras.busy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AdminUiState()
    )

    fun selectStudent(userId: String?) {
        _selectedStudentId.value = userId
    }

    fun clearSnackbar() {
        _snackbar.value = null
    }

    fun saveNotice(
        noticeId: String?,
        title: String,
        message: String,
        category: NoticeCategory,
        pinned: Boolean,
        targetType: NoticeTargetType,
        targetValue: String?,
        eventDate: String?,
        urgent: Boolean,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveNotice(
                noticeId = noticeId,
                title = title,
                message = message,
                category = category,
                pinned = pinned,
                targetType = targetType,
                targetValue = targetValue,
                eventDate = eventDate,
                urgent = urgent,
                published = true
            )
            _isBusy.value = false
            res.onSuccess {
                _snackbar.value = if (noticeId == null) "Notice published" else "Notice updated"
                onDone()
            }.onFailure { e ->
                _snackbar.value = e.message ?: "Failed to save notice"
            }
        }
    }

    fun togglePin(notice: Notice) {
        viewModelScope.launch {
            val res = repository.toggleNoticePin(notice.id, !notice.pinned)
            res.onSuccess {
                _snackbar.value = if (!notice.pinned) "Pinned to dashboard slideshow" else "Unpinned from slideshow"
            }.onFailure { e ->
                _snackbar.value = e.message ?: "Could not update pin"
            }
        }
    }

    fun deleteNotice(noticeId: String) {
        viewModelScope.launch {
            val res = repository.deleteNotice(noticeId)
            res.onSuccess { _snackbar.value = "Notice deleted" }
                .onFailure { e -> _snackbar.value = e.message ?: "Delete failed" }
        }
    }

    fun overrideStudentAttendance(
        student: UserProfile,
        dateKey: String,
        status: AttendanceStatus,
        note: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val termId = AttendanceCalculator.resolveTermIdForDate(dateKey, uiState.value.terms)
            val res = repository.upsertAttendance(
                targetUserId = student.userId,
                dateKey = dateKey,
                termId = termId,
                status = status,
                subjectTags = emptyList(),
                note = note,
                markedBy = "admin",
                existingRecords = uiState.value.selectedStudentRecords
            )
            _isBusy.value = false
            res.onSuccess {
                _snackbar.value = "Updated ${student.greetingName} (${DateUtils.formatReadableShort(dateKey)})"
                onDone()
            }.onFailure { e ->
                _snackbar.value = e.message ?: "Override failed"
            }
        }
    }

    fun deleteStudentAttendance(student: UserProfile, dateKey: String) {
        viewModelScope.launch {
            val res = repository.deleteAttendance(
                targetUserId = student.userId,
                dateKey = dateKey,
                existingRecords = uiState.value.selectedStudentRecords
            )
            res.onSuccess { _snackbar.value = "Deleted attendance record ($dateKey)" }
                .onFailure { e -> _snackbar.value = e.message ?: "Delete failed" }
        }
    }

    fun updateStudentBalanceAndClass(
        student: UserProfile,
        className: String,
        alStream: String,
        priorPresent: Int,
        priorAbsent: Int,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveUserProfile(
                targetUserId = student.userId,
                firstName = student.firstName,
                lastName = student.lastName,
                nickname = student.nickname,
                email = student.email,
                schoolName = student.schoolName,
                grade = student.grade,
                alStream = alStream,
                className = className,
                alExamYear = student.alExamYear,
                subjects = student.subjects,
                reminderEnabled = student.reminderEnabled,
                reminderHour = student.reminderHour,
                reminderMinute = student.reminderMinute,
                countExcusedAsPresent = student.countExcusedAsPresent,
                priorPresentCount = priorPresent,
                priorAbsentCount = priorAbsent,
                setupCompleted = true,
                isExisting = true
            )
            _isBusy.value = false
            res.onSuccess {
                _snackbar.value = "Updated ${student.greetingName}'s details"
                onDone()
            }.onFailure { e ->
                _snackbar.value = e.message ?: "Update failed"
            }
        }
    }

    fun saveCalendarEvent(
        title: String,
        dateKey: String,
        eventType: CalendarEventType,
        description: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val year = dateKey.take(4).toIntOrNull() ?: 2026
            val res = repository.saveCalendarEvent(
                eventId = null,
                title = title,
                dateKey = dateKey,
                eventType = eventType,
                academicYear = year,
                description = description
            )
            _isBusy.value = false
            res.onSuccess {
                _snackbar.value = "Calendar event saved"
                onDone()
            }.onFailure { e ->
                _snackbar.value = e.message ?: "Could not save event"
            }
        }
    }

    fun markNoSchoolDays(
        dateKeys: List<String>,
        reason: String,
        targetType: CalendarEventTargetType,
        targetValue: String?,
        alsoPostNotice: Boolean,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val dateRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
            val validDates = dateKeys.map { it.trim() }.filter { it.matches(dateRegex) }.distinct().sorted()
            if (validDates.isEmpty()) {
                _snackbar.value = "Select at least one valid date (YYYY-MM-DD)"
                return@launch
            }
            if (targetType != CalendarEventTargetType.ALL && targetValue.isNullOrBlank()) {
                _snackbar.value = "Enter a ${targetType.label.lowercase()} to target"
                return@launch
            }
            _isBusy.value = true
            val res = repository.saveBulkNoSchoolDays(
                dateKeys = validDates,
                reason = reason,
                targetType = targetType,
                targetValue = targetValue,
                alsoPostNotice = alsoPostNotice
            )
            _isBusy.value = false
            res.onSuccess { count ->
                val scopeLabel = if (targetType == CalendarEventTargetType.ALL) {
                    "everyone"
                } else {
                    targetValue?.trim().orEmpty()
                }
                val dayText = if (count == 1) {
                    DateUtils.formatReadableShort(validDates.first())
                } else {
                    "$count days"
                }
                _snackbar.value = if (alsoPostNotice) {
                    "Marked $dayText as No School for $scopeLabel & posted notice"
                } else {
                    "Marked $dayText as No School for $scopeLabel"
                }
                onDone()
            }.onFailure { e ->
                _snackbar.value = e.message ?: "Could not mark No School days"
            }
        }
    }

    fun markNoSchoolDayForEveryone(
        dateKey: String,
        reason: String,
        alsoPostNotice: Boolean,
        onDone: () -> Unit = {}
    ) {
        markNoSchoolDays(
            dateKeys = listOf(dateKey),
            reason = reason,
            targetType = CalendarEventTargetType.ALL,
            targetValue = null,
            alsoPostNotice = alsoPostNotice,
            onDone = onDone
        )
    }

    fun deleteCalendarEvent(eventId: String) {
        viewModelScope.launch {
            val res = repository.deleteCalendarEvent(eventId)
            res.onSuccess { _snackbar.value = "Calendar event removed" }
                .onFailure { e -> _snackbar.value = e.message ?: "Delete failed" }
        }
    }

    fun saveTerm(
        name: String,
        startDate: String,
        endDate: String,
        minEligibility: Int,
        active: Boolean,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val year = startDate.take(4).toIntOrNull() ?: 2026
            val res = repository.saveAcademicTerm(
                termId = null,
                name = name,
                startDate = startDate,
                endDate = endDate,
                academicYear = year,
                minEligibilityPercent = minEligibility,
                active = active
            )
            _isBusy.value = false
            res.onSuccess {
                _snackbar.value = "Term saved"
                onDone()
            }.onFailure { e ->
                _snackbar.value = e.message ?: "Could not save term"
            }
        }
    }

    fun deleteTerm(termId: String) {
        viewModelScope.launch {
            val res = repository.deleteAcademicTerm(termId)
            res.onSuccess { _snackbar.value = "Term deleted" }
                .onFailure { e -> _snackbar.value = e.message ?: "Delete failed" }
        }
    }

    fun seedSriLankaCalendar() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.seedSriLankaCalendar2026And2027()
            _isBusy.value = false
            res.onSuccess {
                _snackbar.value = "2026–2027 Sri Lankan Poya days & school terms synced to cloud"
            }.onFailure { e ->
                _snackbar.value = e.message ?: "Seeding failed"
            }
        }
    }

    fun saveGlobalSettings(settings: AppSettings) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveAppSettings(settings)
            _isBusy.value = false
            res.onSuccess { _snackbar.value = "Global defaults updated" }
                .onFailure { e -> _snackbar.value = e.message ?: "Could not save settings" }
        }
    }

    fun rolloverTerm(
        targetTerm: AcademicTerm,
        alsoPostNotice: Boolean,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.rolloverActiveTerm(
                targetTerm = targetTerm,
                alsoPostNotice = alsoPostNotice
            )
            _isBusy.value = false
            res.onSuccess { activeName ->
                _snackbar.value = if (alsoPostNotice) {
                    "Rolled over to $activeName & posted welcome notice"
                } else {
                    "Active term switched to $activeName"
                }
                onDone()
            }.onFailure { e ->
                _snackbar.value = e.message ?: "Term rollover failed"
            }
        }
    }

    fun updateFeedbackStatus(feedbackId: String, status: FeedbackStatus) {
        viewModelScope.launch {
            val res = repository.updateFeedbackStatus(feedbackId, status)
            res.onSuccess { _snackbar.value = "Feedback marked as ${status.label}" }
                .onFailure { e -> _snackbar.value = e.message ?: "Could not update feedback" }
        }
    }

    fun deleteFeedback(feedbackId: String) {
        viewModelScope.launch {
            val res = repository.deleteFeedback(feedbackId)
            res.onSuccess { _snackbar.value = "Feedback removed" }
                .onFailure { e -> _snackbar.value = e.message ?: "Delete failed" }
        }
    }

    private data class AdminExtras(
        val settings: AppSettings,
        val feedback: List<FeedbackReport>,
        val selectedId: String?,
        val selectedRecords: List<AttendanceRecord>,
        val busy: Boolean
    )
}
