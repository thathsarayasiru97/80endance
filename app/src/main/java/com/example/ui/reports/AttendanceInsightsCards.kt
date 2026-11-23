package com.example.ui.reports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceCalculator
import com.example.data.AttendancePatternInsights
import com.example.data.AttendancePeriodBlock
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStats
import com.example.data.CalendarEvent
import com.example.data.CohortPulseSummary
import com.example.data.MonthlyAttendancePoint
import com.example.data.StudentTrendSummary
import com.example.data.TrendsTimeframe
import com.example.data.UserProfile
import com.example.ui.theme.LocalAttendanceColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatIfSkipSimulatorCard(
    stats: AttendanceStats,
    scopeTitle: String,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAttendanceColors.current
    var simulateSkips by remember { mutableStateOf(true) }
    var daysSlider by remember { mutableFloatStateOf(2f) }
    val selectedDays = daysSlider.roundToInt().coerceIn(0, 15)

    val projection = remember(stats, simulateSkips, selectedDays) {
        if (simulateSkips) {
            AttendanceCalculator.simulateFutureSkips(stats = stats, extraSkips = selectedDays, extraAttends = 0)
        } else {
            AttendanceCalculator.simulateFutureSkips(stats = stats, extraSkips = 0, extraAttends = selectedDays)
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("what_if_simulator_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Science,
                            contentDescription = "What-If Skip Simulator",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "\"What-If\" Skip Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Preview how $scopeTitle changes before taking days off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = simulateSkips,
                    onClick = { simulateSkips = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("sim_mode_skip")
                ) {
                    Text("If I skip days")
                }
                SegmentedButton(
                    selected = !simulateSkips,
                    onClick = { simulateSkips = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("sim_mode_attend")
                ) {
                    Text("If I attend next")
                }
            }

            // Projected result banner with smooth emerald <-> crimson color morph & spring numbers
            val resultBg by animateColorAsState(
                targetValue = if (projection.willStayEligible) {
                    semantic.presentContainer.copy(alpha = 0.72f)
                } else {
                    semantic.absentContainer.copy(alpha = 0.85f)
                },
                animationSpec = tween(320),
                label = "sim_result_bg"
            )
            val resultFg by animateColorAsState(
                targetValue = if (projection.willStayEligible) {
                    semantic.onPresentContainer
                } else {
                    semantic.onAbsentContainer
                },
                animationSpec = tween(320),
                label = "sim_result_fg"
            )
            val animatedProjectedPct by animateFloatAsState(
                targetValue = projection.projectedPercent.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "sim_pct_anim"
            )

            Surface(
                color = resultBg,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (selectedDays == 0) {
                                    "Current standing"
                                } else if (simulateSkips) {
                                    "After skipping $selectedDays school day${if (selectedDays == 1) "" else "s"}"
                                } else {
                                    "After attending $selectedDays straight day${if (selectedDays == 1) "" else "s"}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = resultFg.copy(alpha = 0.85f)
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f%%", animatedProjectedPct),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = resultFg,
                                modifier = Modifier.testTag("sim_projected_percent")
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            AnimatedContent(
                                targetState = projection.willStayEligible,
                                transitionSpec = {
                                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.88f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                                        .togetherWith(fadeOut(tween(150)))
                                },
                                label = "sim_eligibility_badge"
                            ) { staysEligible ->
                                Surface(
                                    color = resultFg.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = if (staysEligible) {
                                            "Eligible (>= ${stats.thresholdPercent}%)"
                                        } else {
                                            "Drops < ${stats.thresholdPercent}%"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = resultFg,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (abs(projection.percentDelta) >= 0.05) {
                                Spacer(modifier = Modifier.height(4.dp))
                                val sign = if (projection.percentDelta >= 0) "+" else ""
                                Text(
                                    text = "$sign${String.format(Locale.US, "%.1f%%", projection.percentDelta)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = resultFg
                                )
                            }
                        }
                    }

                    Text(
                        text = when {
                            projection.projectedTotal == 0 ->
                                "Log attendance days to simulate your exact safe skip buffer."
                            projection.willStayEligible && projection.remainingSafeSkips > 0 ->
                                "Still safe! You would have ${projection.remainingSafeSkips} more skip${if (projection.remainingSafeSkips == 1) "" else "s"} left after that."
                            projection.willStayEligible ->
                                "Right on the ${stats.thresholdPercent}% threshold — no extra skips left afterward."
                            else ->
                                "You'd need ${projection.daysNeededToRecover} consecutive school day${if (projection.daysNeededToRecover == 1) "" else "s"} afterward to get back to ${stats.thresholdPercent}%."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = resultFg.copy(alpha = 0.9f)
                    )
                }
            }

            // Slider + Quick Presets
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (simulateSkips) "Extra days off: $selectedDays" else "Consecutive days attended: $selectedDays",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${projection.projectedAttended} / ${projection.projectedTotal} days",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Slider(
                    value = daysSlider,
                    onValueChange = { daysSlider = it },
                    valueRange = 0f..15f,
                    steps = 14,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sim_days_slider")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1, 3, 5, 10).forEach { preset ->
                        FilterChip(
                            selected = selectedDays == preset,
                            onClick = { daysSlider = preset.toFloat() },
                            label = { Text("${preset}d") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendancePatternsAndChartCard(
    records: List<AttendanceRecord>,
    thresholdPercent: Int,
    periodBlocks: List<AttendancePeriodBlock> = emptyList(),
    communitySummaries: List<StudentTrendSummary> = emptyList(),
    profile: UserProfile? = null,
    calendarEvents: List<CalendarEvent> = emptyList(),
    schoolStartDate: String? = null,
    modifier: Modifier = Modifier
) {
    // Page 0 = Everyone's Trends & Patterns (DEFAULT tile, defaulting to All Time)
    // Page 1 = My Personal Trends & Patterns
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    var everyoneTimeframe by remember { mutableStateOf(TrendsTimeframe.ALL_TIME) }
    var personalTimeframe by remember { mutableStateOf(TrendsTimeframe.ALL_TIME) }

    val everyoneInsights = remember(communitySummaries, everyoneTimeframe) {
        AttendanceCalculator.calculateCommunityInsights(
            summaries = communitySummaries,
            timeframe = everyoneTimeframe,
            maxMonths = 6
        )
    }
    val cohortPulse = remember(communitySummaries, profile, everyoneTimeframe) {
        AttendanceCalculator.calculateCohortPulse(
            summaries = communitySummaries,
            profile = profile,
            timeframe = everyoneTimeframe
        )
    }
    val personalInsights = remember(records, periodBlocks, personalTimeframe, profile, calendarEvents, schoolStartDate) {
        AttendanceCalculator.calculatePersonalInsightsForScope(
            records = records,
            periodBlocks = periodBlocks,
            timeframe = personalTimeframe,
            maxMonths = 6,
            lockedRange = profile?.lockedBaselineRange,
            calendarEvents = calendarEvents,
            schoolStartDate = schoolStartDate
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("attendance_patterns_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Header + Swipe Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (pagerState.currentPage == 0) Icons.Rounded.Groups else Icons.Rounded.AutoGraph,
                                contentDescription = "Trends and patterns",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Trends & Patterns",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (pagerState.currentPage == 0) {
                                "Everyone's school-wide attendance insights"
                            } else {
                                "Your personal attendance & weekday habits"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Swipe,
                            contentDescription = "Swipe between Everyone and My Stats",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (pagerState.currentPage == 0) "Swipe" else "Swipe",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Synchronized Scope Selector (Everyone [default] | My Stats)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trends_scope_switcher")
            ) {
                SegmentedButton(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("trends_tab_everyone")
                ) {
                    Text("Everyone", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                SegmentedButton(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("trends_tab_personal")
                ) {
                    Text("My Stats", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Slidable 2-Page Content (Page 0 = Everyone, Page 1 = Personal)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trends_horizontal_pager")
            ) { page ->
                if (page == 0) {
                    TrendsInsightSlideContent(
                        isCommunity = true,
                        timeframe = everyoneTimeframe,
                        onTimeframeChange = { everyoneTimeframe = it },
                        insights = everyoneInsights,
                        cohortPulse = cohortPulse,
                        thresholdPercent = thresholdPercent,
                        pageTag = "trends_page_everyone",
                        allTimeTag = "trends_everyone_all_time",
                        monthlyTag = "trends_everyone_monthly"
                    )
                } else {
                    TrendsInsightSlideContent(
                        isCommunity = false,
                        timeframe = personalTimeframe,
                        onTimeframeChange = { personalTimeframe = it },
                        insights = personalInsights,
                        cohortPulse = null,
                        thresholdPercent = thresholdPercent,
                        pageTag = "trends_page_personal",
                        allTimeTag = "trends_personal_all_time",
                        monthlyTag = "trends_personal_monthly"
                    )
                }
            }

            // Pager Dot Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(2) { idx ->
                    val active = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(7.dp)
                            .width(if (active) 22.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendsInsightSlideContent(
    isCommunity: Boolean,
    timeframe: TrendsTimeframe,
    onTimeframeChange: (TrendsTimeframe) -> Unit,
    insights: AttendancePatternInsights,
    cohortPulse: CohortPulseSummary?,
    thresholdPercent: Int,
    pageTag: String,
    allTimeTag: String,
    monthlyTag: String
) {
    val semantic = LocalAttendanceColors.current
    val hasAnyData = insights.totalCountedDays > 0 || insights.monthlyPoints.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(pageTag),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Timeframe Toggle inside each card: All Time | Monthly
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = timeframe == TrendsTimeframe.ALL_TIME,
                onClick = { onTimeframeChange(TrendsTimeframe.ALL_TIME) },
                label = { Text("All Time") },
                modifier = Modifier.testTag(allTimeTag)
            )
            FilterChip(
                selected = timeframe == TrendsTimeframe.MONTHLY,
                onClick = { onTimeframeChange(TrendsTimeframe.MONTHLY) },
                label = { Text("Monthly") },
                modifier = Modifier.testTag(monthlyTag)
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isCommunity && insights.contributorCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${insights.contributorCount} student${if (insights.contributorCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (!hasAnyData) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isCommunity) {
                        if (timeframe == TrendsTimeframe.MONTHLY) {
                            "No school-wide attendance logs recorded for this month yet. Check All Time or log today's attendance."
                        } else {
                            "School-wide trends will appear automatically as students log attendance."
                        }
                    } else {
                        if (timeframe == TrendsTimeframe.MONTHLY) {
                            "No attendance logged for this month yet. Switch to All Time or check in on school days."
                        } else {
                            "Log a few school days or lock a period to unlock your personal trends and weekday patterns."
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            // Headline Summary Banner
            val headlinePct = insights.overallPercent ?: 0.0
            val headlineOk = headlinePct >= thresholdPercent
            val headlineBg = if (headlineOk) {
                semantic.presentContainer.copy(alpha = 0.58f)
            } else {
                semantic.absentContainer.copy(alpha = 0.68f)
            }
            val headlineFg = if (headlineOk) semantic.onPresentContainer else semantic.onAbsentContainer
            Surface(
                color = headlineBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val scopeHeadline = when {
                            isCommunity && timeframe == TrendsTimeframe.ALL_TIME -> "Everyone • All-Time Average"
                            isCommunity && timeframe == TrendsTimeframe.MONTHLY -> "Everyone • ${insights.activeMonthLabel ?: "This Month"}"
                            !isCommunity && timeframe == TrendsTimeframe.ALL_TIME -> "My All-Time Average"
                            else -> "My ${insights.activeMonthLabel ?: "Monthly"} Average"
                        }
                        Text(
                            text = scopeHeadline,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = headlineFg.copy(alpha = 0.88f)
                        )
                        Text(
                            text = "${insights.totalAttendedDays} / ${insights.totalCountedDays} counted school days",
                            style = MaterialTheme.typography.bodySmall,
                            color = headlineFg.copy(alpha = 0.8f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "%.1f%%", headlinePct),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = headlineFg
                        )
                        insights.monthOverMonthDelta?.takeIf { abs(it) >= 0.05 }?.let { delta ->
                            val sign = if (delta >= 0) "+" else ""
                            Text(
                                text = "$sign${String.format(Locale.US, "%.1f%%", delta)} vs prev mo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = headlineFg
                            )
                        }
                    }
                }
            }

            // 0. Anonymous Stream & Class Average Pulse (Everyone tab only)
            if (isCommunity && cohortPulse != null && cohortPulse.items.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cohort_pulse_section")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Class · Stream · Grade Pulse",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "100% anonymous",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        cohortPulse.items.forEachIndexed { idx, item ->
                            val targetFrac = (item.percentage / 100.0).toFloat().coerceIn(0f, 1f)
                            var pulseRevealed by remember(timeframe, item.scope, targetFrac) { mutableStateOf(false) }
                            LaunchedEffect(timeframe, item.scope, targetFrac) {
                                delay(idx * 55L)
                                pulseRevealed = true
                            }
                            val animatedFrac by animateFloatAsState(
                                targetValue = if (pulseRevealed) targetFrac else 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "cohort_pulse_${item.scope}"
                            )
                            val barTint = when {
                                item.percentage >= thresholdPercent -> semantic.present
                                else -> semantic.absent
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.label} (${item.contributorCount})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", item.percentage),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = barTint
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(animatedFrac)
                                            .clip(RoundedCornerShape(50))
                                            .background(barTint)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1. Monthly Bar Chart
            if (insights.monthlyPoints.isNotEmpty()) {
                MonthlyAttendanceBarChart(
                    points = insights.monthlyPoints,
                    thresholdPercent = thresholdPercent,
                    presentColor = semantic.present,
                    absentColor = semantic.absent,
                    thresholdColor = MaterialTheme.colorScheme.primary
                )
            }

            // 2. Smart Insight Callout Pills
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (timeframe == TrendsTimeframe.ALL_TIME) {
                    insights.bestMonth?.let { bestM ->
                        val prefix = if (isCommunity) "Strongest school month" else "Best month"
                        InsightRow(
                            icon = Icons.Rounded.EmojiEvents,
                            bg = semantic.presentContainer.copy(alpha = 0.65f),
                            fg = semantic.onPresentContainer,
                            text = "$prefix: ${bestM.fullLabel} (${String.format(Locale.US, "%.0f%%", bestM.percentage)} • ${bestM.attendedDays}/${bestM.totalDays} days)"
                        )
                    }
                } else {
                    insights.monthOverMonthDelta?.let { delta ->
                        val rising = delta >= 0
                        val subjectLabel = if (isCommunity) "School-wide attendance" else "Your attendance"
                        val dirWord = if (rising) "up" else "down"
                        InsightRow(
                            icon = if (rising) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                            bg = if (rising) semantic.presentContainer.copy(alpha = 0.65f) else semantic.absentContainer.copy(alpha = 0.72f),
                            fg = if (rising) semantic.onPresentContainer else semantic.onAbsentContainer,
                            text = "$subjectLabel is $dirWord ${String.format(Locale.US, "%.1f%%", abs(delta))} compared to last month"
                        )
                    }
                }

                insights.mostSkippedWeekday?.let { skipped ->
                    val subjectPhrase = if (isCommunity) "are the most skipped day across students" else "are your most skipped day"
                    InsightRow(
                        icon = Icons.AutoMirrored.Rounded.TrendingDown,
                        bg = semantic.absentContainer.copy(alpha = 0.72f),
                        fg = semantic.onAbsentContainer,
                        text = "${skipped.fullName}s $subjectPhrase (${skipped.absentDays} missed • ${String.format(Locale.US, "%.0f%%", skipped.percentage)} attended)"
                    )
                }

                insights.bestWeekday?.takeIf { it.dayIndex != insights.mostSkippedWeekday?.dayIndex }?.let { bestDay ->
                    val prefix = if (isCommunity) "Most attended weekday" else "Most consistent weekday"
                    InsightRow(
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        bg = semantic.schoolEventContainer.copy(alpha = 0.65f),
                        fg = semantic.onSchoolEventContainer,
                        text = "$prefix: ${bestDay.fullName} (${String.format(Locale.US, "%.0f%%", bestDay.percentage)} attended)"
                    )
                }
            }

            // 3. Mon–Fri Weekday Breakdown Bars
            val hasWeekdayData = insights.weekdayStats.any { it.totalDays > 0 }
            if (hasWeekdayData) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when {
                                isCommunity && timeframe == TrendsTimeframe.ALL_TIME -> "School Weekday Rate (All Time)"
                                isCommunity && timeframe == TrendsTimeframe.MONTHLY -> "School Weekday Rate (${insights.activeMonthLabel ?: "Monthly"})"
                                !isCommunity && timeframe == TrendsTimeframe.ALL_TIME -> "My Weekday Rate (All Time)"
                                else -> "My Weekday Rate (${insights.activeMonthLabel ?: "Monthly"})"
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    insights.weekdayStats.forEachIndexed { dayIdx, day ->
                        val targetFraction = if (day.totalDays > 0) (day.percentage / 100.0).toFloat().coerceIn(0f, 1f) else 0f
                        var barRevealed by remember(pageTag, timeframe, day.shortName, targetFraction) { mutableStateOf(false) }
                        LaunchedEffect(pageTag, timeframe, day.shortName, targetFraction) {
                            delay(dayIdx * 45L)
                            barRevealed = true
                        }
                        val animatedFraction by animateFloatAsState(
                            targetValue = if (barRevealed) targetFraction else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "weekday_bar_${pageTag}_${day.shortName}"
                        )
                        val barColor = when {
                            day.totalDays == 0 -> MaterialTheme.colorScheme.outlineVariant
                            day.percentage >= thresholdPercent -> semantic.present
                            else -> semantic.absent
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = day.shortName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(36.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(animatedFraction)
                                        .clip(RoundedCornerShape(50))
                                        .background(barColor)
                                )
                            }
                            Text(
                                text = if (day.totalDays == 0) "—" else String.format(Locale.US, "%.0f%%", day.percentage),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(40.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyAttendanceBarChart(
    points: List<MonthlyAttendancePoint>,
    thresholdPercent: Int,
    presentColor: Color,
    absentColor: Color,
    thresholdColor: Color
) {
    val barAnimatables = remember(points.size) { List(points.size) { Animatable(0f) } }
    LaunchedEffect(points) {
        barAnimatables.forEachIndexed { idx, anim ->
            launch {
                anim.snapTo(0f)
                delay(idx * 65L)
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val goalFraction = (thresholdPercent.coerceIn(50, 99) / 100f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monthly_attendance_chart"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Percentage labels above each bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            points.forEach { pt ->
                Text(
                    text = String.format(Locale.US, "%.0f%%", pt.percentage),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (pt.percentage >= thresholdPercent) presentColor else absentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bar canvas with dashed 80% threshold line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = points.size.coerceAtLeast(1)
                val slotWidth = size.width / count
                val barWidth = (slotWidth * 0.46f).coerceIn(20f, 76f)
                val chartHeight = size.height

                // Draw background tracks & staggered spring-rising bars
                points.forEachIndexed { idx, pt ->
                    val centerX = slotWidth * idx + slotWidth / 2f
                    val left = centerX - barWidth / 2f
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                    val progress = barAnimatables.getOrNull(idx)?.value ?: 1f
                    val pctFraction = (pt.percentage.toFloat() / 100f).coerceIn(0.04f, 1f) * progress
                    val barHeight = (chartHeight * pctFraction).coerceAtLeast(0f)
                    val barColor = if (pt.percentage >= thresholdPercent) presentColor else absentColor
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, chartHeight - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                }

                // Draw 80% dashed goal line across chart
                val goalY = chartHeight * (1f - goalFraction)
                drawLine(
                    color = thresholdColor.copy(alpha = 0.85f),
                    start = Offset(0f, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
            }
        }

        // Month labels underneath each bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.shortLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            text = "Dashed line marks your $thresholdPercent% GCE A/L eligibility goal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InsightRow(
    icon: ImageVector,
    bg: Color,
    fg: Color,
    text: String
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = fg
            )
        }
    }
}
