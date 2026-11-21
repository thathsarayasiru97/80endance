package com.example.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.HistoryToggleOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceStatus
import com.example.data.DateUtils
import com.example.data.Notice
import com.example.ui.components.AnimatedEligibilityGauge
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.NoticeBoardSheet
import com.example.ui.components.PinnedNoticeSlideshow
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ProfilePhotoDialog
import com.example.ui.components.AdminOnlineNowPill
import com.example.ui.components.BroadcastAlertBanner
import com.example.ui.components.CommunityAccountCountPill
import com.example.ui.components.DevelopedByAtayaFooter
import com.example.ui.components.ResolvedFeedbackBanner
import com.example.ui.components.RollingCounterText
import com.example.ui.components.StaggeredReveal
import com.example.ui.components.StreakAndWeeklyCard
import com.example.ui.components.SyncStatusPill
import com.example.ui.components.bouncyClickable
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
    onSaveCustomAvatar: (String?) -> Unit = {},
    onDismissBroadcast: (String) -> Unit = {},
    onDismissResolvedFeedback: (String) -> Unit = {},
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
    var visible by remember { mutableStateOf(false) }
    var heroCountdownExpanded by remember { mutableStateOf(state.daysUntilAlExam > 0) }

    LaunchedEffect(Unit) {
        visible = true
        if (state.daysUntilAlExam > 0) {
            delay(2000L)
            heroCountdownExpanded = false
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
            // 1. Greeting Header + A/L Countdown + Sync Status
            item {
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
                                    text = "Hey, ${profile?.greetingName ?: "Student"}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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

                    val examYear = profile?.alExamYear ?: state.appSettings.currentAcademicYear

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
                                .bouncyClickable(pressedScale = 0.97f) { heroCountdownExpanded = false }
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
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .bouncyClickable(
                                        enabled = state.daysUntilAlExam > 0,
                                        pressedScale = 0.93f
                                    ) {
                                        if (state.daysUntilAlExam > 0) heroCountdownExpanded = true
                                    }
                                    .testTag("home_al_countdown_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Timer,
                                        contentDescription = "A/L exam countdown",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (state.daysUntilAlExam > 0) {
                                            "$examYear A/L • ${state.daysUntilAlExam}d left"
                                        } else {
                                            "$examYear A/L Exam Period"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        if (state.totalAccounts > 0) {
                            CommunityAccountCountPill(totalAccounts = state.totalAccounts)
                        }

                        state.adminOnlineNowCount?.let { onlineNow ->
                            AdminOnlineNowPill(onlineCount = onlineNow)
                        }

                        if (state.visibleNotices.isNotEmpty() && state.pinnedNotices.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.bouncyClickable(pressedScale = 0.93f) { showNoticeSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Campaign,
                                        contentDescription = "Notices",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "${state.visibleNotices.size} Notices",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1b. Unread Admin Custom Broadcast Notification Banner
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

            // 1c. In-App-Only Resolved Bug/Suggestion Alert Banner
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

            // 2. Pinned Notice 5-Second Auto-Advancing Slideshow
            if (state.pinnedNotices.isNotEmpty()) {
                item {
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
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scope_mode_toggle")
                            ) {
                                SegmentedButton(
                                    selected = scopeMode == DashboardScopeMode.CURRENT_MONTH,
                                    onClick = { onScopeModeChange(DashboardScopeMode.CURRENT_MONTH) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                    modifier = Modifier.testTag("scope_tab_month")
                                ) {
                                    Text("Month", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                SegmentedButton(
                                    selected = scopeMode == DashboardScopeMode.CURRENT_TERM,
                                    onClick = { onScopeModeChange(DashboardScopeMode.CURRENT_TERM) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                    modifier = Modifier.testTag("scope_tab_term")
                                ) {
                                    Text("Term", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                SegmentedButton(
                                    selected = scopeMode == DashboardScopeMode.OVERALL_AL,
                                    onClick = { onScopeModeChange(DashboardScopeMode.OVERALL_AL) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                    modifier = Modifier.testTag("scope_tab_overall")
                                ) {
                                    Text("Overall", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

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

                            AnimatedEligibilityGauge(stats = activeStats)

                            Spacer(modifier = Modifier.height(12.dp))

                            // Actionable Safe-Skip / Recovery Forecast Banner
                            AnimatedContent(
                                targetState = Triple(scopeMode, activeStats.eligible, activeStats.totalCountedDays to activeStats.safeSkipsRemaining),
                                transitionSpec = {
                                    (fadeIn(tween(240)) + slideInVertically { 12 })
                                        .togetherWith(fadeOut(tween(180)) + slideOutVertically { -12 })
                                },
                                label = "forecast_banner_anim"
                            ) { _ ->
                                val forecastBg = if (activeStats.eligible) {
                                    semantic.presentContainer.copy(alpha = 0.7f)
                                } else {
                                    semantic.absentContainer.copy(alpha = 0.85f)
                                }
                                val forecastFg = if (activeStats.eligible) {
                                    semantic.onPresentContainer
                                } else {
                                    semantic.onAbsentContainer
                                }
                                val forecastText = when {
                                    activeStats.totalCountedDays == 0 ->
                                        "Log your first school day below to start tracking your ${activeStats.thresholdPercent}% eligibility."
                                    activeStats.eligible && activeStats.safeSkipsRemaining > 0 ->
                                        "You're on track! Safe buffer: up to ${activeStats.safeSkipsRemaining} more skip${if (activeStats.safeSkipsRemaining == 1) "" else "s"} without dropping below ${activeStats.thresholdPercent}%."
                                    activeStats.eligible ->
                                        "Right on the ${activeStats.thresholdPercent}% line — attend the next school day to build a safe buffer."
                                    else ->
                                        "Below ${activeStats.thresholdPercent}% — attend the next ${activeStats.daysNeededToRecover} consecutive school day${if (activeStats.daysNeededToRecover == 1) "" else "s"} to recover eligibility."
                                }

                                Surface(
                                    color = forecastBg,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = forecastText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = forecastFg,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }
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

                    profile?.isDateInLockedBaseline(todayKey) == true -> {
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
                                            contentDescription = "Locked imported attendance period",
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
                                        text = "Included in Imported Attendance",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Today is covered by your imported percentage (${DateUtils.formatReadableShort(profile.baselineStartDate!!)} → ${DateUtils.formatReadableShort(profile.baselineEndDate!!)}). Manage or unlock in Settings.",
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
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
                                        text = "Tap P, E, or A to fill recent school days — or tap a date for Medical/notes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
    }

    if (showNoticeSheet) {
        NoticeBoardSheet(
            notices = state.visibleNotices,
            onDismiss = { showNoticeSheet = false }
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
