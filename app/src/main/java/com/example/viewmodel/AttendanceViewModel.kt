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
import com.example.system.AttendanceWidgetStore
import com.example.system.ReminderScheduler
import com.example.system.SyncScheduler
import com.example.system.WidgetSnapshot
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
    CURRENT_MONTH,
    CURRENT_TERM,
    OVERALL_AL
}

data class StudentAppState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val profile: UserProfile? = null,
    val googlePhotoUrl: String? = null,
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val visibleNotices: List<Notice> = emptyList(),
    val pinnedNotices: List<Notice> = emptyList(),
    val terms: List<AcademicTerm> = SriLankaCalendarDefaults.defaultTerms,
    val activeTerm: AcademicTerm = SriLankaCalendarDefaults.defaultTerms[2],
    val calendarEvents: List<CalendarEvent> = SriLankaCalendarDefaults.defaultEvents,
    val appSettings: AppSettings = AppSettings(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val currentMonthLabel: String = DateUtils.currentMonthYearLabel(),
    val currentMonthStats: AttendanceStats = AttendanceCalculator.calculateStats(
        emptyList(), 80, true, false, 0, 0
    ),
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
    val todayIsWeekend: Boolean = DateUtils.isWeekend(DateUtils.todayKey()),
    val todayNonSchoolEvent: CalendarEvent? = null,
    val todayIsSchoolDay: Boolean = true,
    val shouldAutoShowCheckIn: Boolean = false,
    val missedSchoolDays: List<String> = emptyList()
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
    val googleDisplayName: String?,
    val googlePhotoUrl: String? = null
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
        val rawEvents = bundle.events.ifEmpty { SriLankaCalendarDefaults.defaultEvents }
        val effectiveEvents = AttendanceCalculator.filterCalendarEventsForStudent(rawEvents, profile)
        val todayKey = DateUtils.todayKey()
        val activeTerm = AttendanceCalculator.resolveActiveTerm(effectiveTerms, todayKey)
        val threshold = activeTerm.minEligibilityPercent.takeIf { it in 50..100 }
            ?: settings.defaultEligibilityPercent

        val monthPrefix = DateUtils.currentMonthPrefix(todayKey)
        val monthLabel = DateUtils.currentMonthYearLabel(todayKey)
        val monthStats = AttendanceCalculator.calculateStats(
            records = bundle.records,
            thresholdPercent = threshold,
            countExcusedAsPresent = true,
            includePriorBalance = false,
            priorPresent = 0,
            priorAbsent = 0,
            monthPrefixFilter = monthPrefix
        )
        val termStats = AttendanceCalculator.calculateStats(
            records = bundle.records,
            thresholdPercent = threshold,
            countExcusedAsPresent = true,
            includePriorBalance = true,
            priorPresent = profile?.priorPresentCount ?: 0,
            priorAbsent = profile?.priorAbsentCount ?: 0,
            termFilter = activeTerm
        )
        val overallStats = AttendanceCalculator.calculateStats(
            records = bundle.records,
            thresholdPercent = threshold,
            countExcusedAsPresent = true,
            includePriorBalance = true,
            priorPresent = profile?.priorPresentCount ?: 0,
            priorAbsent = profile?.priorAbsentCount ?: 0,
            termFilter = null
        )

        val streak = AttendanceCalculator.calculateCurrentStreak(
            records = bundle.records,
            calendarEvents = effectiveEvents,
            countExcusedAsPresent = true,
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
        val todayIsWeekend = DateUtils.isWeekend(todayKey)
        val todayNonSchoolEvent = effectiveEvents.firstOrNull { it.dateKey == todayKey && it.nonSchoolDay }
        val todayIsSchoolDay = !todayIsWeekend && todayNonSchoolEvent == null

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

        val missedSchoolDays = if (profile?.setupCompleted == true) {
            AttendanceCalculator.findUnloggedRecentSchoolDays(
                records = bundle.records,
                calendarEvents = effectiveEvents,
                activeTermStartDate = activeTerm.startDate,
                todayKey = todayKey,
                lookbackCalendarDays = 10,
                maxItems = 5
            )
        } else {
            emptyList()
        }

        val syncStatus = when {
            !isOnline -> SyncStatus.OFFLINE_CACHED
            hasPending -> SyncStatus.SYNCING
            else -> SyncStatus.SYNCED
        }

        if (bundle.profileLoaded && profile?.setupCompleted == true) {
            AttendanceWidgetStore.saveAndRefresh(
                appContext,
                WidgetSnapshot(
                    percentage = termStats.percentage,
                    eligible = termStats.eligible,
                    thresholdPercent = termStats.thresholdPercent,
                    totalCountedDays = termStats.totalCountedDays,
                    safeSkipsRemaining = termStats.safeSkipsRemaining,
                    daysNeededToRecover = termStats.daysNeededToRecover,
                    streak = streak,
                    termName = activeTerm.name,
                    todayDateKey = todayKey,
                    todayStatusLabel = todayRecord?.statusEnum?.label,
                    todayNoSchoolTitle = todayNonSchoolEvent?.title,
                    signedIn = true
                )
            )
        }

        StudentAppState(
            isLoading = !bundle.profileLoaded,
            errorMessage = bundle.streamError,
            profile = profile,
            googlePhotoUrl = profile?.photoUrl?.takeIf { it.isNotBlank() } ?: googlePhotoUrl,
            attendanceRecords = bundle.records,
            visibleNotices = visibleNotices,
            pinnedNotices = pinnedNotices,
            terms = effectiveTerms,
            activeTerm = activeTerm,
            calendarEvents = effectiveEvents,
            appSettings = settings,
            syncStatus = syncStatus,
            currentMonthLabel = monthLabel,
            currentMonthStats = monthStats,
            currentTermStats = termStats,
            overallStats = overallStats,
            currentStreak = streak,
            weeklySummary = weekly,
            nextPoyaOrHoliday = nextHoliday,
            daysUntilAlExam = daysToExam,
            todayRecord = todayRecord,
            todayIsWeekend = todayIsWeekend,
            todayNonSchoolEvent = todayNonSchoolEvent,
            todayIsSchoolDay = todayIsSchoolDay,
            shouldAutoShowCheckIn = shouldAutoPrompt,
            missedSchoolDays = missedSchoolDays
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = StudentAppState(isLoading = true, googlePhotoUrl = googlePhotoUrl)
    )

    fun setScopeMode(mode: DashboardScopeMode) {
        _scopeMode.value = mode
    }

    fun openCheckInPopup(dateKey: String = DateUtils.todayKey()) {
        if (DateUtils.isWeekend(dateKey)) return
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
                countExcusedAsPresent = true,
                priorPresentCount = priorPresentCount,
                priorAbsentCount = priorAbsentCount,
                setupCompleted = true,
                isExisting = isExisting,
                photoUrl = googlePhotoUrl
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

    fun updateCustomAvatar(customAvatarBase64: String?) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.updateUserAvatar(
                userId = currentUserId,
                customAvatarBase64 = customAvatarBase64,
                googlePhotoUrl = googlePhotoUrl
            )
            _isSaving.value = false
            result.onSuccess {
                _snackbarMessage.value = if (customAvatarBase64.isNullOrBlank()) {
                    "Reverted to Google account photo"
                } else {
                    "Profile picture updated & synced"
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not update profile picture"
            }
        }
    }

    fun logAttendance(
        dateKey: String,
        status: AttendanceStatus,
        subjectTags: List<String>,
        note: String?
    ) {
        if (DateUtils.isWeekend(dateKey)) {
            _snackbarMessage.value = "Weekends are automatically marked as off days"
            return
        }
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

    fun quickLogAttendance(dateKey: String, status: AttendanceStatus) {
        logAttendance(
            dateKey = dateKey,
            status = status,
            subjectTags = emptyList(),
            note = null
        )
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
