package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.isVerifiedAdmin
import com.example.auth.signOutGoogle
import com.example.data.AppConstants
import com.example.data.AttendanceRepository
import com.example.data.DateUtils
import com.example.data.LocalPreferencesStore
import com.example.system.AppUpdateChecker
import com.example.system.AppUpdateInfo
import com.example.system.AttendanceWidgetStore
import com.example.system.ReminderScheduler
import com.example.system.UpdateCheckResult
import com.example.system.WidgetSnapshot
import com.example.ui.admin.AdminPanelScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.calendar.CalendarScreen
import com.example.ui.components.AttendanceBaselineDialog
import com.example.ui.components.DailyCheckInDialog
import com.example.ui.components.UpdateAvailableDialog
import com.example.ui.home.HomeScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.setup.AcademicSetupScreen
import com.example.ui.theme.EightyEndanceTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AttendanceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.Color
import com.example.ui.components.LiquidGlassBackdropHost
import com.example.ui.components.LiquidGlassBottomBar
import com.example.ui.components.LiquidGlassNavItem
import com.example.ui.components.LiquidGlassNavigationRail
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var openCheckInTrigger by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderScheduler.ensureNotificationChannel(this)
        if (intent?.getBooleanExtra(AppConstants.EXTRA_OPEN_CHECKIN, false) == true) {
            openCheckInTrigger = true
        }
        setContent {
            EightyEndanceTheme {
                AppRoot(
                    openCheckInFromNotification = openCheckInTrigger,
                    onConsumeNotificationTrigger = { openCheckInTrigger = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(AppConstants.EXTRA_OPEN_CHECKIN, false)) {
            openCheckInTrigger = true
        }
    }
}

private enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    CALENDAR("Calendar", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_calendar"),
    REPORTS("Reports", Icons.Rounded.Assessment, Icons.Outlined.Assessment, "nav_reports"),
    SETTINGS("Settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
}

@Composable
private fun AppRoot(
    openCheckInFromNotification: Boolean,
    onConsumeNotificationTrigger: () -> Unit
) {
    val context = LocalContext.current
    val appScope = rememberCoroutineScope()
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val isOnline by remember(context) {
        AppUpdateChecker.observeInternetAvailable(context)
    }.collectAsStateWithLifecycle(initialValue = AppUpdateChecker.isInternetAvailable(context))

    var hasAutoCheckedUpdate by rememberSaveable { mutableStateOf(false) }
    var pendingUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // Automatically check GitHub Releases once internet is available after the app opens
    LaunchedEffect(isOnline) {
        if (isOnline && !hasAutoCheckedUpdate) {
            when (val result = AppUpdateChecker.checkForUpdate(context)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    hasAutoCheckedUpdate = true
                    pendingUpdateInfo = result.info
                }
                is UpdateCheckResult.UpToDate -> {
                    hasAutoCheckedUpdate = true
                }
                is UpdateCheckResult.Offline,
                is UpdateCheckResult.Error -> Unit
            }
        }
    }

    val runManualUpdateCheck: () -> Unit = {
        if (!isCheckingUpdates) {
            appScope.launch {
                isCheckingUpdates = true
                try {
                    when (val result = AppUpdateChecker.checkForUpdate(context)) {
                        is UpdateCheckResult.UpdateAvailable -> {
                            hasAutoCheckedUpdate = true
                            pendingUpdateInfo = result.info
                        }
                        is UpdateCheckResult.UpToDate -> {
                            hasAutoCheckedUpdate = true
                            updateFeedbackMessage = context.getString(
                                R.string.update_up_to_date,
                                AppUpdateChecker.formatVersionLabel(result.currentVersion)
                            )
                        }
                        is UpdateCheckResult.Offline -> {
                            updateFeedbackMessage = context.getString(R.string.update_offline_message)
                        }
                        is UpdateCheckResult.Error -> {
                            updateFeedbackMessage = result.message
                        }
                    }
                } finally {
                    isCheckingUpdates = false
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LiquidGlassBackdropHost {
            val user = currentUser
            if (user == null) {
                AuthScreen(onAuthSuccess = {})
            } else {
                key(user.uid) {
                    AuthenticatedAppShell(
                        user = user,
                        openCheckInFromNotification = openCheckInFromNotification,
                        onConsumeNotificationTrigger = onConsumeNotificationTrigger,
                        isCheckingUpdates = isCheckingUpdates,
                        updateFeedbackMessage = updateFeedbackMessage,
                        onConsumeUpdateFeedback = { updateFeedbackMessage = null },
                        onCheckForUpdates = runManualUpdateCheck
                    )
                }
            }
        }
    }

    pendingUpdateInfo?.let { info ->
        UpdateAvailableDialog(
            updateInfo = info,
            onUpdateNow = {
                val opened = AppUpdateChecker.openLatestRelease(context, info.releaseUrl)
                if (!opened) {
                    updateFeedbackMessage = context.getString(R.string.update_browser_error)
                }
                pendingUpdateInfo = null
            },
            onDismiss = { pendingUpdateInfo = null }
        )
    }
}

@Composable
private fun AuthenticatedAppShell(
    user: FirebaseUser,
    openCheckInFromNotification: Boolean,
    onConsumeNotificationTrigger: () -> Unit,
    isCheckingUpdates: Boolean,
    updateFeedbackMessage: String?,
    onConsumeUpdateFeedback: () -> Unit,
    onCheckForUpdates: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val isAdmin = remember(user.uid, user.email, user.isEmailVerified) { user.isVerifiedAdmin() }

    val attendanceViewModel: AttendanceViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                AttendanceViewModel(
                    repository = AttendanceRepository(app),
                    localPrefs = LocalPreferencesStore(app),
                    appContext = app,
                    currentUserId = user.uid,
                    userEmail = user.email.orEmpty(),
                    googleDisplayName = user.displayName,
                    googlePhotoUrl = user.photoUrl?.toString()
                )
            }
        }
    )

    val state by attendanceViewModel.uiState.collectAsStateWithLifecycle()
    val scopeMode by attendanceViewModel.scopeMode.collectAsStateWithLifecycle()
    val manualCheckInDate by attendanceViewModel.manualCheckInDate.collectAsStateWithLifecycle()
    val snackbarMessage by attendanceViewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isSaving by attendanceViewModel.isSaving.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var editingProfileInSettings by rememberSaveable { mutableStateOf(false) }
    var showAdminPanel by rememberSaveable { mutableStateOf(false) }
    var showBaselineImportDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            attendanceViewModel.clearSnackbar()
        }
    }

    LaunchedEffect(updateFeedbackMessage) {
        updateFeedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            onConsumeUpdateFeedback()
        }
    }

    LaunchedEffect(openCheckInFromNotification, state.profile?.setupCompleted) {
        if (openCheckInFromNotification && state.profile?.setupCompleted == true) {
            attendanceViewModel.openCheckInPopup(DateUtils.todayKey())
            onConsumeNotificationTrigger()
        }
    }

    // Loading state before first local/cloud profile snapshot
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // First-time Academic Setup OR Edit Profile flow
    if (state.profile == null || !state.profile!!.setupCompleted || editingProfileInSettings) {
        if (editingProfileInSettings) {
            BackHandler { editingProfileInSettings = false }
        }
        AcademicSetupScreen(
            initialDisplayName = user.displayName,
            existingProfile = state.profile,
            isSaving = isSaving,
            onSave = { first, last, nick, school, grade, stream, cls, examYr, subjects, remOn, remH, remM, countExc, priorP, priorA ->
                attendanceViewModel.saveStudentProfile(
                    firstName = first,
                    lastName = last,
                    nickname = nick,
                    schoolName = school,
                    grade = grade,
                    alStream = stream,
                    className = cls,
                    alExamYear = examYr,
                    subjects = subjects,
                    reminderEnabled = remOn,
                    reminderHour = remH,
                    reminderMinute = remM,
                    countExcusedAsPresent = countExc,
                    priorPresentCount = priorP,
                    priorAbsentCount = priorA,
                    onComplete = { editingProfileInSettings = false }
                )
            }
        )
        return
    }

    // Discreet Admin Panel (exclusively accessible from Settings when verified admin)
    if (isAdmin && showAdminPanel) {
        val adminViewModel: AdminViewModel = viewModel(
            key = "admin_${user.uid}",
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY])
                    AdminViewModel(AttendanceRepository(app))
                }
            }
        )
        AdminPanelScreen(
            viewModel = adminViewModel,
            onBack = { showAdminPanel = false }
        )
        return
    }

    if (currentTab != AppTab.HOME) {
        BackHandler { currentTab = AppTab.HOME }
    }

    val navItems = remember {
        AppTab.entries.map { tab ->
            LiquidGlassNavItem(
                label = tab.label,
                selectedIcon = tab.selectedIcon,
                unselectedIcon = tab.unselectedIcon,
                testTag = tab.tag
            )
        }
    }

    // Adaptive Layout: Floating LiquidGlassBottomBar on Compact phones,
    // Floating LiquidGlassNavigationRail on Medium/Expanded screens (>= 600dp)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        val contentBackdrop = rememberLayerBackdrop()
        val navBarInsetBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomScrollPadding = if (useRail) navBarInsetBottom + 24.dp else navBarInsetBottom + 106.dp

        Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Ambient background + scrollable screens captured into contentBackdrop so
            // cards visibly scroll and refract underneath the floating Liquid Glass bar/rail,
            // while LiquidGlassButtons inside sample their sibling ambientBackdrop without recursion.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(contentBackdrop)
            ) {
                LiquidGlassBackdropHost {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(start = if (useRail) 104.dp else 0.dp)
                    ) {
                        Crossfade(
                            targetState = currentTab,
                            animationSpec = tween(220),
                            label = "tab_crossfade"
                        ) { tab ->
                    when (tab) {
                        AppTab.HOME -> HomeScreen(
                            state = state,
                            scopeMode = scopeMode,
                            onScopeModeChange = { attendanceViewModel.setScopeMode(it) },
                            onOpenCheckIn = { dateKey -> attendanceViewModel.openCheckInPopup(dateKey) },
                            onSyncClick = { attendanceViewModel.triggerManualSync() },
                            onQuickLogDate = { dateKey, status -> attendanceViewModel.quickLogAttendance(dateKey, status) },
                            onSaveCustomAvatar = { base64 -> attendanceViewModel.updateCustomAvatar(base64) },
                            bottomBarPadding = bottomScrollPadding
                        )

                        AppTab.CALENDAR -> CalendarScreen(
                            state = state,
                            onEditDate = { dateKey -> attendanceViewModel.openCheckInPopup(dateKey) },
                            onDeleteDate = { dateKey -> attendanceViewModel.deleteAttendance(dateKey) },
                            onOpenBaselineImport = { showBaselineImportDialog = true },
                            bottomBarPadding = bottomScrollPadding
                        )

                        AppTab.REPORTS -> ReportsScreen(
                            state = state,
                            onToggleCountExcused = {},
                            bottomBarPadding = bottomScrollPadding
                        )

                        AppTab.SETTINGS -> SettingsScreen(
                            state = state,
                            isVerifiedAdmin = isAdmin,
                            onEditProfile = { editingProfileInSettings = true },
                            onUpdatePreferences = { enabled, hour, minute, countExc ->
                                attendanceViewModel.updateReminderAndExcused(enabled, hour, minute, countExc)
                            },
                            onPreviewCheckInPopup = {
                                attendanceViewModel.openCheckInPopup(DateUtils.todayKey())
                            },
                            onManualSync = { attendanceViewModel.triggerManualSync() },
                            onOpenAdminPanel = { if (isAdmin) showAdminPanel = true },
                            onSignOut = {
                                AttendanceWidgetStore.saveAndRefresh(context, WidgetSnapshot(signedIn = false))
                                signOutGoogle(credentialManager, scope)
                            },
                            onSaveCustomAvatar = { base64 -> attendanceViewModel.updateCustomAvatar(base64) },
                            onOpenBaselineImport = { showBaselineImportDialog = true },
                            onClearBaseline = { attendanceViewModel.clearAttendancePercentageBaseline() },
                            isCheckingUpdates = isCheckingUpdates,
                            onCheckForUpdates = onCheckForUpdates,
                            bottomBarPadding = bottomScrollPadding
                        )
                    }
                        }
                    }
                }
            }

            // Layer 2: Floating Liquid Glass Navigation (sibling overlay sampling contentBackdrop)
            if (useRail) {
                LiquidGlassNavigationRail(
                    backdrop = contentBackdrop,
                    items = navItems,
                    selectedIndex = currentTab.ordinal,
                    onItemSelected = { idx -> currentTab = AppTab.entries[idx] },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            } else {
                LiquidGlassBottomBar(
                    backdrop = contentBackdrop,
                    items = navItems,
                    selectedIndex = currentTab.ordinal,
                    onItemSelected = { idx -> currentTab = AppTab.entries[idx] },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Layer 3: Snackbar positioned safely above the floating glass bar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (useRail) 16.dp else 94.dp, start = 16.dp, end = 16.dp)
            )
        }
    }

    // Smart Daily Check-In Modal Popup (Auto-triggered after reminder time OR manual tap)
    val activeCheckInDate = manualCheckInDate
        ?: if (state.shouldAutoShowCheckIn) DateUtils.todayKey() else null

    if (activeCheckInDate != null) {
        val existingRec = remember(state.attendanceRecords, activeCheckInDate) {
            state.attendanceRecords.firstOrNull { it.dateKey == activeCheckInDate }
        }
        DailyCheckInDialog(
            dateKey = activeCheckInDate,
            existingRecord = existingRec,
            subjects = state.profile?.subjects.orEmpty(),
            isSaving = isSaving,
            onDismiss = {
                attendanceViewModel.dismissCheckInPopup(
                    markDismissedToday = (manualCheckInDate == null)
                )
            },
            onSave = { status, subjectTags, note ->
                attendanceViewModel.logAttendance(activeCheckInDate, status, subjectTags, note)
            }
        )
    }

    if (showBaselineImportDialog) {
        AttendanceBaselineDialog(
            profile = state.profile,
            activeTerm = state.activeTerm,
            terms = state.terms,
            calendarEvents = state.calendarEvents,
            existingRecords = state.attendanceRecords,
            isSaving = isSaving,
            onDismiss = { showBaselineImportDialog = false },
            onSave = { startKey, endKey, pct, presentDays, absentDays ->
                attendanceViewModel.saveAttendancePercentageBaseline(
                    startDateKey = startKey,
                    endDateKey = endKey,
                    percentage = pct,
                    presentDays = presentDays,
                    absentDays = absentDays,
                    onComplete = { showBaselineImportDialog = false }
                )
            },
            onClearBaseline = {
                attendanceViewModel.clearAttendancePercentageBaseline()
            }
        )
    }
}
