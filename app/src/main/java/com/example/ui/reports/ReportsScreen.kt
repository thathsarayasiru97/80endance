package com.example.ui.reports

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AcademicTerm
import com.example.data.AttendanceCalculator
import com.example.data.AttendanceStatus
import com.example.data.DateUtils
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassSegmentOption
import com.example.ui.components.RollingCounterText
import com.example.ui.components.SlidableLiquidGlassSegmentedBar
import com.example.ui.components.StaggeredReveal
import com.example.ui.theme.LocalAttendanceColors
import com.example.viewmodel.StudentAppState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    state: StudentAppState,
    onToggleCountExcused: (Boolean) -> Unit = {},
    bottomBarPadding: Dp = 24.dp
) {
    val context = LocalContext.current
    val semantic = LocalAttendanceColors.current
    val profile = state.profile

    var selectedTerm by remember(state.terms) { mutableStateOf<AcademicTerm?>(null) } // null = Overall (G12 & 13)
    var showMonthlyWrapped by remember { mutableStateOf(false) }
    var showParentSummary by remember { mutableStateOf(false) }

    val personalInsightsForWrapped = remember(
        state.attendanceRecords,
        state.periodBlocks,
        profile,
        state.calendarEvents,
        state.effectiveSchoolStartDateKey
    ) {
        AttendanceCalculator.calculatePersonalInsightsForScope(
            records = state.attendanceRecords,
            periodBlocks = state.periodBlocks,
            timeframe = com.example.data.TrendsTimeframe.MONTHLY,
            maxMonths = 6,
            lockedRange = profile?.lockedBaselineRange,
            calendarEvents = state.calendarEvents,
            schoolStartDate = state.effectiveSchoolStartDateKey
        )
    }

    val stats = remember(
        state.attendanceRecords,
        state.periodBlocks,
        selectedTerm,
        profile,
        state.calendarEvents,
        state.effectiveSchoolStartDateKey
    ) {
        val threshold = selectedTerm?.minEligibilityPercent ?: state.appSettings.defaultEligibilityPercent
        AttendanceCalculator.calculateStats(
            records = state.attendanceRecords,
            thresholdPercent = threshold,
            countExcusedAsPresent = true,
            includePriorBalance = selectedTerm == null || selectedTerm?.id == state.activeTerm.id,
            priorPresent = profile?.priorPresentCount ?: 0,
            priorAbsent = profile?.priorAbsentCount ?: 0,
            termFilter = selectedTerm,
            lockedRange = profile?.lockedBaselineRange,
            periodBlocks = state.periodBlocks,
            calendarEvents = state.calendarEvents,
            schoolStartDate = state.effectiveSchoolStartDateKey
        )
    }

    val subjectCounts = remember(
        state.attendanceRecords,
        state.periodBlocks,
        state.calendarEvents,
        state.effectiveSchoolStartDateKey,
        profile,
        selectedTerm
    ) {
        val term = selectedTerm
        val lockedRange = profile?.lockedBaselineRange
        val cleanBlocks = AttendanceCalculator.deduplicatePeriodBlocks(state.periodBlocks, lockedRange)
        val nonSchoolDates = state.calendarEvents
            .filter { it.nonSchoolDay }
            .map { it.dateKey }
            .toSet()
        val startBound = state.effectiveSchoolStartDateKey
        val uniqueRecords = AttendanceCalculator.deduplicateRecords(state.attendanceRecords)
            .filter { rec ->
                if (DateUtils.isWeekend(rec.dateKey)) return@filter false
                if (rec.dateKey in nonSchoolDates) return@filter false
                if (startBound.isNotBlank() && rec.dateKey < startBound) return@filter false
                if (lockedRange != null && rec.dateKey <= lockedRange.second) return@filter false
                if (cleanBlocks.any { it.containsDate(rec.dateKey) }) return@filter false
                true
            }
        val scoped = if (term != null) {
            uniqueRecords.filter { rec ->
                if (term.startDate.isNotBlank() && term.endDate.isNotBlank()) {
                    rec.dateKey >= term.startDate && rec.dateKey <= term.endDate
                } else {
                    rec.termId == term.id
                }
            }
        } else {
            uniqueRecords
        }
        val map = mutableMapOf<String, Pair<Int, Int>>() // subject -> (present, missed)
        scoped.forEach { rec ->
            rec.subjectTags.forEach { sub ->
                val cur = map[sub] ?: (0 to 0)
                when (rec.statusEnum) {
                    AttendanceStatus.PRESENT,
                    AttendanceStatus.SCHOOL_EVENT,
                    AttendanceStatus.MEDICAL -> map[sub] = (cur.first + 1) to cur.second
                    AttendanceStatus.ABSENT -> map[sub] = cur.first to (cur.second + 1)
                    AttendanceStatus.HOLIDAY -> Unit
                }
            }
        }
        map.entries.sortedByDescending { it.value.first + it.value.second }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .testTag("reports_lazy_column"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = bottomBarPadding
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Term & Eligibility Reports",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Inspect Grade 12 & 13 attendance and export summaries",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Trends & Patterns Card at the very top of Reports
            item(key = "reports_trends_card") {
                StaggeredReveal(index = 0) {
                    AttendancePatternsAndChartCard(
                        records = state.attendanceRecords,
                        thresholdPercent = stats.thresholdPercent,
                        periodBlocks = state.periodBlocks,
                        communitySummaries = state.communityTrendSummaries,
                        profile = state.profile,
                        calendarEvents = state.calendarEvents,
                        schoolStartDate = state.effectiveSchoolStartDateKey
                    )
                }
            }

            // 2. Interactive "What-If" Skip Simulator right below Trends & Patterns
            item(key = "reports_skip_simulator_card") {
                StaggeredReveal(index = 1) {
                    WhatIfSkipSimulatorCard(
                        stats = stats,
                        scopeTitle = selectedTerm?.name ?: "Overall GCE A/L"
                    )
                }
            }

            // Scope Switcher (Overall + Terms) in Real Liquid Glass
            item {
                val visibleTerms = remember(state.terms) { state.terms.take(4) }
                val scopeOptions = remember(visibleTerms) {
                    buildList {
                        add(
                            LiquidGlassSegmentOption(
                                label = "Overall (G12 & 13)",
                                testTag = "reports_scope_overall"
                            )
                        )
                        visibleTerms.forEach { term ->
                            add(
                                LiquidGlassSegmentOption(
                                    label = term.name,
                                    testTag = "reports_scope_${term.id}"
                                )
                            )
                        }
                    }
                }
                val selectedScopeIdx = remember(selectedTerm, visibleTerms) {
                    if (selectedTerm == null) 0
                    else (visibleTerms.indexOfFirst { it.id == selectedTerm?.id } + 1).coerceAtLeast(0)
                }
                SlidableLiquidGlassSegmentedBar(
                    options = scopeOptions,
                    selectedIndex = selectedScopeIdx,
                    onOptionSelected = { idx ->
                        selectedTerm = if (idx == 0) null else visibleTerms.getOrNull(idx - 1)
                    },
                    barHeight = 46.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reports_term_scope_switcher")
                )
            }

            // Summary Card
            item {
                val animatedPct by animateFloatAsState(
                    targetValue = stats.percentage.toFloat().coerceIn(0f, 100f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "reports_summary_pct"
                )
                val summaryAccent by animateColorAsState(
                    targetValue = if (stats.eligible) semantic.present else semantic.absent,
                    animationSpec = tween(360),
                    label = "reports_summary_accent"
                )
                StaggeredReveal(index = 2) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = selectedTerm?.name ?: "Overall GCE A/L",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${stats.effectiveAttended} of ${stats.totalCountedDays} counted school days",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", animatedPct),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = summaryAccent
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (animatedPct / 100f).coerceIn(0f, 1f) },
                                color = summaryAccent,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                StatTile("Present", stats.presentDays, semantic.presentContainer, semantic.onPresentContainer, Modifier.weight(1f))
                                StatTile("Absent", stats.absentDays, semantic.absentContainer, semantic.onAbsentContainer, Modifier.weight(1f))
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                StatTile("Medical", stats.medicalDays, semantic.medicalContainer, semantic.onMedicalContainer, Modifier.weight(1f))
                                StatTile("Excused", stats.schoolEventDays, semantic.schoolEventContainer, semantic.onSchoolEventContainer, Modifier.weight(1f))
                            }

                            if (stats.priorPresentDays > 0 || stats.priorAbsentDays > 0 || stats.periodPresentDays > 0 || stats.periodAbsentDays > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val parts = buildList {
                                        if (stats.priorPresentDays > 0 || stats.priorAbsentDays > 0) {
                                            add("imported baseline: +${stats.priorPresentDays}P / +${stats.priorAbsentDays}A")
                                        }
                                        if (stats.periodPresentDays > 0 || stats.periodAbsentDays > 0) {
                                            add("locked periods: +${stats.periodPresentDays}P / +${stats.periodAbsentDays}A")
                                        }
                                    }
                                    Text(
                                        text = "Includes ${parts.joinToString(" • ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Excused & Medical Policy Info Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = semantic.schoolEventContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "College attendance rule",
                                    tint = semantic.onSchoolEventContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Excused & Medical Count as Present",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Official college sports, club/society events, and medical absences automatically count toward your 80% GCE A/L eligibility and streak.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Milestone Badges & Achievements Shelf
            if (state.milestones.isNotEmpty()) {
                item {
                    MilestoneBadgesCard(milestones = state.milestones)
                }
            }

            // Shareable Cards: Monthly Wrapped & Parent Summary
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reports_tools_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Shareable Cards",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showMonthlyWrapped = true }
                                    .testTag("reports_open_wrapped_btn")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "Monthly Wrapped Card",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Monthly Wrapped",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "WhatsApp graphic",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showParentSummary = true }
                                    .testTag("reports_open_parent_card_btn")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FamilyRestroom,
                                        contentDescription = "Parent Summary Card",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Parent Card",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Clean, no notes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Subject Tags Summary
            if (subjectCounts.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Tagged A/L Subjects",
                                style = MaterialTheme.typography.titleMedium
                            )
                            subjectCounts.forEach { (subject, counts) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(subject, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${counts.first} attended • ${counts.second} missed",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Native Android Share Report Button
            item {
                LiquidGlassButton(
                    onClick = {
                        if (profile != null) {
                            val reportText = AttendanceCalculator.buildShareableReport(
                                profile = profile,
                                scopeTitle = selectedTerm?.name ?: "Overall (Grade 12 & 13)",
                                stats = stats,
                                streak = state.currentStreak
                            )
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "80endance Attendance Report")
                                putExtra(Intent.EXTRA_TEXT, reportText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Attendance Report"))
                        }
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.primary,
                    minHeight = 54.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_report_button")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Term Report")
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showMonthlyWrapped) {
        MonthlyWrappedPreviewDialog(
            profile = profile,
            monthLabel = state.currentMonthLabel,
            stats = state.currentMonthStats.takeIf { it.totalCountedDays > 0 } ?: stats,
            insights = personalInsightsForWrapped,
            streak = state.currentStreak,
            milestones = state.milestones,
            onDismiss = { showMonthlyWrapped = false }
        )
    }

    if (showParentSummary) {
        ParentSummaryPreviewDialog(
            profile = profile,
            termName = selectedTerm?.name ?: "Overall (Grade 12 & 13)",
            stats = stats,
            overallStats = state.overallStats,
            onDismiss = { showParentSummary = false }
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: Int,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            RollingCounterText(
                count = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = fg
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fg.copy(alpha = 0.85f)
            )
        }
    }
}
