package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AcademicCatalog
import com.example.data.AcademicTerm
import com.example.data.AppAccentPalette
import com.example.data.AppSettings
import com.example.data.AppThemeMode
import com.example.data.AttendanceCalculator
import com.example.data.AttendanceMilestone
import com.example.data.AttendancePeriodBlock
import com.example.data.AttendancePeriodEntryMode
import com.example.data.AttendanceRecord
import com.example.data.AttendanceRepository
import com.example.data.AttendanceStats
import com.example.data.AttendanceStatus
import com.example.data.BroadcastNotification
import com.example.data.CalendarEvent
import com.example.data.CalendarEventTargetType
import com.example.data.CalendarEventType
import com.example.data.CelebrationEvent
import com.example.data.CelebrationType
import com.example.data.CommunityStats
import com.example.data.DateUtils
import com.example.data.FeedbackReport
import com.example.data.FeedbackStatus
import com.example.data.FeedbackType
import com.example.data.LocalPreferencesStore
import com.example.data.LocalReminderPrefs
import com.example.data.NextDayPollSummary
import com.example.data.NextDayPollWindow
import com.example.data.NextDaySchoolVote
import com.example.data.Notice
import com.example.data.PersonalAcademicMarker
import com.example.data.PersonalMarkerCategory
import com.example.data.SriLankaCalendarDefaults
import com.example.data.StudentTrendSummary
import com.example.data.SyncStatus
import com.example.data.UndoableCheckInAction
import com.example.data.UserProfile
import com.example.data.WeekDaySummary
import com.example.system.AttendanceWidgetStore
import com.example.system.BackgroundBroadcastChecker
import com.example.system.ReminderScheduler
import com.example.system.SyncScheduler
import com.example.system.WidgetSnapshot
import androidx.core.app.NotificationManagerCompat
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val allCalendarEventsForAdmin: List<CalendarEvent> = emptyList(),
    val adminAllStudents: List<UserProfile> = emptyList(),
    val academicCatalog: AcademicCatalog = AcademicCatalog.defaultCatalog(),
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
    val missedSchoolDays: List<String> = emptyList(),
    val milestones: List<AttendanceMilestone> = emptyList(),
    val sundayPrepEnabled: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accentPalette: AppAccentPalette = AppAccentPalette.ROYAL_BLUE,
    val totalAccounts: Int = 0,
    val adminOnlineNowCount: Int? = null,
    val adminOnlineUsers: List<UserProfile> = emptyList(),
    val unreadBroadcasts: List<BroadcastNotification> = emptyList(),
    val resolvedFeedbackAlerts: List<FeedbackReport> = emptyList(),
    val lastSeenAppVersion: String = "",
    val lastFeedbackPromptVersion: String = "",
    val localPrefsLoaded: Boolean = false,
    val instantBackgroundAlertsEnabled: Boolean = true,
    val lastBackgroundCheckMillis: Long = 0L,
    val periodBlocks: List<AttendancePeriodBlock> = emptyList(),
    val communityTrendSummaries: List<StudentTrendSummary> = emptyList(),
    val personalMarkers: List<PersonalAcademicMarker> = emptyList(),
    val afterSchoolUnlockPromptEnabled: Boolean = false,
    val dangerZoneNudgeEnabled: Boolean = true,
    val isInDangerZone: Boolean = false,
    val pollWindow: NextDayPollWindow = NextDayPollWindow(),
    val nextDayPollSummary: NextDayPollSummary = NextDayPollSummary(),
    val autoOfferedSwipeDeckThisLaunch: Boolean = false
) {
    val effectiveSchoolStartDateKey: String
        get() = appSettings.effectiveSchoolStartDate(profile?.alExamYear)

    fun isBeforeSchoolStart(dateKey: String): Boolean =
        dateKey < effectiveSchoolStartDateKey

    fun lockingPeriodFor(dateKey: String): AttendancePeriodBlock? =
        periodBlocks.firstOrNull { it.containsDate(dateKey) }

    fun isDateLocked(dateKey: String): Boolean =
        profile?.isDateInLockedBaseline(dateKey) == true || lockingPeriodFor(dateKey) != null
}

private data class CoreFirestoreBundle(
    val profile: UserProfile? = null,
    val profileLoaded: Boolean = false,
    val records: List<AttendanceRecord> = emptyList(),
    val notices: List<Notice> = emptyList(),
    val terms: List<AcademicTerm> = emptyList(),
    val events: List<CalendarEvent> = emptyList(),
    val streamError: String? = null
)

private data class AdminStreamBundle(
    val students: List<UserProfile> = emptyList(),
    val onlineNowCount: Int? = null,
    val onlineUsers: List<UserProfile> = emptyList()
)

private data class StudentExtraData(
    val academicCatalog: AcademicCatalog = AcademicCatalog.defaultCatalog(),
    val userFeedback: List<FeedbackReport> = emptyList(),
    val periodBlocks: List<AttendancePeriodBlock> = emptyList(),
    val communityTrendSummaries: List<StudentTrendSummary> = emptyList(),
    val personalMarkers: List<PersonalAcademicMarker> = emptyList()
)

private data class CommunityExtrasBundle(
    val settings: AppSettings = AppSettings(),
    val communityStats: CommunityStats = CommunityStats(),
    val broadcasts: List<BroadcastNotification> = emptyList(),
    val adminBundle: AdminStreamBundle = AdminStreamBundle(),
    val academicCatalog: AcademicCatalog = AcademicCatalog.defaultCatalog(),
    val userFeedback: List<FeedbackReport> = emptyList(),
    val periodBlocks: List<AttendancePeriodBlock> = emptyList(),
    val communityTrendSummaries: List<StudentTrendSummary> = emptyList(),
    val personalMarkers: List<PersonalAcademicMarker> = emptyList(),
    val nextDayVotes: List<NextDaySchoolVote> = emptyList()
)

private data class LocalSessionState(
    val prefs: LocalReminderPrefs = LocalReminderPrefs(),
    val prefsLoaded: Boolean = false,
    val sessionDismissedCheckIn: Boolean = false,
    val sessionDismissedBroadcastIds: Set<String> = emptySet(),
    val sessionAckResolvedFeedbackIds: Set<String> = emptySet(),
    val autoOfferedSwipeDeckThisLaunch: Boolean = false,
    val clockTick: Pair<String, Int> = DateUtils.todayKey() to DateUtils.currentHourMinute().first
)

