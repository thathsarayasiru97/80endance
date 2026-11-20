package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AcademicTerm
import com.example.data.AppSettings
import com.example.data.AttendanceCalculator
import com.example.data.AttendanceRecord
import com.example.data.AttendanceRepository
import com.example.data.AttendanceStats
import com.example.data.AttendanceStatus
import com.example.data.CalendarEvent
import com.example.data.DateUtils
import com.example.data.LocalPreferencesStore
import com.example.data.LocalReminderPrefs
import com.example.data.Notice
import com.example.data.SriLankaCalendarDefaults
import com.example.data.SyncStatus
import com.example.data.UserProfile
import com.example.data.WeekDaySummary
import com.example.system.ReminderScheduler
import com.example.system.SyncScheduler
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DashboardScopeMode {
    CURRENT_TERM,
    OVERALL_AL
}

data class StudentAppState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val profile: UserProfile? = null,
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val visibleNotices: List<Notice> = emptyList(),
    val pinnedNotices: List<Notice> = emptyList(),
    val terms: List<AcademicTerm> = SriLankaCalendarDefaults.defaultTerms,
    val activeTerm: AcademicTerm = SriLankaCalendarDefaults.defaultTerms[2],
    val calendarEvents: List<CalendarEvent> = SriLankaCalendarDefaults.defaultEvents,
    val appSettings: AppSettings = AppSettings(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val currentTermStats: AttendanceStats = AttendanceCalculator.calculateStats(
        emptyList(), 80, true, false, 0, 0
    ),
    val overallStats: AttendanceStats = AttendanceCalculator.calculateStats(
        emptyList(), 80, true, true, 0, 0
    ),
    val currentStreak: Int = 0,
    val weeklySummary: List<WeekDaySummary> = emptyList(),
    val nextPoyaOrHoliday: CalendarEvent? = null,
    val daysUntilAlExam: Int = 0,
    val todayRecord: AttendanceRecord? = null,
    val todayIsSchoolDay: Boolean = true,
    val shouldAutoShowCheckIn: Boolean = false
)

private data class CoreFirestoreBundle(
    val profile: UserProfile? = null,
    val profileLoaded: Boolean = false,
    val records: List<AttendanceRecord> = emptyList(),
    val notices: List<Notice> = emptyList(),
    val terms: List<AcademicTerm> = emptyList(),
    val events: List<CalendarEvent> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val streamError: String? = null
)

