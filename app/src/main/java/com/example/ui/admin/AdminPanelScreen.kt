package com.example.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.CalendarEvent
import com.example.data.DateUtils
import com.example.data.FeedbackStatus
import com.example.data.FeedbackType
import com.example.data.Notice
import com.example.data.NoticeTargetType
import com.example.data.UserProfile
import com.example.system.AppUpdateChecker
import com.example.system.UpdateCheckResult
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.PulsingLiveDot
import com.example.ui.components.RollingCounterText
import com.example.ui.components.SmoothTabContent
import com.example.ui.components.springPressScale
import com.example.ui.theme.LocalAttendanceColors
import com.example.viewmodel.AdminViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminPanelScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbar.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val semantic = LocalAttendanceColors.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var showNoticeDialog by remember { mutableStateOf(false) }
    var editingNotice by remember { mutableStateOf<Notice?>(null) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var editingCalendarEvent by remember { mutableStateOf<CalendarEvent?>(null) }

    // Student filter state
    var searchQuery by remember { mutableStateOf("") }
    var studentFilter by remember { mutableStateOf("all") } // all, risk, eligible
    var overrideStudent by remember { mutableStateOf<UserProfile?>(null) }
    var overrideDateKey by remember { mutableStateOf(DateUtils.todayKey()) }
    var editStudentProfile by remember { mutableStateOf<UserProfile?>(null) }
    var previewAvatarStudent by remember { mutableStateOf<UserProfile?>(null) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var latestGithubVersion by remember { mutableStateOf<String?>(null) }
    var isCheckingGithubRelease by remember { mutableStateOf(false) }
    val todayKey = remember { DateUtils.todayKey() }

    fun refreshGithubRelease() {
        if (isCheckingGithubRelease) return
        scope.launch {
            isCheckingGithubRelease = true
            val check = AppUpdateChecker.checkForUpdate(context)
            latestGithubVersion = when (check) {
                is UpdateCheckResult.UpdateAvailable -> check.info.latestVersion
                is UpdateCheckResult.UpToDate -> check.latestFoundTag ?: check.currentVersion
                else -> latestGithubVersion
            }
            isCheckingGithubRelease = false
        }
    }

    LaunchedEffect(Unit) {
        refreshGithubRelease()
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Settings")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = 6
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (selectedTab == 6) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = if (selectedTab == 6) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("admin_topbar_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Open Config & APK Override",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Config",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val showFab = selectedTab == 0 || selectedTab == 3
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (selectedTab == 0) {
                            editingNotice = null
                            showNoticeDialog = true
                        } else {
                            editingCalendarEvent = null
                            showAddEventDialog = true
                        }
                    },
                    modifier = Modifier
                        .springPressScale()
                        .testTag(if (selectedTab == 0) "admin_add_notice_fab" else "admin_add_event_fab")
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = if (selectedTab == 0) "Add notice" else "Add calendar event"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Online & Total Accounts Presence Strip
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_live_presence_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulsingLiveDot(color = semantic.present, dotSize = 8.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        RollingCounterText(
                            count = state.onlineNowCount,
                            suffix = " online right now",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = semantic.present
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.guestObserverCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${state.guestObserverCount} guest",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            RollingCounterText(
                                count = state.activeStudentCount,
                                suffix = " students",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            val openFeedbackCount = remember(state.feedbackItems) {
                state.feedbackItems.count { it.statusEnum == FeedbackStatus.OPEN }
            }
            fun selectTab(index: Int) {
                if (selectedTab != index) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedTab = index
                }
            }
            PrimaryScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
                Tab(selected = selectedTab == 0, onClick = { selectTab(0) }, text = { Text("Notices") })
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectTab(1) },
                    text = { Text("Students (${state.activeStudentCount})") }
                )
                Tab(
                    selected = selectedTab == 6,
                    onClick = { selectTab(6) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Text("Config", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("admin_tab_config")
                )
                Tab(selected = selectedTab == 2, onClick = { selectTab(2) }, text = { Text("Notify") }, modifier = Modifier.testTag("admin_tab_notify"))
                Tab(selected = selectedTab == 3, onClick = { selectTab(3) }, text = { Text("Calendar") })
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectTab(4) },
                    text = { Text("Catalog") },
                    modifier = Modifier.testTag("admin_tab_catalog")
                )
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectTab(5) },
                    text = {
                        Text(
                            if (openFeedbackCount > 0) "Feedback ($openFeedbackCount)" else "Feedback"
                        )
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
            ) {
                SmoothTabContent(
                    targetState = selectedTab,
                    orderOf = { it },
                    label = "admin_tab_transition"
                ) { activeTab ->
                when (activeTab) {
                    0 -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Pinned notices rotate automatically every 5 seconds on student dashboards.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (state.notices.isEmpty()) {
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No notices yet. Tap + to publish a school-wide, stream, or class update.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(18.dp)
                                        )
                                    }
                                }
                            } else {
                                items(state.notices, key = { it.id }) { notice ->
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = buildString {
                                                            append(notice.categoryEnum.label)
                                                            append(" • ")
                                                            append(
                                                                when (notice.targetTypeEnum) {
                                                                    NoticeTargetType.ALL -> "All"
                                                                    else -> notice.targetValue ?: notice.targetTypeEnum.label
                                                                }
                                                            )
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    val scheduledMs = notice.scheduledAtMillis
                                                    if (scheduledMs != null && notice.isScheduledForFuture(state.presenceNowMillis)) {
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                                            shape = RoundedCornerShape(50)
                                                        ) {
                                                            Text(
                                                                text = "Scheduled • ${DateUtils.formatScheduledDateTime(scheduledMs)}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Row {
                                                    IconButton(onClick = { viewModel.togglePin(notice) }) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.PushPin,
                                                            contentDescription = "Toggle pin",
                                                            tint = if (notice.pinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                    IconButton(onClick = { editingNotice = notice; showNoticeDialog = true }) {
                                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit notice")
                                                    }
                                                    IconButton(onClick = { viewModel.deleteNotice(notice.id) }) {
                                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete notice")
                                                    }
                                                }
                                            }
                                            Text(notice.title, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                notice.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }

                    1 -> {
                        val filteredStudents = remember(state.students, searchQuery, studentFilter, state.presenceNowMillis) {
                            state.students.filter { st ->
                                val matchesQuery = searchQuery.isBlank() ||
                                    st.fullName.contains(searchQuery, ignoreCase = true) ||
                                    st.className.contains(searchQuery, ignoreCase = true) ||
                                    st.alStream.contains(searchQuery, ignoreCase = true) ||
                                    st.email.contains(searchQuery, ignoreCase = true)
                                val matchesFilter = when (studentFilter) {
                                    "online" -> !st.guestObserver && st.isCurrentlyOnline(state.presenceNowMillis)
                                    "risk" -> !st.guestObserver && st.quickOverallPercent < state.appSettings.defaultEligibilityPercent
                                    "eligible" -> !st.guestObserver && st.quickOverallPercent >= state.appSettings.defaultEligibilityPercent
                                    "guests" -> st.guestObserver
                                    else -> true
                                }
                                matchesQuery && matchesFilter
                            }.sortedWith(
                                compareByDescending<UserProfile> { !it.guestObserver && it.isCurrentlyOnline(state.presenceNowMillis) }
                                    .thenByDescending { it.lastSeenAt?.seconds ?: 0L }
                                    .thenBy { it.fullName.lowercase() }
                            )
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search name, class (13M2), or stream") },
                                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(selected = studentFilter == "all", onClick = { studentFilter = "all" }, label = { Text("All (${state.students.size})") })
                                    FilterChip(
                                        selected = studentFilter == "online",
                                        onClick = { studentFilter = "online" },
                                        label = { Text("Online (${state.onlineNowCount})") },
                                        modifier = Modifier.testTag("admin_filter_online")
                                    )
                                    FilterChip(selected = studentFilter == "risk", onClick = { studentFilter = "risk" }, label = { Text("Below 80%") })
                                    FilterChip(selected = studentFilter == "eligible", onClick = { studentFilter = "eligible" }, label = { Text("Eligible") })
                                    FilterChip(
                                        selected = studentFilter == "guests",
                                        onClick = { studentFilter = "guests" },
                                        label = { Text("Guests (${state.guestObserverCount})") },
                                        modifier = Modifier.testTag("admin_filter_guests")
                                    )
                                }
                            }
                            items(filteredStudents, key = { it.userId }) { student ->
                                val pct = student.quickOverallPercent
                                val isOk = pct >= state.appSettings.defaultEligibilityPercent
                                val studentOnline = student.isCurrentlyOnline(state.presenceNowMillis)
                                val lastSeenLabel = DateUtils.formatLastSeen(
                                    timestampMillis = student.lastSeenAt?.toDate()?.time,
                                    isOnline = studentOnline,
                                    nowMillis = state.presenceNowMillis
                                )
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .springPressScale()
                                        .clickable { viewModel.selectStudent(student.userId) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box {
                                            ProfileAvatar(
                                                customAvatarBase64 = student.customAvatarBase64,
                                                googlePhotoUrl = student.photoUrl,
                                                displayName = student.fullName,
                                                size = 46.dp,
                                                onClick = { previewAvatarStudent = student },
                                                modifier = Modifier.testTag("admin_student_avatar_${student.userId}")
                                            )
                                            if (studentOnline) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .size(13.dp)
                                                        .clip(CircleShape)
                                                        .background(semantic.present)
                                                        .border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(student.fullName, style = MaterialTheme.typography.titleSmall)
                                                if (student.guestObserver) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "GUEST",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${student.className} • ${student.alStream} • ${student.alExamYear} A/L",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "P:${student.presentCount}  A:${student.absentCount}  M:${student.medicalCount}  E:${student.schoolEventCount}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (studentOnline) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(7.dp)
                                                            .clip(CircleShape)
                                                            .background(semantic.present)
                                                    )
                                                }
                                                Text(
                                                    text = lastSeenLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (studentOnline) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (studentOnline) semantic.present else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.testTag("admin_student_last_seen_${student.userId}")
                                                )
                                                val rawVersion = student.appVersionName?.trim()?.takeIf { it.isNotBlank() }
                                                val versionLabel = rawVersion?.let { if (it.startsWith("v", ignoreCase = true)) it else "v$it" } ?: "v—"
                                                val isLatestVersion = rawVersion != null && rawVersion == BuildConfig.VERSION_NAME
                                                Surface(
                                                    color = when {
                                                        isLatestVersion -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                                        rawVersion != null -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                                                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.testTag("admin_student_version_${student.userId}")
                                                ) {
                                                    Text(
                                                        text = versionLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = when {
                                                            isLatestVersion -> MaterialTheme.colorScheme.onPrimaryContainer
                                                            rawVersion != null -> MaterialTheme.colorScheme.onTertiaryContainer
                                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Surface(
                                            color = if (isOk) semantic.presentContainer else semantic.absentContainer,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = String.format(Locale.US, "%.1f%%", pct),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isOk) semantic.onPresentContainer else semantic.onAbsentContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        AdminBroadcastTab(
                            students = state.students,
                            broadcasts = state.broadcasts,
                            isBusy = state.isBusy,
                            onSendBroadcast = { title, message, targetAll, grades, streams, classes, userIds, summary, onDone ->
                                viewModel.sendBroadcastNotification(
                                    title = title,
                                    message = message,
                                    targetAll = targetAll,
                                    targetGrades = grades,
                                    targetStreams = streams,
                                    targetClasses = classes,
                                    targetUserIds = userIds,
                                    audienceSummary = summary,
                                    onDone = onDone
                                )
                            },
                            onDeleteBroadcast = { id -> viewModel.deleteBroadcastNotification(id) }
                        )
                    }

                    3 -> {
                        val sortedEvents = remember(state.calendarEvents, todayKey) {
                            val upcoming = state.calendarEvents
                                .filter { it.dateKey >= todayKey }
                                .sortedBy { it.dateKey }
                            val past = state.calendarEvents
                                .filter { it.dateKey < todayKey }
                                .sortedByDescending { it.dateKey }
                            upcoming + past
                        }
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Multi-Day / Range / Class-Specific No School Card
                            item {
                                AdminNoSchoolCard(
                                    isBusy = state.isBusy,
                                    onMarkNoSchoolDays = { dates, reason, targetType, targetValue, alsoNotice ->
                                        viewModel.markNoSchoolDays(
                                            dateKeys = dates,
                                            reason = reason,
                                            targetType = targetType,
                                            targetValue = targetValue,
                                            alsoPostNotice = alsoNotice
                                        )
                                    }
                                )
                            }

                            // Guided Term Rollover Wizard
                            item {
                                AdminTermRolloverCard(
                                    terms = state.terms,
                                    isBusy = state.isBusy,
                                    onRolloverTerm = { targetTerm, alsoNotice ->
                                        viewModel.rolloverTerm(targetTerm, alsoNotice)
                                    },
                                    schoolStartDate = state.appSettings.effectiveSchoolStartDate(2028),
                                    onSaveSchoolStartDate = { newDate ->
                                        viewModel.saveSchoolStartDate(newDate)
                                    },
                                    onDeleteTerm = { term ->
                                        viewModel.deleteTerm(term.id)
                                    }
                                )
                            }

                            // Calendar Events List (App Config, Direct APK Override & Feature Flags moved to Config tab)
                            item {
                                Text("Calendar Events (${sortedEvents.size})", style = MaterialTheme.typography.titleSmall)
                            }
                            items(sortedEvents, key = { it.id }) { ev ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ev.title, style = MaterialTheme.typography.titleSmall)
                                            val scopeSuffix = ev.targetDisplayLabel?.let { " • $it" }.orEmpty()
                                            Text(
                                                "${ev.dateKey} • ${ev.eventTypeEnum.label}$scopeSuffix",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (!ev.description.isNullOrBlank()) {
                                                Text(
                                                    text = ev.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingCalendarEvent = ev
                                                    showAddEventDialog = true
                                                }
                                            ) {
                                                Icon(Icons.Rounded.Edit, contentDescription = "Edit event")
                                            }
                                            IconButton(onClick = { viewModel.deleteCalendarEvent(ev.id) }) {
                                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete event")
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }

                    4 -> {
                        AdminCatalogTab(
                            catalog = state.academicCatalog,
                            isBusy = state.isBusy,
                            onSaveCatalog = { updated, message ->
                                viewModel.saveAcademicCatalog(updated, message)
                            }
                        )
                    }

                    5 -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Bug reports & feature suggestions submitted by students from Settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (state.feedbackItems.isEmpty()) {
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No bug reports or suggestions yet.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(18.dp)
                                        )
                                    }
                                }
                            } else {
                                items(state.feedbackItems, key = { it.id }) { fb ->
                                    val isBug = fb.typeEnum == FeedbackType.BUG
                                    val isCatalogReq = fb.typeEnum == FeedbackType.CATALOG_REQUEST
                                    val isResolved = fb.statusEnum == FeedbackStatus.RESOLVED
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = when {
                                                            isBug -> Icons.Rounded.BugReport
                                                            isCatalogReq -> Icons.AutoMirrored.Rounded.MenuBook
                                                            else -> Icons.Rounded.Lightbulb
                                                        },
                                                        contentDescription = fb.typeEnum.label,
                                                        tint = when {
                                                            isBug -> semantic.absent
                                                            isCatalogReq -> MaterialTheme.colorScheme.tertiary
                                                            else -> MaterialTheme.colorScheme.primary
                                                        },
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = "${fb.typeEnum.label} • ${fb.statusEnum.label}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (isResolved) semantic.present else MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Row {
                                                    if (!isResolved) {
                                                        IconButton(
                                                            onClick = { viewModel.updateFeedbackStatus(fb.id, FeedbackStatus.RESOLVED) }
                                                        ) {
                                                            Icon(
                                                                Icons.Rounded.CheckCircle,
                                                                contentDescription = "Mark resolved",
                                                                tint = semantic.present
                                                            )
                                                        }
                                                    }
                                                    IconButton(onClick = { viewModel.deleteFeedback(fb.id) }) {
                                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete feedback")
                                                    }
                                                }
                                            }
                                            Text(
                                                text = fb.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = fb.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = listOfNotNull(
                                                    fb.userName,
                                                    fb.className?.takeIf { it.isNotBlank() },
                                                    fb.userEmail,
                                                    "v${fb.appVersion}"
                                                ).joinToString(" • "),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(32.dp)) }
                        }
                    }

                    6 -> {
                        AdminConfigTab(
                            appSettings = state.appSettings,
                            students = state.students,
                            blockedEmails = state.blockedEmails,
                            latestGithubVersion = latestGithubVersion,
                            isCheckingGithubRelease = isCheckingGithubRelease,
                            isBusy = state.isBusy,
                            onRefreshGithubRelease = { refreshGithubRelease() },
                            onSaveDirectApkOverride = { url, version ->
                                viewModel.saveDirectApkOverride(url, version)
                            },
                            onSetNextDayPollEnabled = { enabled ->
                                viewModel.setNextDayPollEnabled(enabled)
                            },
                            onSaveGlobalSettings = { updatedSettings ->
                                viewModel.saveGlobalSettings(updatedSettings)
                            },
                            onUnblockEmail = { email ->
                                viewModel.unblockEmail(email)
                            }
                        )
                    }
                }
                }
            }
        }
    }

    // Selected Student Detail Sheet (Full Monthly Calendar View + Locked Periods + Guest Observer + Remove & Block)
    state.selectedStudent?.let { student ->
        ModalBottomSheet(onDismissRequest = { viewModel.selectStudent(null) }) {
            AdminStudentCalendarSheet(
                student = student,
                records = state.selectedStudentRecords,
                periodBlocks = state.selectedStudentPeriodBlocks,
                calendarEvents = state.calendarEvents,
                terms = state.terms,
                appSettings = state.appSettings,
                presenceNowMillis = state.presenceNowMillis,
                onAvatarClick = { previewAvatarStudent = student },
                onEditProfile = { editStudentProfile = student },
                onToggleGuestObserver = { isGuest ->
                    viewModel.toggleStudentGuestObserver(student, isGuest)
                },
                onLogOrOverrideDate = { dateKey ->
                    overrideDateKey = dateKey
                    overrideStudent = student
                },
                onDeleteDateAttendance = { dateKey ->
                    viewModel.deleteStudentAttendance(student, dateKey)
                },
                onDeletePeriodBlock = { periodId ->
                    viewModel.deleteStudentPeriodBlock(student, periodId)
                },
                onRemoveAndBlockStudent = { reason ->
                    viewModel.removeAndBlockStudent(student, reason)
                }
            )
        }
    }

    if (showNoticeDialog) {
        NoticeEditorDialog(
            editingNotice = editingNotice,
            onDismiss = { showNoticeDialog = false },
            onSave = { id, title, msg, cat, pinned, targetType, targetVal, evDate, urgent, scheduledAtMillis ->
                viewModel.saveNotice(id, title, msg, cat, pinned, targetType, targetVal, evDate, urgent, scheduledAtMillis) {
                    showNoticeDialog = false
                }
            }
        )
    }

    overrideStudent?.let { st ->
        AdminOverrideAttendanceDialog(
            student = st,
            initialDateKey = overrideDateKey,
            onDismiss = { overrideStudent = null },
            onSave = { dk, status, note ->
                viewModel.overrideStudentAttendance(st, dk, status, note) {
                    overrideStudent = null
                }
            }
        )
    }

    editStudentProfile?.let { st ->
        AdminEditStudentDialog(
            student = st,
            onDismiss = { editStudentProfile = null },
            onSave = { cls, stream, pPres, pAbs ->
                viewModel.updateStudentBalanceAndClass(st, cls, stream, pPres, pAbs) {
                    editStudentProfile = null
                }
            }
        )
    }

    if (showAddEventDialog) {
        AdminCalendarEventEditorDialog(
            initialDateKey = editingCalendarEvent?.dateKey ?: todayKey,
            editingEvent = editingCalendarEvent,
            students = state.students,
            catalog = state.academicCatalog,
            isBusy = state.isBusy,
            onDismiss = {
                showAddEventDialog = false
                editingCalendarEvent = null
            },
            onSave = { eventId, title, dk, type, desc, targetAll, grades, streams, classes, userIds, summary, notify ->
                viewModel.saveCalendarEvent(
                    eventId = eventId,
                    title = title,
                    dateKey = dk,
                    eventType = type,
                    description = desc,
                    targetAll = targetAll,
                    targetGrades = grades,
                    targetStreams = streams,
                    targetClasses = classes,
                    targetUserIds = userIds,
                    audienceSummary = summary,
                    alsoSendNotification = notify
                ) {
                    showAddEventDialog = false
                    editingCalendarEvent = null
                }
            }
        )
    }

    // Full-size Student Profile Photo Viewer for Admin
    previewAvatarStudent?.let { st ->
        Dialog(onDismissRequest = { previewAvatarStudent = null }) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_student_photo_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = st.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${st.className} • ${st.alStream}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { previewAvatarStudent = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close student photo")
                        }
                    }

                    ProfileAvatar(
                        customAvatarBase64 = st.customAvatarBase64,
                        googlePhotoUrl = st.photoUrl,
                        displayName = st.fullName,
                        size = 180.dp
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = when {
                                !st.customAvatarBase64.isNullOrBlank() -> "Custom Uploaded Photo"
                                !st.photoUrl.isNullOrBlank() -> "Google Account Photo"
                                else -> "Default Initials"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    TextButton(onClick = { previewAvatarStudent = null }) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