class AttendanceViewModel(
    private val repository: AttendanceRepository,
    private val localPrefs: LocalPreferencesStore,
    private val appContext: Context,
    val currentUserId: String,
    val userEmail: String,
    val googleDisplayName: String?,
    val googlePhotoUrl: String? = null,
    val isVerifiedAdmin: Boolean = false
) : ViewModel() {

    private val _scopeMode = MutableStateFlow(DashboardScopeMode.OVERALL_AL)
    val scopeMode: StateFlow<DashboardScopeMode> = _scopeMode.asStateFlow()

    private val _manualCheckInDate = MutableStateFlow<String?>(null)
    val manualCheckInDate: StateFlow<String?> = _manualCheckInDate.asStateFlow()

    private val _manualCheckInIsPreview = MutableStateFlow(false)
    val manualCheckInIsPreview: StateFlow<Boolean> = _manualCheckInIsPreview.asStateFlow()

    private val _sessionDismissedCheckIn = MutableStateFlow(false)

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _undoableCheckIn = MutableStateFlow<UndoableCheckInAction?>(null)
    val undoableCheckIn: StateFlow<UndoableCheckInAction?> = _undoableCheckIn.asStateFlow()

    private val _celebrationEvent = MutableStateFlow<CelebrationEvent?>(null)
    val celebrationEvent: StateFlow<CelebrationEvent?> = _celebrationEvent.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isCheckingBackgroundAlerts = MutableStateFlow(false)
    val isCheckingBackgroundAlerts: StateFlow<Boolean> = _isCheckingBackgroundAlerts.asStateFlow()

    private var lastCachedAudienceSignature: String = ""
    private var lastSyncedTrendSignature: String = ""
    private var lastReconciledCounterSignature: String = ""
    private var lastDangerNudgeTriggeredDate: String = ""
    private var lastSyncedPollEnabled: Boolean? = null

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

    private val _sessionDismissedBroadcastIds = MutableStateFlow<Set<String>>(emptySet())
    private val _sessionAckResolvedFeedbackIds = MutableStateFlow<Set<String>>(emptySet())
    private val _autoOfferedSwipeDeckThisLaunch = MutableStateFlow(false)

    private val adminStreamFlow: Flow<AdminStreamBundle> = if (isVerifiedAdmin) {
        combine(
            repository.observeAllStudentsAdmin().safeFirestoreFlow(emptyList(), "adminStudents"),
            kotlinx.coroutines.flow.flow {
                while (true) {
                    emit(System.currentTimeMillis())
                    kotlinx.coroutines.delay(30_000L)
                }
            }
        ) { studentsPair, nowMs ->
            val onlineList = studentsPair.first
                .filter { it.isCurrentlyOnline(nowMs) }
                .sortedByDescending { it.lastSeenAt?.toDate()?.time ?: 0L }
            AdminStreamBundle(
                students = studentsPair.first,
                onlineNowCount = onlineList.size,
                onlineUsers = onlineList
            )
        }
    } else {
        kotlinx.coroutines.flow.flowOf(AdminStreamBundle())
    }

    private val studentExtraDataFlow: Flow<StudentExtraData> = combine(
        repository.observeAcademicCatalog().safeFirestoreFlow(AcademicCatalog.defaultCatalog(), "catalog"),
        repository.observeUserFeedbackReports(currentUserId).safeFirestoreFlow(emptyList(), "userFeedback"),
        repository.observePeriodBlocks(currentUserId).safeFirestoreFlow(emptyList(), "periodBlocks"),
        repository.observeAllStudentTrendSummaries().safeFirestoreFlow(emptyList(), "communityTrends"),
        repository.observePersonalMarkers(currentUserId).safeFirestoreFlow(emptyList(), "personalMarkers")
    ) { catalogPair, feedbackPair, periodsPair, trendsPair, markersPair ->
        StudentExtraData(
            academicCatalog = catalogPair.first,
            userFeedback = feedbackPair.first,
            periodBlocks = periodsPair.first,
            communityTrendSummaries = trendsPair.first,
            personalMarkers = markersPair.first
        )
    }

    private val pollClockTickFlow: Flow<Pair<String, Int>> = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(DateUtils.todayKey() to DateUtils.currentHourMinute().first)
            kotlinx.coroutines.delay(60_000L)
        }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val nextDayVotesFlow: Flow<List<NextDaySchoolVote>> = combine(
        repository.observeAppSettings().safeFirestoreFlow(AppSettings(), "pollSettings"),
        pollClockTickFlow
    ) { settingsPair, (todayKey, hourNow) ->
        if (!settingsPair.first.nextDayPollEnabled || (hourNow in 5..19)) {
            null
        } else {
            if (hourNow < 5) todayKey else DateUtils.addDays(todayKey, 1)
        }
    }.distinctUntilChanged().flatMapLatest { targetDate ->
        if (targetDate == null) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            repository.observeNextDayVotesForDate(targetDate)
                .safeFirestoreFlow(emptyList(), "nextDayVotes")
                .map { it.first }
        }
    }

    private val extrasBundleFlow: Flow<CommunityExtrasBundle> = combine(
        repository.observeAppSettings().safeFirestoreFlow(AppSettings(), "settings"),
        repository.observeCommunityStats().safeFirestoreFlow(CommunityStats(), "communityStats"),
        repository.observeBroadcastNotifications().safeFirestoreFlow(emptyList(), "broadcasts"),
        adminStreamFlow,
        combine(studentExtraDataFlow, nextDayVotesFlow) { studentExtras, votes ->
            studentExtras to votes
        }
    ) { settingsPair, statsPair, broadcastsPair, adminBundle, extrasWithVotes ->
        val (studentExtras, votes) = extrasWithVotes
        CommunityExtrasBundle(
            settings = settingsPair.first,
            communityStats = statsPair.first,
            broadcasts = broadcastsPair.first,
            adminBundle = adminBundle,
            academicCatalog = studentExtras.academicCatalog,
            userFeedback = studentExtras.userFeedback,
            periodBlocks = studentExtras.periodBlocks,
            communityTrendSummaries = studentExtras.communityTrendSummaries,
            personalMarkers = studentExtras.personalMarkers,
            nextDayVotes = votes
        )
    }

    private val localSessionFlow: Flow<LocalSessionState> = combine(
        localPrefs.prefsFlow.map { it to true },
        _sessionDismissedCheckIn,
        _sessionDismissedBroadcastIds,
        _sessionAckResolvedFeedbackIds,
        combine(_autoOfferedSwipeDeckThisLaunch, pollClockTickFlow) { autoOffered, tick -> autoOffered to tick }
    ) { prefsPair, dismissedCheckIn, dismissedBroadcasts, ackFeedback, autoAndTick ->
        LocalSessionState(
            prefs = prefsPair.first,
            prefsLoaded = prefsPair.second,
            sessionDismissedCheckIn = dismissedCheckIn,
            sessionDismissedBroadcastIds = dismissedBroadcasts,
            sessionAckResolvedFeedbackIds = ackFeedback,
            autoOfferedSwipeDeckThisLaunch = autoAndTick.first,
            clockTick = autoAndTick.second
        )
    }

    private val networkFlow = SyncScheduler.observeNetworkConnectivity(appContext)

    val uiState: StateFlow<StudentAppState> = combine(
        coreBundleFlow,
        extrasBundleFlow,
        networkFlow,
        repository.hasPendingWrites,
        localSessionFlow
    ) { bundle, extras, isOnline, hasPending, localState ->
        val prefs = localState.prefs
        val sessionDismissed = localState.sessionDismissedCheckIn
        val sessionDismissedBroadcasts = localState.sessionDismissedBroadcastIds
        val sessionAckFeedback = localState.sessionAckResolvedFeedbackIds

        val settings = extras.settings
        val profile = bundle.profile
        val effectiveTerms = bundle.terms.ifEmpty { SriLankaCalendarDefaults.defaultTerms }
        val rawEvents = bundle.events.ifEmpty { SriLankaCalendarDefaults.defaultEvents }
        val effectiveEvents = AttendanceCalculator.filterCalendarEventsForStudent(rawEvents, profile, currentUserId)
        val todayKey = DateUtils.todayKey()
        val activeTerm = AttendanceCalculator.resolveActiveTerm(effectiveTerms, todayKey)
        val threshold = activeTerm.minEligibilityPercent.takeIf { it in 50..100 }
            ?: settings.defaultEligibilityPercent

        val lockedRange = profile?.lockedBaselineRange
        val periodBlocks = extras.periodBlocks
        val schoolStartKey = settings.effectiveSchoolStartDate(profile?.alExamYear)
        val monthPrefix = DateUtils.currentMonthPrefix(todayKey)
        val monthLabel = DateUtils.currentMonthYearLabel(todayKey)
        val baselineWithinMonth = lockedRange != null &&
            lockedRange.first.startsWith(monthPrefix) &&
            lockedRange.second.startsWith(monthPrefix)
        val monthStats = AttendanceCalculator.calculateStats(
            records = bundle.records,
            thresholdPercent = threshold,
            countExcusedAsPresent = true,
            includePriorBalance = baselineWithinMonth,
            priorPresent = if (baselineWithinMonth) profile.priorPresentCount else 0,
            priorAbsent = if (baselineWithinMonth) profile.priorAbsentCount else 0,
            monthPrefixFilter = monthPrefix,
            lockedRange = lockedRange,
            periodBlocks = periodBlocks,
            calendarEvents = effectiveEvents,
            schoolStartDate = schoolStartKey
        )
        val termStats = AttendanceCalculator.calculateStats(
            records = bundle.records,
            thresholdPercent = threshold,
            countExcusedAsPresent = true,
            includePriorBalance = true,
            priorPresent = profile?.priorPresentCount ?: 0,
            priorAbsent = profile?.priorAbsentCount ?: 0,
            termFilter = activeTerm,
            lockedRange = lockedRange,
            periodBlocks = periodBlocks,
            calendarEvents = effectiveEvents,
            schoolStartDate = schoolStartKey
        )
        val overallStats = AttendanceCalculator.calculateStats(
            records = bundle.records,
            thresholdPercent = threshold,
            countExcusedAsPresent = true,
            includePriorBalance = true,
            priorPresent = profile?.priorPresentCount ?: 0,
            priorAbsent = profile?.priorAbsentCount ?: 0,
            termFilter = null,
            lockedRange = lockedRange,
            periodBlocks = periodBlocks,
            calendarEvents = effectiveEvents,
            schoolStartDate = schoolStartKey
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
            todayKey = todayKey,
            lockedRange = lockedRange,
            periodBlocks = periodBlocks
        )
        val nextHoliday = AttendanceCalculator.nextUpcomingPoyaOrHoliday(effectiveEvents, todayKey)
        val examYear = profile?.alExamYear ?: settings.currentAcademicYear
        val daysToExam = AttendanceCalculator.daysUntilAlExam(examYear, settings, todayKey)
        val visibleNotices = AttendanceCalculator.filterNoticesForStudent(
            notices = bundle.notices,
            profile = profile,
            nowMillis = System.currentTimeMillis()
        )
        val pinnedNotices = visibleNotices.filter { it.pinned }
        val todayRecord = bundle.records.firstOrNull { it.dateKey == todayKey }
        val todayIsWeekend = DateUtils.isWeekend(todayKey)
        val todayNonSchoolEvent = effectiveEvents.firstOrNull { it.dateKey == todayKey && it.nonSchoolDay }
        val todayIsLocked = profile?.isDateInLockedBaseline(todayKey) == true ||
            periodBlocks.any { it.containsDate(todayKey) }
        val todayIsSchoolDay = !todayIsWeekend && todayNonSchoolEvent == null && !todayIsLocked && todayKey >= schoolStartKey

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

        val effectiveLookbackStart = if (schoolStartKey > activeTerm.startDate) schoolStartKey else activeTerm.startDate
        val missedSchoolDays = if (profile?.setupCompleted == true && todayKey >= schoolStartKey) {
            AttendanceCalculator.findUnloggedRecentSchoolDays(
                records = bundle.records,
                calendarEvents = effectiveEvents,
                activeTermStartDate = effectiveLookbackStart,
                todayKey = todayKey,
                lookbackCalendarDays = 10,
                maxItems = 5,
                lockedRange = lockedRange,
                periodBlocks = periodBlocks
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
                    effectiveAttended = termStats.effectiveAttended,
                    totalCountedDays = termStats.totalCountedDays,
                    safeSkipsRemaining = termStats.safeSkipsRemaining,
                    daysNeededToRecover = termStats.daysNeededToRecover,
                    streak = streak,
                    termName = activeTerm.name,
                    todayDateKey = todayKey,
                    todayStatusLabel = todayRecord?.statusEnum?.label
                        ?: if (todayIsLocked) "Locked" else null,
                    todayNoSchoolTitle = todayNonSchoolEvent?.title,
                    signedIn = true,
                    weekDays = AttendanceWidgetStore.toWidgetWeekDays(weekly),
                    daysUntilAlExam = daysToExam,
                    alExamYear = examYear,
                    nextHolidayTitle = nextHoliday?.title,
                    nextHolidayDateShort = nextHoliday?.dateKey?.let { DateUtils.formatReadableShort(it) }
                )
            )
        }

        // Compute & deduplicate-sync anonymous numeric trend summary for school-wide & cohort analytics
        val localTrendSummary = if (bundle.profileLoaded && profile?.setupCompleted == true) {
            AttendanceCalculator.buildStudentTrendSummary(
                userId = currentUserId,
                records = bundle.records,
                periodBlocks = periodBlocks,
                priorPresent = profile.priorPresentCount,
                priorAbsent = profile.priorAbsentCount,
                grade = profile.grade,
                alStream = profile.alStream,
                className = profile.className,
                lockedRange = lockedRange,
                calendarEvents = effectiveEvents,
                schoolStartDate = schoolStartKey
            )
        } else {
            null
        }
        if (localTrendSummary != null && localTrendSummary.totalCounted > 0) {
            val trendSig = "${localTrendSummary.totalAttended}|${localTrendSummary.totalAbsent}|${localTrendSummary.grade}|${localTrendSummary.alStream}|${localTrendSummary.className}|${localTrendSummary.monthlyTotalsJson}|${localTrendSummary.monthlyWeekdayJson}|${localTrendSummary.allTimeWeekdayAttended.joinToString(",")}|${localTrendSummary.allTimeWeekdayAbsent.joinToString(",")}"
            if (trendSig != lastSyncedTrendSignature) {
                lastSyncedTrendSignature = trendSig
                viewModelScope.launch {
                    repository.upsertStudentTrendSummary(localTrendSummary)
                }
            }
        }
        // Reconcile UserProfile presentCount/absentCount in Firestore if locked period blocks were not yet included
        if (bundle.profileLoaded && profile?.setupCompleted == true && periodBlocks.isNotEmpty()) {
            val storedEffectivePresent = profile.presentCount + profile.priorPresentCount + profile.medicalCount + profile.schoolEventCount
            val storedEffectiveAbsent = profile.absentCount + profile.priorAbsentCount
            val counterSig = "${overallStats.effectiveAttended}|${overallStats.absentDays}"
            if ((storedEffectivePresent != overallStats.effectiveAttended || storedEffectiveAbsent != overallStats.absentDays) &&
                counterSig != lastReconciledCounterSignature
            ) {
                lastReconciledCounterSignature = counterSig
                val recordsSnapshot = bundle.records
                viewModelScope.launch {
                    repository.reconcileUserCountersWithPeriodBlocks(
                        userId = currentUserId,
                        records = recordsSnapshot,
                        periodBlocks = periodBlocks,
                        lockedBaselineRange = lockedRange
                    )
                }
            }
        }
        val mergedCommunitySummaries = if (localTrendSummary != null && localTrendSummary.totalCounted > 0) {
            extras.communityTrendSummaries.filterNot { it.userId == currentUserId || it.id == currentUserId } + localTrendSummary
        } else {
            extras.communityTrendSummaries
        }
        val trendSummaryByUserId = mergedCommunitySummaries.associateBy { it.userId.ifBlank { it.id } }

        val inDangerZone = AttendanceCalculator.isDangerZone(termStats)
        if (localState.prefsLoaded &&
            profile?.setupCompleted == true &&
            prefs.dangerZoneNudgeEnabled &&
            inDangerZone &&
            prefs.lastDangerNudgeDate != todayKey &&
            lastDangerNudgeTriggeredDate != todayKey
        ) {
            lastDangerNudgeTriggeredDate = todayKey
            viewModelScope.launch {
                val posted = ReminderScheduler.showDangerZoneNudgeNotification(
                    context = appContext,
                    percentage = termStats.percentage,
                    thresholdPercent = termStats.thresholdPercent,
                    safeSkipsRemaining = termStats.safeSkipsRemaining
                )
                if (posted) {
                    localPrefs.markDangerNudgeFiredForDate(todayKey)
                }
            }
        }

        val milestones = AttendanceCalculator.calculateMilestones(
            records = bundle.records,
            currentStreak = streak,
            termStats = termStats,
            overallStats = overallStats,
            lockedRange = lockedRange,
            periodBlocks = periodBlocks,
            calendarEvents = effectiveEvents,
            schoolStartDate = schoolStartKey
        )

        val nowMs = System.currentTimeMillis()
        val recentMatchingBroadcasts = if (profile?.setupCompleted == true) {
            extras.broadcasts.filter { bc ->
                bc.id.isNotBlank() &&
                    bc.matchesStudent(profile, currentUserId) &&
                    bc.id !in prefs.seenBroadcastIds &&
                    bc.id !in sessionDismissedBroadcasts &&
                    run {
                        val createdMs = bc.createdAt?.toDate()?.time
                        createdMs == null || (nowMs - createdMs) <= 7L * 86_400_000L
                    }
            }
        } else {
            emptyList()
        }

        // Keep local DataStore audience cache synced so cold-process background checks never drop broadcasts
        if (profile != null && profile.setupCompleted) {
            val sig = "$currentUserId|${profile.grade.trim()}|${profile.alStream.trim()}|${profile.className.trim()}"
            if (sig != lastCachedAudienceSignature) {
                lastCachedAudienceSignature = sig
                viewModelScope.launch {
                    localPrefs.saveCachedStudentAudience(
                        userId = currentUserId,
                        grade = profile.grade,
                        stream = profile.alStream,
                        className = profile.className,
                        setupCompleted = true
                    )
                }
            }
        }

        // Fire real Android system notification STRICTLY ONCE per device via shared Mutex-protected engine
        if (localState.prefsLoaded && profile?.setupCompleted == true && recentMatchingBroadcasts.isNotEmpty()) {
            val hasUnnotified = recentMatchingBroadcasts.any { bc ->
                bc.id !in prefs.notifiedBroadcastIds && bc.id !in prefs.seenBroadcastIds
            }
            if (hasUnnotified && ReminderScheduler.hasNotificationPermission(appContext)) {
                val snapshotList = recentMatchingBroadcasts.toList()
                viewModelScope.launch {
                    BackgroundBroadcastChecker.deliverMatchingBroadcasts(
                        context = appContext,
                        prefsStore = localPrefs,
                        userId = currentUserId,
                        profile = profile,
                        broadcasts = snapshotList,
                        maxAgeMillis = 48L * 3_600_000L
                    )
                }
            }
        }

        // In-app-only notification whenever the admin marks a student's bug/suggestion as Resolved
        val resolvedFeedbackAlerts = if (profile?.setupCompleted == true) {
            extras.userFeedback.filter { fb ->
                fb.id.isNotBlank() &&
                    fb.statusEnum == FeedbackStatus.RESOLVED &&
                    fb.id !in prefs.acknowledgedResolvedFeedbackIds &&
                    fb.id !in sessionAckFeedback
            }
        } else {
            emptyList()
        }

        val displayedAccounts = maxOf(
            extras.communityStats.totalAccounts,
            if (profile?.setupCompleted == true) 1 else 0
        )

        val pollWindow = AttendanceCalculator.resolveNextDayPollWindow(
            calendarEvents = effectiveEvents,
            todayKey = todayKey,
            hourOfDay = nowHour
        )
        val pollSummary = AttendanceCalculator.calculateNextDayPollSummary(
            votes = extras.nextDayVotes,
            currentUserId = currentUserId,
            myClassName = profile?.className,
            targetDateKey = pollWindow.targetDateKey
        )
        if (lastSyncedPollEnabled != settings.nextDayPollEnabled) {
            lastSyncedPollEnabled = settings.nextDayPollEnabled
            viewModelScope.launch {
                localPrefs.saveCachedNextDayPollEnabled(settings.nextDayPollEnabled)
                ReminderScheduler.scheduleNextDayPollAlarm(appContext, settings.nextDayPollEnabled)
            }
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
            allCalendarEventsForAdmin = if (isVerifiedAdmin) rawEvents else emptyList(),
            adminAllStudents = if (isVerifiedAdmin) {
                extras.adminBundle.students.map { st ->
                    st.withReconciledTrendSummary(trendSummaryByUserId[st.userId])
                }
            } else emptyList(),
            academicCatalog = extras.academicCatalog,
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
            missedSchoolDays = missedSchoolDays,
            milestones = milestones,
            sundayPrepEnabled = prefs.sundayPrepEnabled,
            themeMode = prefs.themeMode,
            accentPalette = prefs.accentPalette,
            totalAccounts = displayedAccounts,
            adminOnlineNowCount = extras.adminBundle.onlineNowCount,
            adminOnlineUsers = if (isVerifiedAdmin) {
                extras.adminBundle.onlineUsers.map { st ->
                    st.withReconciledTrendSummary(trendSummaryByUserId[st.userId])
                }
            } else emptyList(),
            unreadBroadcasts = recentMatchingBroadcasts,
            resolvedFeedbackAlerts = resolvedFeedbackAlerts,
            lastSeenAppVersion = prefs.lastSeenAppVersion,
            lastFeedbackPromptVersion = prefs.lastFeedbackPromptVersion,
            localPrefsLoaded = localState.prefsLoaded,
            instantBackgroundAlertsEnabled = prefs.instantBackgroundAlertsEnabled,
            lastBackgroundCheckMillis = prefs.lastBackgroundCheckMillis,
            periodBlocks = periodBlocks,
            communityTrendSummaries = mergedCommunitySummaries,
            personalMarkers = extras.personalMarkers,
            afterSchoolUnlockPromptEnabled = prefs.afterSchoolUnlockPromptEnabled,
            dangerZoneNudgeEnabled = prefs.dangerZoneNudgeEnabled,
            isInDangerZone = inDangerZone,
            pollWindow = pollWindow,
            nextDayPollSummary = pollSummary,
            autoOfferedSwipeDeckThisLaunch = localState.autoOfferedSwipeDeckThisLaunch
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = StudentAppState(isLoading = true, googlePhotoUrl = googlePhotoUrl)
    )

    fun markSwipeDeckAutoOffered() {
        _autoOfferedSwipeDeckThisLaunch.value = true
    }

    fun setPresence(online: Boolean) {
        viewModelScope.launch {
            repository.updatePresence(currentUserId, online)
        }
    }

    fun dismissBroadcastAlert(broadcastId: String) {
        if (broadcastId.isBlank()) return
        _sessionDismissedBroadcastIds.value = _sessionDismissedBroadcastIds.value + broadcastId
        viewModelScope.launch {
            localPrefs.markBroadcastsSeen(listOf(broadcastId))
            localPrefs.markBroadcastsNotified(listOf(broadcastId))
        }
    }

    fun dismissResolvedFeedbackAlert(feedbackId: String) {
        if (feedbackId.isBlank()) return
        _sessionAckResolvedFeedbackIds.value = _sessionAckResolvedFeedbackIds.value + feedbackId
        viewModelScope.launch {
            localPrefs.acknowledgeResolvedFeedback(listOf(feedbackId))
        }
    }

    fun markAppVersionSeen(versionName: String) {
        viewModelScope.launch {
            localPrefs.markAppVersionSeen(versionName)
        }
    }

    fun markFeedbackPromptShown(versionName: String) {
        viewModelScope.launch {
            localPrefs.markFeedbackPromptShownForVersion(versionName)
        }
    }

    fun setScopeMode(mode: DashboardScopeMode) {
        _scopeMode.value = mode
    }

    fun openCheckInPopup(
        dateKey: String = DateUtils.todayKey(),
        forcePreview: Boolean = false
    ) {
        if (forcePreview) {
            _manualCheckInIsPreview.value = true
            _manualCheckInDate.value = dateKey
            return
        }
        _manualCheckInIsPreview.value = false
        if (DateUtils.isWeekend(dateKey)) return
        if (dateKey > DateUtils.todayKey()) {
            _snackbarMessage.value = "You can't mark attendance for future dates"
            return
        }
        if (uiState.value.isBeforeSchoolStart(dateKey)) {
            _snackbarMessage.value = "Attendance starts from ${DateUtils.formatReadableShort(uiState.value.effectiveSchoolStartDateKey)}"
            return
        }
        if (uiState.value.isDateLocked(dateKey)) {
            _snackbarMessage.value = "This day is locked inside a saved attendance period"
            return
        }
        _manualCheckInDate.value = dateKey
    }

    fun dismissCheckInPopup(markDismissedToday: Boolean) {
        _manualCheckInDate.value = null
        _manualCheckInIsPreview.value = false
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
        baselineStartDate: String? = null,
        baselineEndDate: String? = null,
        baselinePercent: Double? = null,
        updateBaselineFields: Boolean = false,
        missingSubjectAlerts: List<Pair<String, String>> = emptyList(),
        missingClassAlert: String? = null,
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
                photoUrl = googlePhotoUrl,
                baselineStartDate = baselineStartDate,
                baselineEndDate = baselineEndDate,
                baselinePercent = baselinePercent,
                updateBaselineFields = updateBaselineFields
            )
            val currentRecords = uiState.value.attendanceRecords
            val currentPeriodBlocks = uiState.value.periodBlocks
            if (result.isSuccess &&
                updateBaselineFields &&
                isExisting &&
                baselineStartDate != null &&
                baselineEndDate != null &&
                (priorPresentCount + priorAbsentCount > 0) &&
                (currentRecords.any { it.dateKey <= baselineEndDate } ||
                    currentPeriodBlocks.any { it.startDate <= baselineEndDate })
            ) {
                val effectivePct = baselinePercent
                    ?: ((priorPresentCount.toDouble() / (priorPresentCount + priorAbsentCount).toDouble()) * 100.0)
                repository.saveAttendancePercentageBaseline(
                    userId = currentUserId,
                    startDateKey = baselineStartDate,
                    endDateKey = baselineEndDate,
                    percentage = effectivePct,
                    presentDays = priorPresentCount,
                    absentDays = priorAbsentCount,
                    existingRecords = currentRecords,
                    existingPeriodBlocks = currentPeriodBlocks
                )
            }
            _isSaving.value = false
            result.onSuccess {
                localPrefs.saveReminder(reminderEnabled, reminderHour, reminderMinute)
                localPrefs.saveCachedStudentAudience(
                    userId = currentUserId,
                    grade = grade,
                    stream = alStream,
                    className = className,
                    setupCompleted = true
                )
                ReminderScheduler.scheduleDailyReminder(appContext, reminderEnabled, reminderHour, reminderMinute)
                SyncScheduler.ensureAllBackgroundChecksScheduled(
                    context = appContext,
                    instantListenerEnabled = uiState.value.instantBackgroundAlertsEnabled,
                    allowStartForegroundService = true
                )
                if (!isExisting) {
                    // Record current version so first-time users see Feedback Welcome (not What's New)
                    localPrefs.markAppVersionSeen(BuildConfig.VERSION_NAME)
                }

                // Notify admin about any manually typed subjects/classes missing from the catalog
                val studentFullName = "$firstName $lastName".trim().ifEmpty { googleDisplayName ?: "Student" }
                missingSubjectAlerts.forEach { (bucketName, customSubject) ->
                    val cleanSub = customSubject.trim()
                    if (cleanSub.isNotEmpty()) {
                        repository.submitFeedback(
                            userId = currentUserId,
                            userName = studentFullName,
                            userEmail = userEmail,
                            className = className,
                            type = FeedbackType.CATALOG_REQUEST,
                            title = "Missing Subject: $cleanSub",
                            message = "Subject '$cleanSub' was not listed under $alStream → $bucketName. Student ($grade • $className) added it manually.",
                            appVersion = BuildConfig.VERSION_NAME
                        )
                    }
                }
                missingClassAlert?.trim()?.takeIf { it.isNotEmpty() }?.let { customClass ->
                    repository.submitFeedback(
                        userId = currentUserId,
                        userName = studentFullName,
                        userEmail = userEmail,
                        className = customClass,
                        type = FeedbackType.CATALOG_REQUEST,
                        title = "Missing Class: $customClass",
                        message = "Class '$customClass' was not listed under $grade → $alStream. Student entered it manually.",
                        appVersion = BuildConfig.VERSION_NAME
                    )
                }

                _snackbarMessage.value = if (isExisting) "Profile updated" else "Welcome to 80endance!"
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not save profile"
            }
        }
    }

    fun saveAttendancePercentageBaseline(
        startDateKey: String,
        endDateKey: String,
        percentage: Double,
        presentDays: Int,
        absentDays: Int,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val current = uiState.value
            val result = repository.saveAttendancePercentageBaseline(
                userId = currentUserId,
                startDateKey = startDateKey,
                endDateKey = endDateKey,
                percentage = percentage,
                presentDays = presentDays,
                absentDays = absentDays,
                existingRecords = current.attendanceRecords,
                existingPeriodBlocks = current.periodBlocks
            )
            _isSaving.value = false
            result.onSuccess { replacedCount ->
                val rangeLabel = "${DateUtils.formatReadableShort(startDateKey)} – ${DateUtils.formatReadableShort(endDateKey)}"
                val replacedHint = if (replacedCount > 0) " ($replacedCount prior entries consolidated)" else ""
                _snackbarMessage.value = "Imported ${String.format("%.1f", percentage)}% for $rangeLabel • period locked$replacedHint"
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not save attendance percentage"
            }
        }
    }

    fun clearAttendancePercentageBaseline(resetPriorCounts: Boolean = true) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.clearAttendancePercentageBaseline(
                userId = currentUserId,
                resetPriorCounts = resetPriorCounts
            )
            _isSaving.value = false
            result.onSuccess {
                _snackbarMessage.value = "Imported attendance period cleared and dates unlocked"
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not clear imported period"
            }
        }
    }

    fun savePeriodBlock(
        startDateKey: String,
        endDateKey: String,
        totalSchoolDays: Int,
        presentDays: Int,
        absentDays: Int,
        percentage: Double,
        entryMode: AttendancePeriodEntryMode,
        label: String? = null,
        existingBlockId: String? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val current = uiState.value
            val result = repository.savePeriodBlock(
                userId = currentUserId,
                startDateKey = startDateKey,
                endDateKey = endDateKey,
                totalSchoolDays = totalSchoolDays,
                presentDays = presentDays,
                absentDays = absentDays,
                percentage = percentage,
                entryMode = entryMode,
                label = label,
                existingRecords = current.attendanceRecords,
                existingBlockId = existingBlockId,
                existingPeriodBlocks = current.periodBlocks,
                lockedBaselineRange = current.profile?.lockedBaselineRange
            )
            _isSaving.value = false
            result.onSuccess { consolidatedCount ->
                val rangeLabel = "${DateUtils.formatReadableShort(startDateKey)} – ${DateUtils.formatReadableShort(endDateKey)}"
                val suffix = if (consolidatedCount > 0) " ($consolidatedCount individual entries consolidated)" else ""
                _snackbarMessage.value = "Locked $rangeLabel • ${presentDays}P / ${absentDays}A (${String.format(java.util.Locale.US, "%.1f%%", percentage)})$suffix"
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not lock attendance period"
            }
        }
    }

    fun deletePeriodBlock(periodId: String) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.deletePeriodBlock(currentUserId, periodId)
            _isSaving.value = false
            result.onSuccess {
                _snackbarMessage.value = "Time period unlocked — dates are editable in Calendar again"
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not unlock period"
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
        if (_manualCheckInIsPreview.value && (DateUtils.isWeekend(dateKey) || dateKey > DateUtils.todayKey())) {
            _manualCheckInDate.value = null
            _manualCheckInIsPreview.value = false
            _snackbarMessage.value = "Check-in popup preview works! (Weekend / non-school day wasn't logged)"
            return
        }
        if (DateUtils.isWeekend(dateKey)) {
            _snackbarMessage.value = "Weekends are automatically marked as off days"
            return
        }
        if (dateKey > DateUtils.todayKey()) {
            _snackbarMessage.value = "You can't mark attendance for future dates"
            return
        }
        if (uiState.value.isBeforeSchoolStart(dateKey)) {
            _snackbarMessage.value = "Attendance starts from ${DateUtils.formatReadableShort(uiState.value.effectiveSchoolStartDateKey)}"
            return
        }
        if (uiState.value.isDateLocked(dateKey)) {
            _snackbarMessage.value = "Cannot edit days inside a locked attendance period"
            return
        }
        val nonSchoolEv = uiState.value.calendarEvents.firstOrNull { it.dateKey == dateKey && it.nonSchoolDay }
        if (nonSchoolEv != null && status != AttendanceStatus.HOLIDAY) {
            _snackbarMessage.value = "${nonSchoolEv.title} is a non-school day"
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val current = uiState.value
            val previousRecord = current.attendanceRecords.firstOrNull { it.dateKey == dateKey }
            val wasEligibleBefore = current.currentTermStats.eligible
            val hadCountedDaysBefore = current.currentTermStats.totalCountedDays > 0
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
                _manualCheckInIsPreview.value = false
                if (dateKey == DateUtils.todayKey()) {
                    _sessionDismissedCheckIn.value = true
                }
                val offlineHint = if (current.syncStatus == SyncStatus.OFFLINE_CACHED) " (saved offline)" else ""
                val msg = "Marked ${status.label} for ${DateUtils.formatReadableShort(dateKey)}$offlineHint"
                _undoableCheckIn.value = UndoableCheckInAction(
                    message = msg,
                    dateKey = dateKey,
                    previousRecord = previousRecord
                )
                evaluateCelebrationsAfterLog(
                    previousState = current,
                    loggedDateKey = dateKey,
                    loggedStatus = status,
                    termId = termId,
                    wasEligibleBefore = wasEligibleBefore,
                    hadCountedDaysBefore = hadCountedDaysBefore
                )
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Failed to save attendance"
            }
        }
    }

    private suspend fun evaluateCelebrationsAfterLog(
        previousState: StudentAppState,
        loggedDateKey: String,
        loggedStatus: AttendanceStatus,
        termId: String,
        wasEligibleBefore: Boolean,
        hadCountedDaysBefore: Boolean
    ) {
        if (loggedStatus == AttendanceStatus.ABSENT || loggedStatus == AttendanceStatus.HOLIDAY) {
            // Track if student dropped below 80% so future climb-back triggers Recovery celebration
            val simulated = previousState.attendanceRecords.filterNot { it.dateKey == loggedDateKey } +
                AttendanceRecord(userId = currentUserId, dateKey = loggedDateKey, termId = termId, status = loggedStatus.raw)
            val afterStats = AttendanceCalculator.calculateStats(
                records = simulated,
                thresholdPercent = previousState.activeTerm.minEligibilityPercent,
                countExcusedAsPresent = true,
                includePriorBalance = true,
                priorPresent = previousState.profile?.priorPresentCount ?: 0,
                priorAbsent = previousState.profile?.priorAbsentCount ?: 0,
                termFilter = previousState.activeTerm,
                lockedRange = previousState.profile?.lockedBaselineRange,
                periodBlocks = previousState.periodBlocks,
                calendarEvents = previousState.calendarEvents,
                schoolStartDate = previousState.effectiveSchoolStartDateKey
            )
            if (!afterStats.eligible) {
                localPrefs.saveWasBelowThreshold(true)
            }
            return
        }

        val prefs = localPrefs.getSnapshot()
        val todayKey = DateUtils.todayKey()
        val simulatedRecords = previousState.attendanceRecords.filterNot { it.dateKey == loggedDateKey } +
            AttendanceRecord(userId = currentUserId, dateKey = loggedDateKey, termId = termId, status = loggedStatus.raw)
        val afterStats = AttendanceCalculator.calculateStats(
            records = simulatedRecords,
            thresholdPercent = previousState.activeTerm.minEligibilityPercent,
            countExcusedAsPresent = true,
            includePriorBalance = true,
            priorPresent = previousState.profile?.priorPresentCount ?: 0,
            priorAbsent = previousState.profile?.priorAbsentCount ?: 0,
            termFilter = previousState.activeTerm,
            lockedRange = previousState.profile?.lockedBaselineRange,
            periodBlocks = previousState.periodBlocks,
            calendarEvents = previousState.calendarEvents,
            schoolStartDate = previousState.effectiveSchoolStartDateKey
        )
        val afterStreak = AttendanceCalculator.calculateCurrentStreak(
            records = simulatedRecords,
            calendarEvents = previousState.calendarEvents,
            countExcusedAsPresent = true,
            todayKey = todayKey
        )
        val afterWeek = AttendanceCalculator.calculateWeeklySummary(
            records = simulatedRecords,
            calendarEvents = previousState.calendarEvents,
            todayKey = todayKey,
            lockedRange = previousState.profile?.lockedBaselineRange,
            periodBlocks = previousState.periodBlocks
        )

        // 1. Recovery celebration: climbed back above 80%!
        if ((prefs.wasBelowThreshold || (hadCountedDaysBefore && !wasEligibleBefore)) && afterStats.eligible) {
            localPrefs.saveWasBelowThreshold(false)
            _celebrationEvent.value = CelebrationEvent(
                type = CelebrationType.ELIGIBILITY_RECOVERY,
                title = "Back Above ${afterStats.thresholdPercent}%!",
                subtitle = String.format(java.util.Locale.US, "You climbed back to %.1f%% — A/L eligibility restored!", afterStats.percentage),
                badgeLabel = "COMEBACK"
            )
            return
        }

        // 2. Streak milestones: 10, 20, 50 school days
        val milestoneTarget = listOf(50, 20, 10).firstOrNull { afterStreak >= it && prefs.lastCelebratedStreak < it }
        if (milestoneTarget != null) {
            localPrefs.markCelebratedStreak(milestoneTarget)
            _celebrationEvent.value = CelebrationEvent(
                type = CelebrationType.STREAK_MILESTONE,
                title = "${milestoneTarget}-Day Streak Unlocked!",
                subtitle = "Incredible consistency — $afterStreak straight school days logged.",
                badgeLabel = "${milestoneTarget}D STREAK"
            )
            return
        }

        // 3. Perfect 5/5 Week (Mon-Fri)
        val weekKey = DateUtils.mondayOfWeek(todayKey)
        if (AttendanceCalculator.isPerfectFiveOfFiveWeek(afterWeek) && prefs.lastCelebratedWeekKey != weekKey) {
            localPrefs.markCelebratedWeek(weekKey)
            _celebrationEvent.value = CelebrationEvent(
                type = CelebrationType.PERFECT_WEEK,
                title = "Perfect 5/5 Week!",
                subtitle = "Full Mon–Fri attendance locked in. Keep that momentum rolling!",
                badgeLabel = "5 / 5 WEEK"
            )
        }
    }

    fun clearUndoableCheckIn() {
        _undoableCheckIn.value = null
    }

    fun consumePendingExternalUndo() {
        val pending = AttendanceWidgetStore.consumePendingExternalUndo(appContext) ?: return
        val prevRecord = pending.previousStatusRaw?.takeIf { it.isNotBlank() }?.let { raw ->
            AttendanceRecord(
                userId = currentUserId,
                dateKey = pending.dateKey,
                termId = AttendanceCalculator.resolveTermIdForDate(pending.dateKey, uiState.value.terms),
                status = raw,
                note = pending.previousNote
            )
        }
        val sourceLabel = when (pending.source) {
            "widget" -> "widget"
            "quick_tile" -> "Quick Settings tile"
            "notification" -> "notification"
            else -> "quick action"
        }
        _undoableCheckIn.value = UndoableCheckInAction(
            message = "Marked ${pending.statusLabel} via $sourceLabel (${DateUtils.formatReadableShort(pending.dateKey)})",
            dateKey = pending.dateKey,
            previousRecord = prevRecord
        )
    }

    fun dismissCelebration() {
        _celebrationEvent.value = null
    }

    fun undoLastCheckIn() {
        val action = _undoableCheckIn.value ?: return
        _undoableCheckIn.value = null
        AttendanceWidgetStore.clearPendingExternalUndo(appContext)
        viewModelScope.launch {
            _isSaving.value = true
            val current = uiState.value
            val prev = action.previousRecord
            val result = if (prev != null) {
                repository.upsertAttendance(
                    targetUserId = currentUserId,
                    dateKey = action.dateKey,
                    termId = prev.termId.ifBlank { AttendanceCalculator.resolveTermIdForDate(action.dateKey, current.terms) },
                    status = prev.statusEnum,
                    subjectTags = prev.subjectTags,
                    note = prev.note,
                    markedBy = "student",
                    existingRecords = current.attendanceRecords
                )
            } else {
                repository.deleteAttendance(
                    targetUserId = currentUserId,
                    dateKey = action.dateKey,
                    existingRecords = current.attendanceRecords
                )
            }
            _isSaving.value = false
            result.onSuccess {
                _snackbarMessage.value = if (prev != null) {
                    "Reverted ${DateUtils.formatReadableShort(action.dateKey)} back to ${prev.statusEnum.label}"
                } else {
                    "Undid entry for ${DateUtils.formatReadableShort(action.dateKey)}"
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not undo check-in"
            }
        }
    }

    fun bulkLogAttendance(
        dateKeys: List<String>,
        status: AttendanceStatus,
        note: String? = null,
        onComplete: () -> Unit = {}
    ) {
        val current = uiState.value
        val today = DateUtils.todayKey()
        val nonSchoolDates = current.calendarEvents
            .filter { it.nonSchoolDay }
            .map { it.dateKey }
            .toSet()
        val eligibleDates = dateKeys.filter { key ->
            key <= today &&
                !DateUtils.isWeekend(key) &&
                key !in nonSchoolDates &&
                !current.isBeforeSchoolStart(key) &&
                !current.isDateLocked(key)
        }.distinct()
        if (eligibleDates.isEmpty()) {
            _snackbarMessage.value = "No editable school days selected"
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.bulkUpsertAttendance(
                targetUserId = currentUserId,
                dateKeys = eligibleDates,
                status = status,
                note = note,
                terms = current.terms,
                existingRecords = current.attendanceRecords,
                lockedBaselineRange = current.profile?.lockedBaselineRange,
                periodBlocks = current.periodBlocks
            )
            _isSaving.value = false
            result.onSuccess { count ->
                _snackbarMessage.value = "Marked $count day${if (count == 1) "" else "s"} as ${status.label}"
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not bulk-update selected days"
            }
        }
    }

    fun savePersonalMarker(
        dateKey: String,
        title: String,
        category: PersonalMarkerCategory,
        note: String?,
        existingMarkerId: String? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.savePersonalMarker(
                userId = currentUserId,
                dateKey = dateKey,
                title = title,
                category = category,
                note = note,
                existingMarkerId = existingMarkerId
            )
            _isSaving.value = false
            result.onSuccess {
                _snackbarMessage.value = if (existingMarkerId == null) {
                    "Pinned \"$title\" to ${DateUtils.formatReadableShort(dateKey)}"
                } else {
                    "Updated marker \"$title\""
                }
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not save personal marker"
            }
        }
    }

    fun deletePersonalMarker(markerId: String) {
        viewModelScope.launch {
            val result = repository.deletePersonalMarker(currentUserId, markerId)
            result.onSuccess {
                _snackbarMessage.value = "Removed personal calendar marker"
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not remove marker"
            }
        }
    }

    fun setAfterSchoolUnlockPromptEnabled(enabled: Boolean) {
        viewModelScope.launch {
            localPrefs.saveAfterSchoolUnlockPromptEnabled(enabled)
            if (enabled) {
                SyncScheduler.ensureAllBackgroundChecksScheduled(
                    context = appContext,
                    instantListenerEnabled = true,
                    allowStartForegroundService = true
                )
            }
            _snackbarMessage.value = if (enabled) {
                "After-School Unlock Prompt on — reminds you on first unlock after 1:45 PM"
            } else {
                "After-School Unlock Prompt paused"
            }
        }
    }

    fun setDangerZoneNudgeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            localPrefs.saveDangerZoneNudgeEnabled(enabled)
            _snackbarMessage.value = if (enabled) {
                "80% Edge Smart Nudge enabled"
            } else {
                "80% Edge Smart Nudge muted"
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
        if (uiState.value.isDateLocked(dateKey)) {
            _snackbarMessage.value = "Cannot modify days inside a locked attendance period"
            return
        }
        if (_undoableCheckIn.value?.dateKey == dateKey) {
            _undoableCheckIn.value = null
        }
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

    fun adminSaveCalendarEvent(
        eventId: String?,
        title: String,
        dateKey: String,
        eventType: CalendarEventType,
        description: String?,
        targetAll: Boolean,
        targetGrades: List<String>,
        targetStreams: List<String>,
        targetClasses: List<String>,
        targetUserIds: List<String>,
        audienceSummary: String,
        alsoSendNotification: Boolean,
        onComplete: () -> Unit = {}
    ) {
        if (!isVerifiedAdmin) return
        viewModelScope.launch {
            _isSaving.value = true
            val year = dateKey.take(4).toIntOrNull() ?: 2026
            val result = repository.saveCalendarEvent(
                eventId = eventId,
                title = title,
                dateKey = dateKey,
                eventType = eventType,
                academicYear = year,
                description = description,
                targetType = if (targetAll) CalendarEventTargetType.ALL else CalendarEventTargetType.CLASS,
                targetValue = if (targetAll) null else audienceSummary.take(60),
                targetAll = targetAll,
                targetGrades = targetGrades,
                targetStreams = targetStreams,
                targetClasses = targetClasses,
                targetUserIds = targetUserIds,
                audienceSummary = audienceSummary,
                alsoSendNotification = alsoSendNotification
            )
            _isSaving.value = false
            result.onSuccess {
                _snackbarMessage.value = if (alsoSendNotification) {
                    "Calendar updated & notification sent to $audienceSummary"
                } else if (eventId == null) {
                    "Calendar update added for ${DateUtils.formatReadableShort(dateKey)}"
                } else {
                    "Calendar update saved"
                }
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not save calendar update"
            }
        }
    }

    fun adminDeleteCalendarEvent(eventId: String) {
        if (!isVerifiedAdmin) return
        viewModelScope.launch {
            val res = repository.deleteCalendarEvent(eventId)
            res.onSuccess { _snackbarMessage.value = "Calendar event deleted" }
                .onFailure { err -> _snackbarMessage.value = err.message ?: "Could not delete event" }
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

    fun setSundayPrepEnabled(enabled: Boolean) {
        viewModelScope.launch {
            localPrefs.saveSundayPrepEnabled(enabled)
            ReminderScheduler.scheduleSundayPrep(appContext, enabled)
            _snackbarMessage.value = if (enabled) {
                "Sunday 7:00 PM week-ahead prep enabled"
            } else {
                "Sunday evening prep turned off"
            }
        }
    }

    fun setAppearance(themeMode: AppThemeMode, accentPalette: AppAccentPalette) {
        viewModelScope.launch {
            localPrefs.saveAppearance(themeMode, accentPalette)
        }
    }

    fun setInstantBackgroundAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            localPrefs.saveInstantBackgroundAlertsEnabled(enabled)
            SyncScheduler.ensureAllBackgroundChecksScheduled(
                context = appContext,
                instantListenerEnabled = enabled,
                allowStartForegroundService = true
            )
            _snackbarMessage.value = if (enabled) {
                "Instant background alerts enabled — announcements will arrive even when app is closed"
            } else {
                "Instant listener paused — periodic background checks remain active"
            }
        }
    }

    fun checkBackgroundNotificationsNow() {
        if (_isCheckingBackgroundAlerts.value) return
        viewModelScope.launch {
            _isCheckingBackgroundAlerts.value = true
            try {
                val result = BackgroundBroadcastChecker.checkAndDeliver(appContext)
                _snackbarMessage.value = when {
                    result.permissionMissing ->
                        "Notifications are blocked — tap Allow Notifications first"
                    result.error != null ->
                        "Background check couldn't complete: ${result.error}"
                    result.deliveredCount > 0 ->
                        "Delivered ${result.deliveredCount} new announcement notification${if (result.deliveredCount == 1) "" else "s"}!"
                    else ->
                        "Background check OK — you're all caught up!"
                }
            } finally {
                _isCheckingBackgroundAlerts.value = false
            }
        }
    }

    fun submitFeedback(
        type: FeedbackType,
        title: String,
        message: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val profile = uiState.value.profile
            val res = repository.submitFeedback(
                userId = currentUserId,
                userName = profile?.fullName ?: googleDisplayName ?: "Student",
                userEmail = profile?.email?.ifBlank { userEmail } ?: userEmail,
                className = profile?.className?.takeIf { it.isNotBlank() },
                type = type,
                title = title,
                message = message,
                appVersion = BuildConfig.VERSION_NAME
            )
            _isSaving.value = false
            res.onSuccess {
                _snackbarMessage.value = if (type == FeedbackType.BUG) {
                    "Bug report sent — thank you for helping improve 80endance!"
                } else {
                    "Idea received — thanks for helping shape 80endance!"
                }
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not send feedback"
            }
        }
    }

    fun submitNextDayVote(going: Boolean) {
        val snapshot = uiState.value
        if (!snapshot.appSettings.nextDayPollEnabled) return
        val window = snapshot.pollWindow
        if (!window.isOpen) {
            _snackbarMessage.value = "Poll is only open from 8:00 PM to 5:00 AM before a school day"
            return
        }
        viewModelScope.launch {
            val res = repository.submitNextDayVote(
                userId = currentUserId,
                targetDateKey = window.targetDateKey,
                going = going,
                profile = snapshot.profile
            )
            res.onSuccess {
                runCatching {
                    NotificationManagerCompat.from(appContext).cancel(ReminderScheduler.POLL_NOTIFICATION_ID)
                }
                val fetchedVotes = repository.getNextDayVotesForDate(window.targetDateKey)
                val myVoteEntry = NextDaySchoolVote(
                    id = "${window.targetDateKey}_$currentUserId",
                    userId = currentUserId,
                    targetDateKey = window.targetDateKey,
                    going = going,
                    grade = snapshot.profile?.grade,
                    alStream = snapshot.profile?.alStream,
                    className = snapshot.profile?.className
                )
                val merged = fetchedVotes.filterNot { it.userId == currentUserId } + myVoteEntry
                val summary = AttendanceCalculator.calculateNextDayPollSummary(
                    votes = merged,
                    currentUserId = currentUserId,
                    myClassName = snapshot.profile?.className,
                    targetDateKey = window.targetDateKey
                )
                ReminderScheduler.showNextDayPollAnalysisNotification(
                    context = appContext,
                    summary = summary,
                    userVotedGoing = going
                )
                val comingPct = summary.goingPercent.roundToInt()
                _snackbarMessage.value =
                    "Voted ${if (going) "Coming" else "Not Coming"} • $comingPct% coming (${summary.totalVotes} voted)"
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Could not save your vote"
            }
        }
    }
}
