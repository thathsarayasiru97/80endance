package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceStatus
import com.example.data.DateUtils
import com.example.data.Notice
import com.example.ui.components.AnimatedEligibilityGauge
import com.example.ui.components.NoticeBoardSheet
import com.example.ui.components.PinnedNoticeSlideshow
import com.example.ui.components.StreakAndWeeklyCard
import com.example.ui.components.SyncStatusPill
import com.example.ui.theme.LocalAttendanceColors
import com.example.viewmodel.DashboardScopeMode
import com.example.viewmodel.StudentAppState

@Composable
fun HomeScreen(
    state: StudentAppState,
    scopeMode: DashboardScopeMode,
    onScopeModeChange: (DashboardScopeMode) -> Unit,
    onOpenCheckIn: (String) -> Unit,
    onSyncClick: () -> Unit
) {
    val profile = state.profile
    val semantic = LocalAttendanceColors.current
    val activeStats = if (scopeMode == DashboardScopeMode.CURRENT_TERM) {
        state.currentTermStats
    } else {
        state.overallStats
    }

    var showNoticeSheet by remember { mutableStateOf(false) }
    var focusedNotice by remember { mutableStateOf<Notice?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hey, ${profile?.greetingName ?: "Student"}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = listOfNotNull(
                                    profile?.className?.takeIf { it.isNotBlank() },
                                    profile?.alStream?.takeIf { it.isNotBlank() }
                                ).joinToString(" • ").ifEmpty { "Royal College, Colombo" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SyncStatusPill(
                            syncStatus = state.syncStatus,
                            onClick = onSyncClick
                        )
                    }

                    // A/L Exam Countdown & Notices Chip Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val examYear = profile?.alExamYear ?: state.appSettings.currentAcademicYear
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50)
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

                        if (state.visibleNotices.isNotEmpty() && state.pinnedNotices.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable { showNoticeSheet = true }
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
                                    selected = scopeMode == DashboardScopeMode.CURRENT_TERM,
                                    onClick = { onScopeModeChange(DashboardScopeMode.CURRENT_TERM) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                ) {
                                    Text(state.activeTerm.name.substringBefore("—").trim().ifEmpty { "Current Term" })
                                }
                                SegmentedButton(
                                    selected = scopeMode == DashboardScopeMode.OVERALL_AL,
                                    onClick = { onScopeModeChange(DashboardScopeMode.OVERALL_AL) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) {
                                    Text("Overall (G12 & 13)")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            AnimatedEligibilityGauge(stats = activeStats)

                            Spacer(modifier = Modifier.height(12.dp))

                            // Actionable Safe-Skip / Recovery Forecast Banner
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

            // 4. Quick Today Check-In Card
            item {
                val todayKey = DateUtils.todayKey()
                val todayRec = state.todayRecord
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
                            Text(
                                text = when {
                                    todayRec != null -> "Today: ${todayRec.statusEnum.label}"
                                    !state.todayIsSchoolDay -> "No school scheduled today"
                                    else -> "Did you attend school today?"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (todayRec?.note?.isNotBlank() == true) {
                                Text(
                                    text = todayRec.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { onOpenCheckIn(todayKey) },
                            shape = RoundedCornerShape(14.dp),
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

            // 5. Personal Attendance Streak & Weekly Summary Card
            item {
                StreakAndWeeklyCard(
                    streak = state.currentStreak,
                    weeklySummary = state.weeklySummary,
                    onDayClick = { dateKey -> onOpenCheckIn(dateKey) }
                )
            }

            // 6. Next Sri Lankan Poya Day / Holiday Card
            state.nextPoyaOrHoliday?.let { nextHoliday ->
                item {
                    val daysAway = DateUtils.daysBetween(DateUtils.todayKey(), nextHoliday.dateKey)
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

            item { Spacer(modifier = Modifier.height(16.dp)) }
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
}
