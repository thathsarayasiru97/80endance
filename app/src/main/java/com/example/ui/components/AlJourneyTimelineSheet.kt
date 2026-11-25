package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AlJourneyTimeline
import com.example.data.DateUtils
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusOrange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated A/L Countdown Pill with shimmering gold comet border sweep,
 * breathing halo pulse, and smooth tap spring.
 */
@Composable
fun AnimatedAlCountdownPill(
    daysUntilAlExam: Int,
    examYear: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "al_pill_glow")
    val shimmerPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_phase"
    )
    val beaconPulse by infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_pulse"
    )
    val iconBreath by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_breath"
    )

    val angleRad = shimmerPhase * 2f * PI.toFloat()
    val startX = 0.5f + 0.5f * cos(angleRad)
    val startY = 0.5f + 0.5f * sin(angleRad)
    val endX = 1f - startX
    val endY = 1f - startY

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            RoyalGold.copy(alpha = 0.28f),
            RoyalGold.copy(alpha = 0.95f),
            Color.White.copy(alpha = 0.85f),
            RoyalGold.copy(alpha = 0.30f)
        ),
        start = Offset(startX * 320f, startY * 90f),
        end = Offset(endX * 320f, endY * 90f)
    )

    val pillShape = RoundedCornerShape(999.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(34.dp)
            .clip(pillShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF102448),
                        Color(0xFF1A3464),
                        Color(0xFF132952)
                    )
                )
            )
            .border(width = 1.4.dp, brush = borderBrush, shape = pillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .scale(beaconPulse)
                        .clip(CircleShape)
                        .background(RoyalGold.copy(alpha = 0.28f))
                )
                Icon(
                    imageVector = Icons.Rounded.HourglassTop,
                    contentDescription = "A/L Exam Countdown",
                    tint = RoyalGold,
                    modifier = Modifier
                        .size(14.dp)
                        .scale(iconBreath)
                )
            }
            Text(
                text = if (daysUntilAlExam > 0) "${daysUntilAlExam}d" else "Exam Day",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.2.sp
                ),
                color = Color.White
            )
            Text(
                text = "to A/L $examYear",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = RoyalGold.copy(alpha = 0.95f)
            )
        }
    }
}

/**
 * Expanded A/L Journey Timeline Sheet showing progress from Grade 12 Day 1 to A/L Exam Day 1,
 * with glowing animated progress bar and 200d / 100d / 50d / Term Test checkpoints.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlJourneyTimelineSheet(
    timeline: AlJourneyTimeline,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(timeline.progressFraction) { animateTrigger = true }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animateTrigger) timeline.progressFraction else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 120f),
        label = "al_journey_progress"
    )
    val infinite = rememberInfiniteTransition(label = "al_bar_pulse")
    val markerPulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "marker_pulse"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("al_journey_timeline_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = RoyalGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = RoyalGold,
                            modifier = Modifier.padding(9.dp).size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "A/L ${timeline.examYear} Journey",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "Exam Day 1: ${DateUtils.formatReadableFull(timeline.examDateKey)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("al_timeline_close")) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            // Big Countdown Hero Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF0E2144),
                border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "${timeline.daysRemaining}",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "days until GCE A/L ${timeline.examYear}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD6E4FF)
                            )
                        }
                        Surface(
                            color = RoyalGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "${(animatedProgress * 100f).toInt()}% Complete",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = RoyalGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Glowing Horizontal Journey Milestone Bar
                    val trackColor = Color.White.copy(alpha = 0.14f)
                    val primaryBlue = Color(0xFF60A5FA)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    ) {
                        val barY = size.height / 2f
                        val barH = 10.dp.toPx()
                        val startX = 10.dp.toPx()
                        val endX = size.width - 10.dp.toPx()
                        val usableW = (endX - startX).coerceAtLeast(1f)

                        // Background rail
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(startX, barY - barH / 2f),
                            size = Size(usableW, barH),
                            cornerRadius = CornerRadius(barH, barH)
                        )
                        // Filled progress gradient
                        val fillW = (usableW * animatedProgress).coerceIn(0f, usableW)
                        if (fillW > 1f) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(primaryBlue, RoyalGold)
                                ),
                                topLeft = Offset(startX, barY - barH / 2f),
                                size = Size(fillW, barH),
                                cornerRadius = CornerRadius(barH, barH)
                            )
                        }
                        // Checkpoint ticks along the bar
                        timeline.milestones.forEach { ms ->
                            val x = startX + usableW * ms.fraction.coerceIn(0f, 1f)
                            val dotColor = when {
                                ms.isExamDay -> RoyalGold
                                ms.reached -> StatusGreen
                                ms.isTermTest -> StatusOrange
                                else -> Color.White.copy(alpha = 0.65f)
                            }
                            drawCircle(
                                color = dotColor,
                                radius = if (ms.isExamDay) 6.dp.toPx() else 4.dp.toPx(),
                                center = Offset(x, barY)
                            )
                        }
                        // Pulsing "You Are Here" live beacon
                        val currentX = startX + fillW
                        drawCircle(
                            color = RoyalGold.copy(alpha = 0.30f),
                            radius = 12.dp.toPx() * markerPulse,
                            center = Offset(currentX, barY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(currentX, barY)
                        )
                        drawCircle(
                            color = RoyalGold,
                            radius = 3.5.dp.toPx(),
                            center = Offset(currentX, barY)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Grade 12 Day 1",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9FB7E0)
                        )
                        Text(
                            text = "${timeline.elapsedDays}d elapsed · ${timeline.daysRemaining}d left",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = RoyalGold
                        )
                        Text(
                            text = "A/L Day 1",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9FB7E0)
                        )
                    }
                }
            }

            // Interactive Thin-Film Soap Bubble & Refractive A/L Countdown Lens
            ThinFilmBubbleInteractiveCard(
                RestingPromptText = "${timeline.daysRemaining} Days to A/L ${timeline.examYear}\nDrag the thin-film bubble up",
                LiftedTitleText = "${(animatedProgress * 100f).toInt()}% Journey Complete",
                LiftedSubtitleText = "${timeline.elapsedDays}d completed · ${timeline.daysRemaining}d left to A/L Day 1\nTap the bubble to pop or switch optics below.",
                initialPreset = BubbleLookPreset.SOAP_BUBBLE,
                initialDarkTheme = true,
                cardHeight = 256.dp
            )

            // Vertical Checkpoints List
            Text(
                text = "Key Journey Checkpoints",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                timeline.milestones.forEach { ms ->
                    val tint = when {
                        ms.isExamDay -> RoyalGold
                        ms.reached -> StatusGreen
                        ms.isTermTest -> StatusOrange
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (ms.isExamDay) {
                            RoyalGold.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (ms.isExamDay) RoyalGold.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    ms.isExamDay -> Icons.Rounded.Flag
                                    ms.reached -> Icons.Rounded.CheckCircle
                                    ms.isTermTest -> Icons.Rounded.School
                                    else -> Icons.Rounded.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ms.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = DateUtils.formatReadableShort(ms.dateKey),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val daysAway = DateUtils.daysBetween(DateUtils.todayKey(), ms.dateKey)
                            val badge = when {
                                ms.reached && !ms.isExamDay -> "Reached"
                                daysAway == 0 -> "Today!"
                                daysAway > 0 -> "In ${daysAway}d"
                                else -> "Passed"
                            }
                            Surface(
                                color = tint.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = tint,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
