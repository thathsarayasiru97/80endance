package com.example.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.HistoryToggleOff
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.clip
import com.example.data.AttendanceCalculator
import com.example.data.AttendanceStatus
import com.example.data.CelebrationEvent
import com.example.data.DateUtils
import com.example.data.NextDayPollSummary
import com.example.data.NextDayPollWindow
import kotlin.math.roundToInt
import com.example.data.Notice
import com.example.ui.components.AlJourneyTimelineSheet
import com.example.ui.components.AnimatedAlCountdownPill
import com.example.ui.components.AnimatedEligibilityGauge
import com.example.ui.components.CelebrationOverlay
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.NoticeBoardSheet
import com.example.ui.components.PinnedNoticeSlideshow
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ProfilePhotoDialog
import com.example.ui.components.AdminOnlineNowPill
import com.example.ui.components.AdminOnlineUsersSheet
import com.example.ui.components.AdminRegisteredMembersSheet
import com.example.ui.components.BroadcastAlertBanner
import com.example.ui.components.CommunityAccountCountPill
import com.example.ui.components.DevelopedByAtayaFooter
import com.example.ui.components.HOME_HEADER_PILL_HEIGHT
import com.example.ui.components.LiquidGlassSegmentOption
import com.example.ui.components.ResolvedFeedbackBanner
import com.example.ui.components.RollingCounterText
import com.example.ui.components.SlidableLiquidGlassSegmentedBar
import com.example.ui.components.StaggeredReveal
import com.example.ui.components.StreakAndWeeklyCard
import com.example.ui.components.SwipeCatchUpDeckDialog
import com.example.ui.components.SyncStatusPill
import com.example.ui.components.bouncyClickable
import com.example.ui.reports.ParentSummaryPreviewDialog
import com.example.ui.theme.LocalAttendanceColors
import com.example.viewmodel.DashboardScopeMode
import com.example.viewmodel.StudentAppState
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: StudentAppState,
    scopeMode: DashboardScopeMode,
    onScopeModeChange: (DashboardScopeMode) -> Unit,
    onOpenCheckIn: (String) -> Unit,
    onSyncClick: () -> Unit,
    onQuickLogDate: (String, AttendanceStatus) -> Unit = { _, _ -> },
    onUndoSwipeDate: (String) -> Unit = {},
    onVoteNextDay: (Boolean) -> Unit = {},
    onSaveCustomAvatar: (String?) -> Unit = {},
    onDismissBroadcast: (String) -> Unit = {},
    onDismissResolvedFeedback: (String) -> Unit = {},
    celebrationEvent: CelebrationEvent? = null,
    onDismissCelebration: () -> Unit = {},
    onMarkSwipeDeckAutoOffered: () -> Unit = {},
    bottomBarPadding: Dp = 24.dp
) {
    val profile = state.profile
    val semantic = LocalAttendanceColors.current
    val activeStats = when (scopeMode) {
        DashboardScopeMode.CURRENT_MONTH -> state.currentMonthStats
        DashboardScopeMode.CURRENT_TERM -> state.currentTermStats
        DashboardScopeMode.OVERALL_AL -> state.overallStats
    }
    val activeScopeCaption = when (scopeMode) {
        DashboardScopeMode.CURRENT_MONTH -> state.currentMonthLabel
        DashboardScopeMode.CURRENT_TERM -> state.activeTerm.name
        DashboardScopeMode.OVERALL_AL -> "Overall (Grade 12 & 13)"
    }

    var showNoticeSheet by remember { mutableStateOf(false) }
    var focusedNotice by remember { mutableStateOf<Notice?>(null) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showAlTimelineSheet by remember { mutableStateOf(false) }
    var showSwipeCatchUpDeck by remember { mutableStateOf(false) }
    var privacyBlurred by remember { mutableStateOf(false) }
    var showParentSummary by remember { mutableStateOf(false) }
    var showAdminOnlineUsersSheet by remember { mutableStateOf(false) }
    var showAdminMembersSheet by remember { mutableStateOf(false) }
    var dismissedBatchMilestoneDays by rememberSaveable { mutableStateOf<Int?>(null) }
    var visible by remember { mutableStateOf(false) }
    var heroCountdownExpanded by remember { mutableStateOf(state.daysUntilAlExam > 0) }

    LaunchedEffect(Unit) {
        visible = true
        if (state.daysUntilAlExam > 0) {
            delay(2000L)
            heroCountdownExpanded = false
        }
    }

    // Auto-offer Swipe-to-Catch-Up stack strictly once per app launch (never on tab switches)
    LaunchedEffect(state.missedSchoolDays.size, state.shouldAutoShowCheckIn, state.autoOfferedSwipeDeckThisLaunch) {
        if (!state.autoOfferedSwipeDeckThisLaunch &&
            state.missedSchoolDays.size >= 3 &&
            !state.shouldAutoShowCheckIn
        ) {
            onMarkSwipeDeckAutoOffered()
            delay(900L)
            showSwipeCatchUpDeck = true
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .testTag("home_lazy_column"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = bottomBarPadding
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Greeting Header + Dynamic Context Banner + A/L Countdown + Sync Status
            item {
                val dynamicContext = remember(
                    profile?.greetingName,
                    state.todayIsWeekend,
                    state.todayNonSchoolEvent,
                    state.todayRecord,
                    state.currentStreak,
                    state.weeklySummary,
                    state.nextPoyaOrHoliday,
                    activeStats
                ) {
                    AttendanceCalculator.buildDynamicGreetingAndContext(
                        greetingName = profile?.greetingName ?: "Student",
                        todayKey = DateUtils.todayKey(),
                        hourOfDay = DateUtils.currentHourMinute().first,
                        todayIsWeekend = state.todayIsWeekend,
                        todayNonSchoolEvent = state.todayNonSchoolEvent,
                        todayRecord = state.todayRecord,
                        currentStreak = state.currentStreak,
                        weeklySummary = state.weeklySummary,
                        nextHoliday = state.nextPoyaOrHoliday,
                        activeStats = activeStats
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            ProfileAvatar(
                                customAvatarBase64 = profile?.customAvatarBase64,
                                googlePhotoUrl = state.googlePhotoUrl,
                                displayName = profile?.fullName ?: "Student",
                                size = 48.dp,
                                onClick = { showPhotoDialog = true },
                                modifier = Modifier.testTag("home_profile_avatar")
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dynamicContext.greetingTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("home_dynamic_greeting_title")
                                )
                                Text(
                                    text = listOfNotNull(
                                        profile?.className?.takeIf { it.isNotBlank() },
                                        profile?.alStream?.takeIf { it.isNotBlank() }
                                    ).joinToString(" • ").ifEmpty { "Royal College, Colombo" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        SyncStatusPill(
                            syncStatus = state.syncStatus,
                            onClick = onSyncClick
                        )
                    }

                    // Subtle Dynamic Context Banner ("Good afternoon · 4 school days left this week", Poya tomorrow, Streak alert)
                    val contextBg = when {
                        dynamicContext.isStreakAlert -> semantic.poyaContainer.copy(alpha = 0.72f)
                        dynamicContext.isHolidayTomorrow || dynamicContext.isHolidayToday -> semantic.poyaContainer.copy(alpha = 0.65f)
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    }
                    val contextFg = when {
                        dynamicContext.isStreakAlert -> semantic.onPoyaContainer
                        dynamicContext.isHolidayTomorrow || dynamicContext.isHolidayToday -> semantic.onPoyaContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        color = contextBg,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_dynamic_context_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    dynamicContext.isStreakAlert -> Icons.Rounded.LocalFireDepartment
                                    dynamicContext.isHolidayTomorrow || dynamicContext.isHolidayToday -> Icons.Rounded.NightsStay
                                    else -> Icons.Rounded.AutoAwesome
                                },
                                contentDescription = null,
                                tint = if (dynamicContext.isStreakAlert) semantic.poya else contextFg,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = dynamicContext.contextBannerText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = contextFg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val examYear = profile?.alExamYear ?: state.appSettings.currentAcademicYear
                    val batchMilestone = remember(state.daysUntilAlExam, examYear) {
                        AttendanceCalculator.resolveBatchCountdownMilestone(
                            daysUntilAlExam = state.daysUntilAlExam,
                            examYear = examYear
                        )
                    }

                    // Shared Celebratory Batch Countdown Milestone Banner (300d, 200d, 100d, 50d, 30d to A/Ls)
                    if (batchMilestone != null && dismissedBatchMilestoneDays != batchMilestone.milestoneDays) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bouncyClickable(pressedScale = 0.98f) { showAlTimelineSheet = true }
                                .testTag("home_batch_milestone_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = "Batch milestone",
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = batchMilestone.badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = batchMilestone.headline,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = batchMilestone.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                                    )
                                }
                                IconButton(
                                    onClick = { dismissedBatchMilestoneDays = batchMilestone.milestoneDays }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Dismiss milestone banner",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Hero "Days Left for A/L" First-Glance Spotlight — starts large, then docks into compact chip
                    AnimatedVisibility(
                        visible = heroCountdownExpanded && state.daysUntilAlExam > 0,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(tween(260)) + scaleIn(initialScale = 0.92f),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeOut(tween(300)) + scaleOut(targetScale = 0.84f, animationSpec = tween(280))
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bouncyClickable(pressedScale = 0.97f) {
                                    heroCountdownExpanded = false
                                    showAlTimelineSheet = true
                                }
                                .testTag("home_al_countdown_hero")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 20.dp, vertical = 18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.Timer,
                                                contentDescription = "A/L Exam Countdown",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "$examYear GCE A/L COUNTDOWN",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Every school day counts toward your goal.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        RollingCounterText(
                                            count = state.daysUntilAlExam,
                                            style = MaterialTheme.typography.displayMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "DAYS LEFT",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // A/L Exam Countdown, Pulsing Green Community Accounts Counter & Notices Chip Row
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedVisibility(
                            visible = !heroCountdownExpanded || state.daysUntilAlExam <= 0,
                            enter = fadeIn(tween(260)) + scaleIn(
                                initialScale = 0.72f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ),
                            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.82f)
                        ) {
                            AnimatedAlCountdownPill(
                                daysUntilAlExam = state.daysUntilAlExam,
                                examYear = examYear,
                                onClick = { showAlTimelineSheet = true },
                                modifier = Modifier.testTag("home_al_countdown_chip")
                            )
                        }

                        if (state.totalAccounts > 0) {
                            val canViewMembers = state.adminOnlineNowCount != null || state.adminAllStudents.isNotEmpty()
                            CommunityAccountCountPill(
                                totalAccounts = state.totalAccounts,
                                onClick = if (canViewMembers) ({ showAdminMembersSheet = true }) else null
                            )
                        }

                        state.adminOnlineNowCount?.let { onlineNow ->
                            AdminOnlineNowPill(
                                onlineCount = onlineNow,
                                onClick = { showAdminOnlineUsersSheet = true }
                            )
                        }

                        if (state.visibleNotices.isNotEmpty() && state.pinnedNotices.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .height(HOME_HEADER_PILL_HEIGHT)
                                    .bouncyClickable(pressedScale = 0.93f) { showNoticeSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Campaign,
                                        contentDescription = "Notices",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${state.visibleNotices.size} Notices",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1b. Pinned Updates Slideshow (always on top above new announcements)
            if (state.pinnedNotices.isNotEmpty()) {
                item(key = "pinned_notices_slideshow") {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { -20 }
                    ) {
                        PinnedNoticeSlideshow(
                            pinnedNotices = state.pinnedNotices,
                            totalNoticeCount = state.visibleNotices.size,
                            onNoticeClick = { focusedNotice = it },
                            onViewAllClick = { showNoticeSheet = true }
                        )
                    }
                }
            }

            // 1c. Unread Admin Custom Broadcast Notification Banner (below Pinned Updates)
            val activeBroadcast = state.unreadBroadcasts.firstOrNull()
            if (activeBroadcast != null) {
                item(key = "broadcast_${activeBroadcast.id}") {
                    BroadcastAlertBanner(
                        broadcast = activeBroadcast,
                        remainingCount = state.unreadBroadcasts.size,
                        onDismiss = { onDismissBroadcast(activeBroadcast.id) }
                    )
                }
            }

            // 1d. In-App-Only Resolved Bug/Suggestion Alert Banner
            val activeResolved = state.resolvedFeedbackAlerts.firstOrNull()
            if (activeResolved != null) {
                item(key = "resolved_feedback_${activeResolved.id}") {
                    ResolvedFeedbackBanner(
                        report = activeResolved,
                        remainingCount = state.resolvedFeedbackAlerts.size,
                        onDismiss = { onDismissResolvedFeedback(activeResolved.id) }
                    )
                }
            }

            // 3. Hero 80% Eligibility Card with Term vs Overall (G12 & G13) Toggle
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically(spring(stiffness = Spring.StiffnessLow)) { 30 }
                ) {
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            val scopeOrder = remember {
                                listOf(
                                    DashboardScopeMode.CURRENT_MONTH,
                                    DashboardScopeMode.OVERALL_AL,
                                    DashboardScopeMode.CURRENT_TERM
                                )
                            }
                            val scopeOptions = remember {
                                listOf(
                                    LiquidGlassSegmentOption(label = "Month", testTag = "scope_tab_month"),
                                    LiquidGlassSegmentOption(label = "Overall", testTag = "scope_tab_overall"),
                                    LiquidGlassSegmentOption(label = "Term", testTag = "scope_tab_term")
                                )
                            }
                            SlidableLiquidGlassSegmentedBar(
                                options = scopeOptions,
                                selectedIndex = scopeOrder.indexOf(scopeMode).coerceAtLeast(0),
                                onOptionSelected = { idx ->
                                    scopeOrder.getOrNull(idx)?.let(onScopeModeChange)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scope_mode_toggle")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            AnimatedContent(
                                targetState = activeScopeCaption,
                                transitionSpec = {
                                    (fadeIn(tween(200)) + slideInVertically { -10 })
                                        .togetherWith(fadeOut(tween(150)) + slideOutVertically { 10 })
                                },
                                label = "scope_caption_anim"
                            ) { caption ->
                                Text(
                                    text = caption,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            AnimatedEligibilityGauge(
                                stats = activeStats,
                                privacyBlurred = privacyBlurred,
                                onDoubleTapTogglePrivacy = { privacyBlurred = !privacyBlurred }
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = if (privacyBlurred) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .clickable { privacyBlurred = !privacyBlurred }
                                        .testTag("privacy_blur_toggle")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (privacyBlurred) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = "Toggle privacy blur",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (privacyBlurred) "Blur On • Reveal" else "Double-tap to blur",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .clickable { showParentSummary = true }
                                        .testTag("home_parent_summary_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FamilyRestroom,
                                            contentDescription = "Open parent-friendly summary card",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Parent Card",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Actionable Safe-Skip / Recovery Forecast Banner
                            val forecastBg = when {
                                privacyBlurred -> MaterialTheme.colorScheme.surfaceContainerLow
                                activeStats.eligible -> semantic.presentContainer.copy(alpha = 0.7f)
                                else -> semantic.absentContainer.copy(alpha = 0.85f)
                            }
                            val forecastFg = when {
                                privacyBlurred -> MaterialTheme.colorScheme.onSurfaceVariant
                                activeStats.eligible -> semantic.onPresentContainer
                                else -> semantic.onAbsentContainer
                            }
                            val forecastText = when {
                                privacyBlurred -> "Privacy Mode active — % and absent count are hidden. Double-tap the ring or tap Reveal above."
                                activeStats.totalCountedDays == 0 ->
                                    "Log your first school day below to start tracking your ${activeStats.thresholdPercent}% eligibility."
                                activeStats.eligible && activeStats.safeSkipsRemaining > 0 ->
                                    "You're on track! Safe buffer: up to ${activeStats.safeSkipsRemaining} more skip${if (activeStats.safeSkipsRemaining == 1) "" else "s"} without dropping below ${activeStats.thresholdPercent}%."
                                activeStats.eligible ->
                                    "Right on the ${activeStats.thresholdPercent}% line — attend the next school day to build a safe buffer."
                                else ->
                                    "Below ${activeStats.thresholdPercent}% — attend the next ${activeStats.daysNeededToRecover} consecutive school day${if (activeStats.daysNeededToRecover == 1) "" else "s"} to recover eligibility."
                            }
                            AnimatedContent(
                                targetState = Triple(forecastBg, forecastFg, forecastText),
                                transitionSpec = {
                                    (fadeIn(tween(240)) + slideInVertically { 12 })
                                        .togetherWith(fadeOut(tween(180)) + slideOutVertically { -12 })
                                },
                                label = "forecast_banner_anim",
                                modifier = Modifier.animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            ) { (bg, fg, copy) ->
                                Surface(
                                    color = bg,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = copy,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = fg,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3b. Danger-Zone Smart Nudge Banner (only shown when sitting right on the 80% edge)
            if (state.isInDangerZone && !privacyBlurred) {
                item(key = "danger_zone_nudge_banner") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = semantic.medicalContainer.copy(alpha = 0.85f),
                        border = BorderStroke(1.2.dp, semantic.medical.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_danger_zone_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = "80% safety edge warning",
                                tint = semantic.medical,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "80% Edge Heads-Up",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = semantic.onMedicalContainer
                                )
                                Text(
                                    text = if (state.currentTermStats.safeSkipsRemaining <= 0) {
                                        "Heads up — 1 more absence drops you below ${state.currentTermStats.thresholdPercent}%."
                                    } else {
                                        "Careful — only ${state.currentTermStats.safeSkipsRemaining} safe skip left before dropping below ${state.currentTermStats.thresholdPercent}%."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semantic.onMedicalContainer.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            // 3c. Next-Day School Poll Card (only visible 8:00 PM – 5:00 AM when enabled by admin)
            if (state.appSettings.nextDayPollEnabled && state.pollWindow.isOpen) {
                item(key = "home_next_day_poll_card") {
                    NextDaySchoolPollCard(
                        window = state.pollWindow,
                        summary = state.nextDayPollSummary,
                        myClassName = profile?.className,
                        onVote = onVoteNextDay
                    )
                }
            }

            // 4. Quick Today Card — Weekend Relaxation, Admin/Poya No School Day, or Check-In
            item {
                val todayKey = DateUtils.todayKey()
                val todayRec = state.todayRecord
                val nonSchoolEv = state.todayNonSchoolEvent

                StaggeredReveal(index = 2) {
                when {
                    state.todayIsWeekend -> {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_weekend_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Weekend,
                                            contentDescription = "Weekend",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = DateUtils.formatReadableShort(todayKey),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Enjoy your weekend!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "No school today — rest up and recharge for the week ahead.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    nonSchoolEv != null && todayRec == null -> {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = semantic.poyaContainer.copy(alpha = 0.6f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_no_school_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = semantic.poya.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.EventBusy,
                                            contentDescription = "No school today",
                                            tint = semantic.onPoyaContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${DateUtils.formatReadableShort(todayKey)} • No School",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = semantic.onPoyaContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = nonSchoolEv.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = semantic.onPoyaContainer
                                    )
                                    Text(
                                        text = "Scheduled off day — won't affect your 80% attendance.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = semantic.onPoyaContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    state.isDateLocked(todayKey) -> {
                        val lockingPeriod = state.lockingPeriodFor(todayKey)
                        val lockRangeLabel = when {
                            lockingPeriod != null ->
                                "${DateUtils.formatReadableShort(lockingPeriod.startDate)} → ${DateUtils.formatReadableShort(lockingPeriod.endDate)}"
                            profile?.baselineStartDate != null && profile.baselineEndDate != null ->
                                "${DateUtils.formatReadableShort(profile.baselineStartDate)} → ${DateUtils.formatReadableShort(profile.baselineEndDate)}"
                            else -> "Locked Period"
                        }
                        val lockSubtitle = if (lockingPeriod != null) {
                            val labelPrefix = lockingPeriod.label?.takeIf { it.isNotBlank() }?.let { "$it: " }.orEmpty()
                            "${labelPrefix}Today is covered by your locked period ($lockRangeLabel • ${lockingPeriod.presentDays}P / ${lockingPeriod.absentDays}A). Manage or unlock in Calendar."
                        } else {
                            "Today is covered by your imported percentage ($lockRangeLabel). Manage or unlock in Settings."
                        }
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_locked_today_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = "Locked attendance period",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${DateUtils.formatReadableShort(todayKey)} • Locked Period",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (lockingPeriod != null) {
                                            "Included in Locked Time Period"
                                        } else {
                                            "Included in Imported Attendance"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = lockSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (todayRec == null) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLowest
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = DateUtils.formatReadableShort(todayKey),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            AnimatedVisibility(
                                                visible = todayRec?.statusEnum == AttendanceStatus.PRESENT ||
                                                    todayRec?.statusEnum == AttendanceStatus.SCHOOL_EVENT ||
                                                    todayRec?.statusEnum == AttendanceStatus.MEDICAL,
                                                enter = fadeIn() + scaleIn(
                                                    initialScale = 0.5f,
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = "Logged present",
                                                    tint = semantic.present,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Text(
                                                text = when {
                                                    todayRec != null -> "Today: ${todayRec.statusEnum.label}"
                                                    else -> "Did you attend school today?"
                                                },
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (todayRec?.note?.isNotBlank() == true) {
                                            Text(
                                                text = todayRec.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    LiquidGlassButton(
                                        onClick = { onOpenCheckIn(todayKey) },
                                        tint = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        minHeight = 48.dp,
                                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                        modifier = Modifier.testTag("home_checkin_button")
                                    ) {
                                        Icon(
                                            imageVector = if (todayRec == null) Icons.Rounded.AddTask else Icons.Rounded.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (todayRec == null) "Check In" else "Edit")
                                    }
                                }

                                // Slidable Thin-Film Liquid-Glass Today Status Switcher
                                val quickStatuses = remember {
                                    listOf(
                                        AttendanceStatus.PRESENT,
                                        AttendanceStatus.MEDICAL,
                                        AttendanceStatus.SCHOOL_EVENT,
                                        AttendanceStatus.ABSENT
                                    )
                                }
                                val quickStatusOptions = remember(semantic) {
                                    listOf(
                                        LiquidGlassSegmentOption(
                                            label = "Present",
                                            testTag = "home_today_status_present",
                                            tintColor = semantic.present
                                        ),
                                        LiquidGlassSegmentOption(
                                            label = "Medical",
                                            testTag = "home_today_status_medical",
                                            tintColor = semantic.medical
                                        ),
                                        LiquidGlassSegmentOption(
                                            label = "Event",
                                            testTag = "home_today_status_event",
                                            tintColor = semantic.schoolEvent
                                        ),
                                        LiquidGlassSegmentOption(
                                            label = "Absent",
                                            testTag = "home_today_status_absent",
                                            tintColor = semantic.absent
                                        )
                                    )
                                }
                                val currentTodayIdx = quickStatuses.indexOf(todayRec?.statusEnum ?: AttendanceStatus.PRESENT)
                                    .coerceAtLeast(0)
                                SlidableLiquidGlassSegmentedBar(
                                    options = quickStatusOptions,
                                    selectedIndex = currentTodayIdx,
                                    onOptionSelected = { idx ->
                                        quickStatuses.getOrNull(idx)?.let { status ->
                                            onQuickLogDate(todayKey, status)
                                        }
                                    },
                                    barHeight = 42.dp,
                                    defaultTint = semantic.present,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("home_today_status_slider")
                                )
                            }
                        }
                    }
                }
                }
            }

            // 4b. Missed-Day Catch-Up Banner (1-tap catch-up for unlogged recent school weekdays)
            if (state.missedSchoolDays.isNotEmpty()) {
                item {
                    StaggeredReveal(index = 3) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_catchup_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = semantic.medicalContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.HistoryToggleOff,
                                            contentDescription = "Missed day catch-up",
                                            tint = semantic.onMedicalContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Quick Catch-Up (${state.missedSchoolDays.size} unlogged)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Swipe through in 5s or tap P, E, A below",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .clickable { showSwipeCatchUpDeck = true }
                                        .testTag("home_open_swipe_deck_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Swipe,
                                            contentDescription = "Swipe catch-up deck",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Swipe",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            state.missedSchoolDays.forEach { dk ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onOpenCheckIn(dk) }
                                        ) {
                                            Text(
                                                text = DateUtils.formatReadableShort(dk),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Tap for details",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            CatchUpActionChip(
                                                label = "P",
                                                bg = semantic.presentContainer,
                                                fg = semantic.onPresentContainer,
                                                testTag = "catchup_present_$dk",
                                                onClick = { onQuickLogDate(dk, AttendanceStatus.PRESENT) }
                                            )
                                            CatchUpActionChip(
                                                label = "E",
                                                bg = semantic.schoolEventContainer,
                                                fg = semantic.onSchoolEventContainer,
                                                testTag = "catchup_excused_$dk",
                                                onClick = { onQuickLogDate(dk, AttendanceStatus.SCHOOL_EVENT) }
                                            )
                                            CatchUpActionChip(
                                                label = "A",
                                                bg = semantic.absentContainer,
                                                fg = semantic.onAbsentContainer,
                                                testTag = "catchup_absent_$dk",
                                                onClick = { onQuickLogDate(dk, AttendanceStatus.ABSENT) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }

            // 5. Personal Attendance Streak & Weekly Summary Card
            item {
                StaggeredReveal(index = 4) {
                    StreakAndWeeklyCard(
                        streak = state.currentStreak,
                        weeklySummary = state.weeklySummary,
                        onDayClick = { dateKey -> onOpenCheckIn(dateKey) }
                    )
                }
            }

            // 6. Next Sri Lankan Poya Day / Holiday Card
            state.nextPoyaOrHoliday?.let { nextHoliday ->
                item {
                    val daysAway = DateUtils.daysBetween(DateUtils.todayKey(), nextHoliday.dateKey)
                    StaggeredReveal(index = 5) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = semantic.poyaContainer.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = semantic.poya.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (nextHoliday.eventType == "poya") Icons.Rounded.NightsStay else Icons.Rounded.Event,
                                        contentDescription = nextHoliday.title,
                                        tint = semantic.onPoyaContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Next Sri Lankan Holiday",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semantic.onPoyaContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = nextHoliday.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = semantic.onPoyaContainer
                                )
                                Text(
                                    text = "${DateUtils.formatReadableShort(nextHoliday.dateKey)} • ${
                                        when (daysAway) {
                                            0 -> "Today"
                                            1 -> "Tomorrow"
                                            else -> "In $daysAway days"
                                        }
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semantic.onPoyaContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                    }
                }
            }

            item {
                DevelopedByAtayaFooter(
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        // Celebratory confetti + banner overlay for Perfect Week (5/5), Streaks, and 80% Recovery
        CelebrationOverlay(
            event = celebrationEvent,
            onDismiss = onDismissCelebration,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 600.dp)
        )
    }

    if (showAlTimelineSheet) {
        val examYear = profile?.alExamYear ?: state.appSettings.currentAcademicYear
        val timeline = remember(examYear, state.appSettings, state.terms) {
            AttendanceCalculator.calculateAlJourneyTimeline(
                examYear = examYear,
                settings = state.appSettings,
                terms = state.terms
            )
        }
        AlJourneyTimelineSheet(
            timeline = timeline,
            onDismiss = { showAlTimelineSheet = false }
        )
    }

    if (showSwipeCatchUpDeck) {
        SwipeCatchUpDeckDialog(
            missedDays = state.missedSchoolDays,
            onLogDay = onQuickLogDate,
            onUndoLastDay = onUndoSwipeDate,
            onDismiss = { showSwipeCatchUpDeck = false }
        )
    }

    if (showNoticeSheet) {
        NoticeBoardSheet(
            notices = state.visibleNotices,
            onDismiss = { showNoticeSheet = false }
        )
    }

    if (showParentSummary) {
        ParentSummaryPreviewDialog(
            profile = profile,
            termName = state.activeTerm.name,
            stats = state.currentTermStats,
            overallStats = state.overallStats,
            onDismiss = { showParentSummary = false }
        )
    }

    if (showAdminOnlineUsersSheet && state.adminOnlineNowCount != null) {
        AdminOnlineUsersSheet(
            onlineUsers = state.adminOnlineUsers,
            onDismiss = { showAdminOnlineUsersSheet = false }
        )
    }

    if (showAdminMembersSheet && (state.adminOnlineNowCount != null || state.adminAllStudents.isNotEmpty())) {
        AdminRegisteredMembersSheet(
            members = state.adminAllStudents,
            onDismiss = { showAdminMembersSheet = false }
        )
    }

    focusedNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { focusedNotice = null },
            icon = { Icon(Icons.Rounded.Campaign, contentDescription = null) },
            title = { Text(notice.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = buildString {
                            append(notice.categoryEnum.label)
                            if (!notice.eventDate.isNullOrBlank()) {
                                append(" • ${DateUtils.formatReadableShort(notice.eventDate)}")
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = notice.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { focusedNotice = null }) { Text("Got it") }
            }
        )
    }

    if (showPhotoDialog) {
        ProfilePhotoDialog(
            displayName = profile?.fullName ?: "Student",
            customAvatarBase64 = profile?.customAvatarBase64,
            googlePhotoUrl = state.googlePhotoUrl,
            onSaveCustomAvatar = onSaveCustomAvatar,
            onDismiss = { showPhotoDialog = false }
        )
    }
}

@Composable
private fun NextDaySchoolPollCard(
    window: NextDayPollWindow,
    summary: NextDayPollSummary,
    myClassName: String?,
    onVote: (Boolean) -> Unit
) {
    val semantic = LocalAttendanceColors.current
    val myVote = summary.myVoteGoing
    val comingRatio by animateFloatAsState(
        targetValue = if (summary.totalVotes > 0) (summary.goingPercent / 100.0).toFloat().coerceIn(0f, 1f) else 0.5f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "poll_coming_ratio"
    )
    val comingPctInt = summary.goingPercent.roundToInt()
    val notComingPctInt = (100 - comingPctInt).coerceIn(0, 100)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(
            width = 1.2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .testTag("home_next_day_poll_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.HowToVote,
                                contentDescription = "Next-day school poll",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (window.isTomorrowTarget) {
                                "Going to school tomorrow?"
                            } else {
                                "Going to school today?"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${DateUtils.formatReadableShort(window.targetDateKey)} • Closes at 5:00 AM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    if (summary.totalVotes == 0) {
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    } else {
                        RollingCounterText(
                            count = summary.totalVotes,
                            suffix = " voted",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val yesSelected = myVote == true
                val noSelected = myVote == false
                val yesBg by animateColorAsState(
                    targetValue = if (yesSelected) semantic.present else semantic.presentContainer.copy(alpha = 0.72f),
                    animationSpec = tween(240),
                    label = "poll_yes_bg"
                )
                val yesFg by animateColorAsState(
                    targetValue = if (yesSelected) MaterialTheme.colorScheme.onPrimary else semantic.onPresentContainer,
                    animationSpec = tween(240),
                    label = "poll_yes_fg"
                )
                val noBg by animateColorAsState(
                    targetValue = if (noSelected) semantic.absentContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    animationSpec = tween(240),
                    label = "poll_no_bg"
                )
                val noFg by animateColorAsState(
                    targetValue = if (noSelected) semantic.onAbsentContainer else MaterialTheme.colorScheme.onSurface,
                    animationSpec = tween(240),
                    label = "poll_no_fg"
                )
                val noBorder by animateColorAsState(
                    targetValue = if (noSelected) semantic.absent else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = tween(240),
                    label = "poll_no_border"
                )

                Button(
                    onClick = { onVote(true) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = yesBg,
                        contentColor = yesFg
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("poll_vote_yes_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (yesSelected) "Coming ✓" else "Yes, Coming",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = { onVote(false) },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = if (noSelected) 1.8.dp else 1.dp,
                        color = noBorder
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = noBg,
                        contentColor = noFg
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("poll_vote_no_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (noSelected) "Skipping ✓" else "No, Skipping",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("poll_live_analysis_bar")
            ) {
                if (summary.totalVotes > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Coming $comingPctInt% (${summary.goingCount})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = semantic.present
                        )
                        Text(
                            text = "Not Coming $notComingPctInt% (${summary.notGoingCount})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = semantic.absent
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(semantic.absent.copy(alpha = 0.25f))
                    ) {
                        if (comingRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(comingRatio.coerceAtLeast(0.001f))
                                    .height(10.dp)
                                    .background(semantic.present)
                            )
                        }
                        val notRatio = (1f - comingRatio)
                        if (notRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(notRatio.coerceAtLeast(0.001f))
                                    .height(10.dp)
                                    .background(semantic.absent)
                            )
                        }
                    }
                    val classSuffix = if (summary.myClassTotalCount > 0 && !myClassName.isNullOrBlank()) {
                        " • ${summary.myClassGoingCount}/${summary.myClassTotalCount} in $myClassName coming"
                    } else {
                        ""
                    }
                    Text(
                        text = "Switch your vote anytime before 5:00 AM$classSuffix",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Be the first to vote — live turnout breakdown unlocks right away and stays switchable until 5:00 AM.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CatchUpActionChip(
    label: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "catchup_chip_scale"
    )
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(width = 48.dp, height = 40.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}
