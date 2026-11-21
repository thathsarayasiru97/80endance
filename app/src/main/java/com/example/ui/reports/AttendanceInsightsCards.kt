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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
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
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceCalculator
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStats
import com.example.data.MonthlyAttendancePoint
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
    modifier: Modifier = Modifier
) {
    val semantic = LocalAttendanceColors.current
    val insights = remember(records) {
        AttendanceCalculator.calculateAttendanceInsights(records, maxMonths = 6)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AutoGraph,
                            contentDescription = "Monthly trends and patterns",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Monthly Trends & Patterns",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Track monthly consistency & weekday habits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Monthly Bar Chart
            if (insights.monthlyPoints.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Log a few school days to unlock your monthly attendance bar chart and weekday insights.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                MonthlyAttendanceBarChart(
                    points = insights.monthlyPoints,
                    thresholdPercent = thresholdPercent,
                    presentColor = semantic.present,
                    absentColor = semantic.absent,
                    thresholdColor = MaterialTheme.colorScheme.primary
                )

                // 2. Smart Insight Callout Pills
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    insights.bestMonth?.let { bestM ->
                        InsightRow(
                            icon = Icons.Rounded.EmojiEvents,
                            bg = semantic.presentContainer.copy(alpha = 0.65f),
                            fg = semantic.onPresentContainer,
                            text = "Best month: ${bestM.fullLabel} (${String.format(Locale.US, "%.0f%%", bestM.percentage)} • ${bestM.attendedDays}/${bestM.totalDays} days)"
                        )
                    }
                    insights.mostSkippedWeekday?.let { skipped ->
                        InsightRow(
                            icon = Icons.Rounded.TrendingDown,
                            bg = semantic.absentContainer.copy(alpha = 0.72f),
                            fg = semantic.onAbsentContainer,
                            text = "${skipped.fullName}s are your most skipped day (${skipped.absentDays} missed • ${String.format(Locale.US, "%.0f%%", skipped.percentage)} attended)"
                        )
                    }
                    insights.bestWeekday?.takeIf { it.dayIndex != insights.mostSkippedWeekday?.dayIndex }?.let { bestDay ->
                        InsightRow(
                            icon = Icons.Rounded.TrendingUp,
                            bg = semantic.schoolEventContainer.copy(alpha = 0.65f),
                            fg = semantic.onSchoolEventContainer,
                            text = "Most consistent weekday: ${bestDay.fullName} (${String.format(Locale.US, "%.0f%%", bestDay.percentage)} attended)"
                        )
                    }
                }

                // 3. Mon–Fri Weekday Breakdown Bars
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
                            text = "Weekday Attendance Rate (Mon – Fri)",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    insights.weekdayStats.forEachIndexed { dayIdx, day ->
                        val targetFraction = if (day.totalDays > 0) (day.percentage / 100.0).toFloat().coerceIn(0f, 1f) else 0f
                        var barRevealed by remember(day.shortName, targetFraction) { mutableStateOf(false) }
                        LaunchedEffect(day.shortName, targetFraction) {
                            delay(dayIdx * 55L)
                            barRevealed = true
                        }
                        val animatedFraction by animateFloatAsState(
                            targetValue = if (barRevealed) targetFraction else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "weekday_bar_${day.shortName}"
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