class AttendanceViewModel(
    private val repository: AttendanceRepository,
    private val localPrefs: LocalPreferencesStore,
    private val appContext: Context,
    val currentUserId: String,
    val userEmail: String,
    val googleDisplayName: String?
) : ViewModel() {

    private val _scopeMode = MutableStateFlow(DashboardScopeMode.CURRENT_TERM)
    val scopeMode: StateFlow<DashboardScopeMode> = _scopeMode.asStateFlow()

    private val _manualCheckInDate = MutableStateFlow<String?>(null)
    val manualCheckInDate: StateFlow<String?> = _manualCheckInDate.asStateFlow()

    private val _sessionDismissedCheckIn = MutableStateFlow(false)

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private fun <T> Flow<T>.safeFirestoreFlow(fallback: T, tag: String): Flow<Pair<T, String?>> =
        this.combine(MutableStateFlow(Unit)) { value, _ -> value to (null as String?) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w("AttendanceVM", "Stream error ($tag)", root)
                emit(fallback to (root.message ?: "Failed to load $tag"))
            }

    private val coreBundleFlow: Flow<CoreFirestoreBundle> = combine(
        repository.observeUserProfile(currentUserId).safeFirestoreFlow(null, "profile"),
        repository.observeAttendance(currentUserId).safeFirestoreFlow(emptyList(), "attendance"),
        repository.observePublishedNotices().safeFirestoreFlow(emptyList(), "notices"),
        repository.observePublishedTerms().safeFirestoreFlow(emptyList(), "terms"),
        repository.observePublishedCalendarEvents().safeFirestoreFlow(emptyList(), "calendar")
    ) { profilePair, recordsPair, noticesPair, termsPair, eventsPair ->
        val firstErr = listOfNotNull(
            profilePair.second,
            recordsPair.second,
            noticesPair.second,
            termsPair.second,
            eventsPair.second
        ).firstOrNull()
        CoreFirestoreBundle(
            profile = profilePair.first,
            profileLoaded = true,
            records = recordsPair.first,
            notices = noticesPair.first,
            terms = termsPair.first,
            events = eventsPair.first,
            streamError = firstErr
        )
    }

    private val settingsFlow = repository.observeAppSettings()
        .safeFirestoreFlow(AppSettings(), "settings")

    private val networkFlow = SyncScheduler.observeNetworkConnectivity(appContext)

    val uiState: StateFlow<StudentAppState> = combine(
        coreBundleFlow,
        settingsFlow,
        networkFlow,
        repository.hasPendingWrites,
        combine(localPrefs.prefsFlow, _sessionDismissedCheckIn) { prefs, dismissed -> prefs to dismissed }
    ) { bundle, settingsPair, isOnline, hasPending, prefsAndDismissed ->
        val (prefs, sessionDismissed) = prefsAndDismissed
        val settings = settingsPair.first
        val profile = bundle.profile
        val effectiveTerms = bundle.terms.ifEmpty { SriLankaCalendarDefaults.defaultTerms }
        val effectiveEvents = bundle.events.ifEmpty { SriLankaCalendarDefaults.defaultEvents }
        val todayKey = DateUtils.todayKey()
        val activeTerm = AttendanceCalculator.resolveActiveTerm(effectiveTerms, todayKey)
        val threshold = activeTerm.minEligibilityPercent.takeIf { it in 50..100 }
            ?: settings.defaultEligibilityPercent
        val countExcused = profile?.countExcusedAsPresent ?: true

        val termStats = AttendanceCalculator.calculateStats(
            records = bundle.records,
            thresholdPercent = threshold,
            countExcusedAsPresent = countExcused,
            includePriorBalance = true,
            priorPresent = profile?.priorPresentCount ?: 0,
            priorAbsent = profile?.priorAbsentCount ?: 0,
            termFilter = activeTerm
        )
        val overallStats = AttendanceCalculator.calculateStats(
            records = bundle.records,
            thresholdPercent = threshold,
            countExcusedAsPresent = countExcused,
            includePriorBalance = true,
            priorPresent = profile?.priorPresentCount ?: 0,
            priorAbsent = profile?.priorAbsentCount ?: 0,
            termFilter = null
        )

        val streak = AttendanceCalculator.calculateCurrentStreak(
            records = bundle.records,
            calendarEvents = effectiveEvents,
            countExcusedAsPresent = countExcused,
            todayKey = todayKey
        )
        val weekly = AttendanceCalculator.calculateWeeklySummary(
            records = bundle.records,
            calendarEvents = effectiveEvents,
            todayKey = todayKey
        )
        val nextHoliday = AttendanceCalculator.nextUpcomingPoyaOrHoliday(effectiveEvents, todayKey)
        val examYear = profile?.alExamYear ?: settings.currentAcademicYear
        val daysToExam = AttendanceCalculator.daysUntilAlExam(examYear, settings, todayKey)
        val visibleNotices = AttendanceCalculator.filterNoticesForStudent(bundle.notices, profile)
        val pinnedNotices = visibleNotices.filter { it.pinned }
        val todayRecord = bundle.records.firstOrNull { it.dateKey == todayKey }
        val todayIsSchoolDay = AttendanceCalculator.isSchoolDay(todayKey, effectiveEvents)

        val (nowHour, nowMinute) = DateUtils.currentHourMinute()
        val remHour = profile?.reminderHour ?: prefs.hour
        val remMin = profile?.reminderMinute ?: prefs.minute
        val pastReminderTime = (nowHour > remHour) || (nowHour == remHour && nowMinute >= remMin)

        val shouldAutoPrompt = profile?.setupCompleted == true &&
            todayIsSchoolDay &&
            todayRecord == null &&
            pastReminderTime &&
            !sessionDismissed &&
            prefs.lastDismissedDate != todayKey

        val syncStatus = when {
            !isOnline -> SyncStatus.OFFLINE_CACHED
            hasPending -> SyncStatus.SYNCING
            else -> SyncStatus.SYNCED
        }

        StudentAppState(
            isLoading = !bundle.profileLoaded,
            errorMessage = bundle.streamError,
            profile = profile,
            attendanceRecords = bundle.records,
            visibleNotices = visibleNotices,
            pinnedNotices = pinnedNotices,
            terms = effectiveTerms,
            activeTerm = activeTerm,
            calendarEvents = effectiveEvents,
            appSettings = settings,
            syncStatus = syncStatus,
            currentTermStats = termStats,
            overallStats = overallStats,
            currentStreak = streak,
            weeklySummary = weekly,
            nextPoyaOrHoliday = nextHoliday,
            daysUntilAlExam = daysToExam,
            todayRecord = todayRecord,
            todayIsSchoolDay = todayIsSchoolDay,
            shouldAutoShowCheckIn = shouldAutoPrompt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = StudentAppState(isLoading = true)
    )

    fun setScopeMode(mode: DashboardScopeMode) {
        _scopeMode.value = mode
    }

    fun openCheckInPopup(dateKey: String = DateUtils.todayKey()) {
        _manualCheckInDate.value = dateKey
    }

    fun dismissCheckInPopup(markDismissedToday: Boolean) {
        _manualCheckInDate.value = null
        if (markDismissedToday) {
            _sessionDismissedCheckIn.value = true
            viewModelScope.launch {
                localPrefs.markPromptDismissedForDate(DateUtils.todayKey())
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun saveStudentProfile(
        firstName: String,
        lastName: String,
        nickname: String?,
        schoolName: String,
        grade: String,
        alStream: String,
        className: String,
        alExamYear: Int,
        subjects: List<String>,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        countExcusedAsPresent: Boolean,
        priorPresentCount: Int,
        priorAbsentCount: Int,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val isExisting = uiState.value.profile != null
            val result = repository.saveUserProfile(
                targetUserId = currentUserId,
                firstName = firstName,
                lastName = lastName,
                nickname = nickname,
                email = userEmail,
                schoolName = schoolName,
                grade = grade,
                alStream = alStream,
                className = className,
                alExamYear = alExamYear,
                subjects = subjects,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                countExcusedAsPresent = countExcusedAsPresent,
                priorPresentCount = priorPresentCount,
                priorAbsentCount = priorAbsentCount,
                setupCompleted = true,
                isExisting = isExisting
            )
            _isSaving.value = false
            result.onSuccess {
                localPrefs.saveReminder(reminderEnabled, reminderHour, reminderMinute)
                ReminderScheduler.scheduleDailyReminder(appContext, reminderEnabled, reminderHour, reminderMinute)
                _snackbarMessage.value = if (isExisting) "Profile updated" else "Welcome to 80endance!"
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not save profile"
            }
        }
    }

    fun logAttendance(
        dateKey: String,
        status: AttendanceStatus,
        subjectTags: List<String>,
        note: String?
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val current = uiState.value
            val termId = AttendanceCalculator.resolveTermIdForDate(dateKey, current.terms)
            val result = repository.upsertAttendance(
                targetUserId = currentUserId,
                dateKey = dateKey,
                termId = termId,
                status = status,
                subjectTags = subjectTags,
                note = note,
                markedBy = "student",
                existingRecords = current.attendanceRecords
            )
            _isSaving.value = false
            result.onSuccess {
                _manualCheckInDate.value = null
                if (dateKey == DateUtils.todayKey()) {
                    _sessionDismissedCheckIn.value = true
                }
                val offlineHint = if (current.syncStatus == SyncStatus.OFFLINE_CACHED) " (saved offline)" else ""
                _snackbarMessage.value = "Marked ${status.label} for ${DateUtils.formatReadableShort(dateKey)}$offlineHint"
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Failed to save attendance"
            }
        }
    }

    fun deleteAttendance(dateKey: String) {
        viewModelScope.launch {
            val result = repository.deleteAttendance(
                targetUserId = currentUserId,
                dateKey = dateKey,
                existingRecords = uiState.value.attendanceRecords
            )
            result.onSuccess {
                _snackbarMessage.value = "Removed entry for ${DateUtils.formatReadableShort(dateKey)}"
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Failed to remove entry"
            }
        }
    }

    fun updateReminderAndExcused(
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        countExcusedAsPresent: Boolean
    ) {
        viewModelScope.launch {
            val result = repository.updateUserPreferences(
                userId = currentUserId,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                countExcusedAsPresent = countExcusedAsPresent
            )
            result.onSuccess {
                localPrefs.saveReminder(reminderEnabled, reminderHour, reminderMinute)
                ReminderScheduler.scheduleDailyReminder(appContext, reminderEnabled, reminderHour, reminderMinute)
                _snackbarMessage.value = "Preferences saved"
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not update preferences"
            }
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            val result = repository.flushPendingWrites()
            result.onSuccess {
                _snackbarMessage.value = if (SyncScheduler.isNetworkAvailable(appContext)) {
                    "All records synced with cloud"
                } else {
                    "Offline — changes queued for automatic background sync"
                }
            }.onFailure {
                _snackbarMessage.value = "Sync queued for when internet reconnects"
            }
        }
    }
}
