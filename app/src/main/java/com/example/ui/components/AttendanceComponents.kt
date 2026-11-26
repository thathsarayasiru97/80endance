package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SportsScore
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.blur
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStats
import com.example.data.AttendanceStatus
import com.example.data.CheckInImpactPreview
import com.example.data.DateUtils
import com.example.data.Notice
import com.example.data.NoticeCategory
import com.example.data.NoticeTargetType
import com.example.data.SyncStatus
import com.example.data.WeekDaySummary
import com.example.ui.theme.LocalAttendanceColors
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedEligibilityGauge(
    stats: AttendanceStats,
    modifier: Modifier = Modifier,
    privacyBlurred: Boolean = false,
    onDoubleTapTogglePrivacy: (() -> Unit)? = null
) {
    val semantic = LocalAttendanceColors.current
    val haptic = LocalHapticFeedback.current
    val latestDoubleTap by rememberUpdatedState(onDoubleTapTogglePrivacy)
    val targetPercent = stats.percentage.toFloat().coerceIn(0f, 100f)
    val totalAbsent = stats.absentDays + stats.priorAbsentDays + stats.periodAbsentDays

    val animatedPercent by animateFloatAsState(
        targetValue = if (privacyBlurred) 50f else targetPercent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "gauge_percent"
    )
    val animatedBlur by animateDpAsState(
        targetValue = if (privacyBlurred) 10.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "gauge_privacy_blur"
    )
    val toggleSquish = remember { Animatable(1f) }
    LaunchedEffect(privacyBlurred) {
        toggleSquish.animateTo(0.95f, tween(80))
        toggleSquish.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
    }

    val mutedNeutralColor = MaterialTheme.colorScheme.outlineVariant
    val isTightEdge = stats.eligible &&
        stats.totalCountedDays >= 5 &&
        stats.percentage < (stats.thresholdPercent + 3.0)
    val accentColor by animateColorAsState(
        targetValue = when {
            privacyBlurred -> mutedNeutralColor
            isTightEdge -> semantic.medical
            stats.eligible -> semantic.present
            else -> semantic.absent
        },
        animationSpec = tween(420),
        label = "gauge_color"
    )
    val pulseTransition = rememberInfiniteTransition(label = "gauge_halo_pulse")
    val haloAlpha by pulseTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gauge_halo_alpha"
    )
    val wavePhaseState = pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gauge_liquid_wave_phase"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val thresholdColor = MaterialTheme.colorScheme.secondary
    val isDarkGauge = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(204.dp)
            .graphicsLayer {
                scaleX = toggleSquish.value
                scaleY = toggleSquish.value
            }
            .then(
                if (onDoubleTapTogglePrivacy != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                latestDoubleTap?.invoke()
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
            .testTag("eligibility_gauge")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 15.dp.toPx()
            val inset = strokePx / 2f + 6.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)
            val startAngle = 140f
            val totalSweep = 260f
            val centerPt = Offset(size.width / 2f, size.height / 2f)

            // Subtle breathing halo behind the active progress arc (suppressed while blurred)
            if (stats.totalCountedDays > 0 && !privacyBlurred) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = haloAlpha),
                            Color.Transparent
                        ),
                        center = centerPt,
                        radius = size.minDimension * 0.50f
                    ),
                    center = centerPt,
                    radius = size.minDimension * 0.50f
                )
            }

            // Inner Living Liquid Glass Wave Orb (clipped inside the gauge ring)
            val innerOrbRadius = (arcSize.width / 2f - strokePx * 0.86f).coerceAtLeast(16f)
            val orbClipPath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        left = centerPt.x - innerOrbRadius,
                        top = centerPt.y - innerOrbRadius,
                        right = centerPt.x + innerOrbRadius,
                        bottom = centerPt.y + innerOrbRadius
                    )
                )
            }
            clipPath(orbClipPath) {
                // Soft inner glass sphere backing
                drawCircle(
                    color = if (isDarkGauge) Color.White.copy(alpha = 0.04f) else accentColor.copy(alpha = 0.05f),
                    radius = innerOrbRadius,
                    center = centerPt
                )

                val fillFraction = (animatedPercent / 100f).coerceIn(0.06f, 0.96f)
                val orbTop = centerPt.y - innerOrbRadius
                val orbBottom = centerPt.y + innerOrbRadius
                val baseWaterY = orbBottom - (innerOrbRadius * 2f * fillFraction)
                val wavePhase = wavePhaseState.value
                val waveAmp = (innerOrbRadius * 0.085f).coerceIn(3f, 8f)
                val steps = 28

                // Back secondary liquid crest
                val backWave = Path().apply {
                    val leftX = centerPt.x - innerOrbRadius
                    val spanX = innerOrbRadius * 2f
                    moveTo(leftX, orbBottom)
                    for (i in 0..steps) {
                        val frac = i.toFloat() / steps
                        val x = leftX + spanX * frac
                        val y = baseWaterY + sin(frac * 2f * PI.toFloat() - wavePhase * 1.15f + 1.2f) * (waveAmp * 0.8f)
                        lineTo(x, y)
                    }
                    lineTo(centerPt.x + innerOrbRadius, orbBottom)
                    close()
                }
                drawPath(
                    path = backWave,
                    color = accentColor.copy(alpha = if (privacyBlurred) 0.08f else if (isDarkGauge) 0.14f else 0.11f)
                )

                // Front primary liquid crest with gentle translucent sheen
                val frontWave = Path().apply {
                    val leftX = centerPt.x - innerOrbRadius
                    val spanX = innerOrbRadius * 2f
                    moveTo(leftX, orbBottom)
                    for (i in 0..steps) {
                        val frac = i.toFloat() / steps
                        val x = leftX + spanX * frac
                        val y = baseWaterY + sin(frac * 2f * PI.toFloat() + wavePhase) * waveAmp
                        lineTo(x, y)
                    }
                    lineTo(centerPt.x + innerOrbRadius, orbBottom)
                    close()
                }
                drawPath(
                    path = frontWave,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (privacyBlurred) 0.12f else if (isDarkGauge) 0.28f else 0.20f),
                            accentColor.copy(alpha = if (privacyBlurred) 0.05f else if (isDarkGauge) 0.11f else 0.08f)
                        ),
                        startY = baseWaterY - waveAmp,
                        endY = orbBottom
                    )
                )

                // Top specular crystal glass highlight crescent
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDarkGauge) 0.16f else 0.42f),
                            Color.Transparent
                        ),
                        center = Offset(centerPt.x - innerOrbRadius * 0.25f, centerPt.y - innerOrbRadius * 0.55f),
                        radius = innerOrbRadius * 0.85f
                    ),
                    radius = innerOrbRadius,
                    center = centerPt
                )
            }
            // Delicate hairline rim around the liquid orb
            drawCircle(
                color = accentColor.copy(alpha = if (privacyBlurred) 0.12f else 0.22f),
                radius = innerOrbRadius,
                center = centerPt,
                style = Stroke(width = 1.dp.toPx())
            )

            // Background track
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Animated progress sweep + luminous comet tip orb
            val progressSweep = (animatedPercent / 100f) * totalSweep
            if (progressSweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(accentColor.copy(alpha = 0.72f), accentColor)
                    ),
                    startAngle = startAngle,
                    sweepAngle = progressSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                if (!privacyBlurred) {
                    val tipRad = (startAngle + progressSweep) * (PI / 180.0)
                    val tipCx = size.width / 2f + (arcSize.width / 2f * cos(tipRad)).toFloat()
                    val tipCy = size.height / 2f + (arcSize.height / 2f * sin(tipRad)).toFloat()
                    drawCircle(
                        color = accentColor.copy(alpha = 0.35f),
                        radius = strokePx * 0.72f,
                        center = Offset(tipCx, tipCy)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.92f),
                        radius = strokePx * 0.28f,
                        center = Offset(tipCx, tipCy)
                    )
                }
            }

            // 80% threshold marker tick
            val thresholdAngleDeg = startAngle + (stats.thresholdPercent / 100f) * totalSweep
            val rad = thresholdAngleDeg * (PI / 180.0)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = arcSize.width / 2f
            val innerR = radius - strokePx * 0.75f
            val outerR = radius + strokePx * 0.75f
            drawLine(
                color = thresholdColor,
                start = Offset(cx + (innerR * cos(rad)).toFloat(), cy + (innerR * sin(rad)).toFloat()),
                end = Offset(cx + (outerR * cos(rad)).toFloat(), cy + (outerR * sin(rad)).toFloat()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = if (animatedBlur > 0.dp) Modifier.blur(animatedBlur) else Modifier
        ) {
            if (privacyBlurred) {
                Text(
                    text = "••.•%",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("gauge_percent_text")
                )
            } else {
                AnimatedOdometerText(
                    text = String.format(Locale.US, "%.1f%%", targetPercent),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("gauge_percent_text")
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            AnimatedContent(
                targetState = privacyBlurred to stats.eligible,
                transitionSpec = {
                    (fadeIn(tween(240)) + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                        .togetherWith(fadeOut(tween(180)))
                },
                label = "eligibility_pill_transition"
            ) { (blurred, eligible) ->
                Surface(
                    color = when {
                        blurred -> MaterialTheme.colorScheme.surfaceContainerHigh
                        eligible -> semantic.presentContainer
                        else -> semantic.absentContainer
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = when {
                            blurred -> "Hidden"
                            eligible -> "Eligible (≥${stats.thresholdPercent}%)"
                            else -> "At Risk (<${stats.thresholdPercent}%)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            blurred -> MaterialTheme.colorScheme.onSurfaceVariant
                            eligible -> semantic.onPresentContainer
                            else -> semantic.onAbsentContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            if (stats.totalCountedDays > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (privacyBlurred) {
                        "•• / •• days • •• absent"
                    } else {
                        "${stats.effectiveAttended}/${stats.totalCountedDays}d • $totalAbsent absent"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("gauge_counts_text")
                )
            }
        }
    }
}

@Composable
fun PinnedNoticeSlideshow(
    pinnedNotices: List<Notice>,
    totalNoticeCount: Int,
    onNoticeClick: (Notice) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pinnedNotices.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { pinnedNotices.size })
    val coroutineScope = rememberCoroutineScope()
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val slideProgress = remember { Animatable(0f) }
    val noticeKey = remember(pinnedNotices) { pinnedNotices.joinToString("|") { it.id } }

    // Keep currentPage valid if notices are dynamically unpinned/deleted
    LaunchedEffect(pinnedNotices.size) {
        if (pinnedNotices.isNotEmpty() && pagerState.currentPage >= pinnedNotices.size) {
            pagerState.scrollToPage(0)
        }
    }

    // Reliable 5-second auto-advance: pauses ONLY during user drag (never cancels mid-animation!)
    LaunchedEffect(noticeKey, isDragged, pagerState.settledPage) {
        if (pinnedNotices.size <= 1) {
            slideProgress.snapTo(0f)
            return@LaunchedEffect
        }
        if (isDragged) {
            slideProgress.stop()
            return@LaunchedEffect
        }
        slideProgress.snapTo(0f)
        slideProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
        )
        val count = pinnedNotices.size
        if (count > 1) {
            val nextPage = (pagerState.currentPage + 1) % count
            pagerState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = "Pinned notices",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (pinnedNotices.size > 1) {
                        "Pinned Updates (${(pagerState.currentPage + 1).coerceAtMost(pinnedNotices.size)}/${pinnedNotices.size})"
                    } else {
                        "Pinned Update"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            TextButton(onClick = onViewAllClick) {
                Text("All ($totalNoticeCount)")
            }
        }

        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pinned_notices_slideshow")
        ) { page ->
            val notice = pinnedNotices.getOrNull(page) ?: return@HorizontalPager
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val scale = (1f - (pageOffset.coerceIn(0f, 1f) * 0.04f))
            val alpha = (1f - (pageOffset.coerceIn(0f, 1f) * 0.22f))

            NoticeBannerCard(
                notice = notice,
                progressFraction = if (pinnedNotices.size > 1 && page == pagerState.currentPage) slideProgress.value else null,
                onClick = { onNoticeClick(notice) },
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
            )
        }

        if (pinnedNotices.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pinnedNotices.size) { index ->
                    val selected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (selected) 22.dp else 7.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(7.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        page = index,
                                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                                    )
                                }
                            }
                            .testTag("pinned_slide_dot_$index")
                    )
                }
            }
        }
    }
}

@Composable
private fun NoticeBannerCard(
    notice: Notice,
    progressFraction: Float? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrgent = notice.urgentFlag || notice.categoryEnum == NoticeCategory.TEACHER_ABSENT
    val isNormalNote = notice.categoryEnum == NoticeCategory.NORMAL_NOTE && !notice.urgentFlag
    val isTip = notice.categoryEnum == NoticeCategory.TIP && !notice.urgentFlag
    val containerColor = when {
        isUrgent -> MaterialTheme.colorScheme.secondaryContainer
        isTip -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.82f)
        isNormalNote -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
    }
    val contentColor = when {
        isUrgent -> MaterialTheme.colorScheme.onSecondaryContainer
        isTip -> MaterialTheme.colorScheme.onTertiaryContainer
        isNormalNote -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = contentColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = notice.categoryEnum.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        if (notice.targetTypeEnum != NoticeTargetType.ALL && !notice.targetValue.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = notice.targetValue,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    if (!notice.eventDate.isNullOrBlank()) {
                        Text(
                            text = DateUtils.formatReadableShort(notice.eventDate),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notice.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (progressFraction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(contentColor.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                            .height(3.dp)
                            .background(contentColor.copy(alpha = 0.55f))
                    )
                }
            }
        }
    }
}

@Composable
fun SyncStatusPill(
    syncStatus: SyncStatus,
    onClick: () -> Unit
) {
    val semantic = LocalAttendanceColors.current
    val (icon, targetBg, targetFg) = when (syncStatus) {
        SyncStatus.SYNCED -> Triple(Icons.Rounded.CloudDone, semantic.presentContainer, semantic.onPresentContainer)
        SyncStatus.SYNCING -> Triple(Icons.Rounded.CloudSync, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        SyncStatus.OFFLINE_CACHED -> Triple(Icons.Rounded.CloudOff, semantic.poyaContainer, semantic.onPoyaContainer)
    }
    val bg by animateColorAsState(targetValue = targetBg, animationSpec = tween(300), label = "sync_bg")
    val fg by animateColorAsState(targetValue = targetFg, animationSpec = tween(300), label = "sync_fg")

    val spinAngle = if (syncStatus == SyncStatus.SYNCING) {
        val spinTransition = rememberInfiniteTransition(label = "sync_icon_spin")
        val angle by spinTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sync_rotation"
        )
        angle
    } else {
        0f
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .bouncyClickable(pressedScale = 0.93f, onClick = onClick)
            .testTag("sync_status_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = syncStatus.label,
                tint = fg,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { rotationZ = spinAngle }
            )
            Spacer(modifier = Modifier.width(5.dp))
            AnimatedContent(
                targetState = syncStatus.label,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                label = "sync_label_anim"
            ) { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = fg
                )
            }
        }
    }
}

@Composable
fun StreakAndWeeklyCard(
    streak: Int,
    weeklySummary: List<WeekDaySummary>,
    onDayClick: (String) -> Unit
) {
    val semantic = LocalAttendanceColors.current
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "This Week & Streak",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Weekends & Poya days never break your streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (streak > 0) semantic.poyaContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LivingStreakFlameIcon(
                            streak = streak,
                            size = 22.dp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        RollingCounterText(
                            count = streak,
                            suffix = "d",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (streak > 0) semantic.onPoyaContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklySummary.forEachIndexed { idx, day ->
                    val (targetBg, targetFg, symbol) = when {
                        day.holidayLabel != null -> Triple(semantic.poyaContainer, semantic.onPoyaContainer, "P")
                        day.isLocked -> Triple(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.onSecondaryContainer,
                            ""
                        )
                        day.isFuture -> Triple(
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            "·"
                        )
                        day.status == AttendanceStatus.PRESENT -> Triple(semantic.presentContainer, semantic.present, "✓")
                        day.status == AttendanceStatus.ABSENT -> Triple(semantic.absentContainer, semantic.absent, "✕")
                        day.status == AttendanceStatus.MEDICAL -> Triple(semantic.medicalContainer, semantic.medical, "M")
                        day.status == AttendanceStatus.SCHOOL_EVENT -> Triple(semantic.schoolEventContainer, semantic.schoolEvent, "E")
                        day.status == AttendanceStatus.HOLIDAY -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "H")
                        else -> Triple(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurfaceVariant, "·")
                    }
                    val bg by animateColorAsState(targetValue = targetBg, animationSpec = tween(280), label = "week_bg_$idx")
                    val fg by animateColorAsState(targetValue = targetFg, animationSpec = tween(280), label = "week_fg_$idx")
                    val canTapDay = !day.isFuture && !day.isLocked

                    var popped by remember(day.dateKey) { mutableStateOf(false) }
                    LaunchedEffect(day.dateKey) {
                        delay(idx * 45L)
                        popped = true
                    }
                    val popScale by animateFloatAsState(
                        targetValue = if (popped) 1f else 0.75f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "week_pop_$idx"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = popScale
                                scaleY = popScale
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (canTapDay) {
                                    Modifier.bouncyClickable(pressedScale = 0.90f) { onDayClick(day.dateKey) }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = day.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                day.isToday -> MaterialTheme.colorScheme.primary
                                day.isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(bg)
                                .then(
                                    if (day.isToday) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            if (day.isLocked && day.holidayLabel == null) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Locked imported attendance day",
                                    tint = fg,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                AnimatedContent(
                                    targetState = symbol,
                                    transitionSpec = {
                                        (scaleIn(
                                            initialScale = 0.6f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                        ) + fadeIn(tween(180)))
                                            .togetherWith(fadeOut(tween(120)))
                                    },
                                    label = "week_symbol_$idx"
                                ) { sym ->
                                    Text(
                                        text = sym,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = fg,
                                        fontWeight = FontWeight.Bold
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

private enum class CheckInStep {
    ASK_YES_NO,
    YES_DETAILS,
    NO_DETAILS
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyCheckInDialog(
    dateKey: String,
    existingRecord: AttendanceRecord?,
    subjects: List<String>,
    isSaving: Boolean,
    previewForStatus: ((AttendanceStatus) -> CheckInImpactPreview?)? = null,
    onDismiss: () -> Unit,
    onSave: (status: AttendanceStatus, subjectTags: List<String>, note: String?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val semantic = LocalAttendanceColors.current
    var step by remember(dateKey) { mutableStateOf(CheckInStep.ASK_YES_NO) }
    var selectedNoStatus by remember(dateKey) {
        mutableStateOf(
            when (existingRecord?.statusEnum) {
                AttendanceStatus.MEDICAL -> AttendanceStatus.MEDICAL
                AttendanceStatus.SCHOOL_EVENT -> AttendanceStatus.SCHOOL_EVENT
                AttendanceStatus.HOLIDAY -> AttendanceStatus.HOLIDAY
                else -> AttendanceStatus.ABSENT
            }
        )
    }
    var selectedSubjects by remember(dateKey) {
        mutableStateOf(existingRecord?.subjectTags?.toSet() ?: emptySet())
    }
    var note by remember(dateKey) { mutableStateOf(existingRecord?.note.orEmpty()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogBackdrop = rememberLayerBackdrop()
        val surfaceBase = MaterialTheme.colorScheme.surfaceContainerLowest
        val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        val secondaryGlow = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = surfaceBase,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Local dialog-window backdrop captured behind foreground controls
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .layerBackdrop(dialogBackdrop)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    surfaceBase,
                                    primaryGlow,
                                    secondaryGlow,
                                    surfaceBase
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        .padding(22.dp)
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = DateUtils.formatReadableShort(dateKey),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close check-in")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val forward = targetState != CheckInStep.ASK_YES_NO
                        val enterSlide = slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { w -> if (forward) w / 4 else -w / 4 }
                        val exitSlide = slideOutHorizontally(tween(160)) { w -> if (forward) -w / 5 else w / 5 }
                        (enterSlide + fadeIn(tween(220)) + scaleIn(initialScale = 0.96f))
                            .togetherWith(exitSlide + fadeOut(tween(150)))
                    },
                    label = "checkin_step_anim"
                ) { currentStep ->
                    when (currentStep) {
                        CheckInStep.ASK_YES_NO -> {
                            Column {
                                Text(
                                    text = if (dateKey == DateUtils.todayKey()) {
                                        "Did you attend school today?"
                                    } else {
                                        "Did you attend school on ${DateUtils.formatReadableShort(dateKey)}?"
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Quick check-in keeps your 80% GCE A/L eligibility accurate.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(22.dp))

                                val absentImpact = remember(dateKey, previewForStatus) {
                                    previewForStatus?.invoke(AttendanceStatus.ABSENT)
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LiquidGlassButton(
                                        onClick = { step = CheckInStep.YES_DETAILS },
                                        backdrop = dialogBackdrop,
                                        tint = semantic.present,
                                        contentColor = semantic.onPresentContainer,
                                        minHeight = 64.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("checkin_yes_button")
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = semantic.present)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Yes", style = MaterialTheme.typography.titleMedium)
                                    }

                                    LiquidGlassButton(
                                        onClick = { step = CheckInStep.NO_DETAILS },
                                        backdrop = dialogBackdrop,
                                        tint = semantic.absent,
                                        contentColor = semantic.onAbsentContainer,
                                        minHeight = 64.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("checkin_no_button")
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.EventBusy, contentDescription = null, tint = semantic.absent)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("No", style = MaterialTheme.typography.titleMedium)
                                            }
                                            if (absentImpact != null) {
                                                Text(
                                                    text = String.format(
                                                        Locale.US,
                                                        "%.1f%% → %.1f%%",
                                                        absentImpact.currentPercent,
                                                        absentImpact.projectedPercent
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = semantic.absent,
                                                    maxLines = 1,
                                                    modifier = Modifier.testTag("checkin_no_btn_impact_badge")
                                                )
                                            }
                                        }
                                    }
                                }

                                if (absentImpact != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    CheckInImpactPreviewBadge(
                                        label = "If Absent",
                                        preview = absentImpact,
                                        isNegative = true,
                                        modifier = Modifier.testTag("checkin_live_impact_preview")
                                    )
                                }

                                if (existingRecord != null && onDelete != null) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    LiquidGlassButton(
                                        onClick = onDelete,
                                        backdrop = dialogBackdrop,
                                        enabled = !isSaving,
                                        tint = semantic.absent,
                                        contentColor = semantic.absent,
                                        minHeight = 46.dp,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("checkin_clear_attendance_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            tint = semantic.absent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Clear Attendance (${existingRecord.statusEnum.shortLabel})",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        CheckInStep.YES_DETAILS -> {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = semantic.present,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Awesome — Marked Present!",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                if (subjects.isNotEmpty()) {
                                    Text(
                                        text = "Optional: tag today's A/L subjects",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        subjects.forEach { sub ->
                                            val isSelected = sub in selectedSubjects
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedSubjects = if (isSelected) selectedSubjects - sub else selectedSubjects + sub
                                                },
                                                label = { Text(sub) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                                OutlinedTextField(
                                    value = note,
                                    onValueChange = { note = it.take(200) },
                                    label = { Text("Optional Note") },
                                    placeholder = { Text("e.g., Lab practical, extra maths class") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { step = CheckInStep.ASK_YES_NO },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Back") }
                                    LiquidGlassButton(
                                        onClick = {
                                            onSave(
                                                AttendanceStatus.PRESENT,
                                                selectedSubjects.toList(),
                                                note.trim().ifEmpty { null }
                                            )
                                        },
                                        backdrop = dialogBackdrop,
                                        enabled = !isSaving,
                                        tint = semantic.present,
                                        contentColor = semantic.onPresentContainer,
                                        minHeight = 48.dp,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .testTag("checkin_confirm_present_button")
                                    ) { Text("Save Present") }
                                }
                                if (existingRecord != null && onDelete != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    TextButton(
                                        onClick = onDelete,
                                        enabled = !isSaving,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("checkin_yes_clear_attendance_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            tint = semantic.absent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Clear Attendance for This Day",
                                            color = semantic.absent,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        CheckInStep.NO_DETAILS -> {
                            Column {
                                Text(
                                    text = "What was the reason?",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val reasonOptions = listOf(
                                    Triple(AttendanceStatus.ABSENT, Icons.Rounded.WarningAmber, "Regular absence"),
                                    Triple(AttendanceStatus.SCHOOL_EVENT, Icons.Rounded.SportsScore, "College sports, club/society, or official event • Counts as present"),
                                    Triple(AttendanceStatus.MEDICAL, Icons.Rounded.LocalHospital, "Sick / medical certificate • Counts as present"),
                                    Triple(AttendanceStatus.HOLIDAY, Icons.Rounded.NightsStay, "School closed / no classes")
                                )

                                val reasonStatuses = remember {
                                    listOf(
                                        AttendanceStatus.ABSENT,
                                        AttendanceStatus.SCHOOL_EVENT,
                                        AttendanceStatus.MEDICAL,
                                        AttendanceStatus.HOLIDAY
                                    )
                                }
                                val reasonSliderOptions = remember(semantic) {
                                    listOf(
                                        LiquidGlassSegmentOption(
                                            label = "Absent",
                                            testTag = "checkin_slider_absent",
                                            tintColor = semantic.absent
                                        ),
                                        LiquidGlassSegmentOption(
                                            label = "Event",
                                            testTag = "checkin_slider_event",
                                            tintColor = semantic.schoolEvent
                                        ),
                                        LiquidGlassSegmentOption(
                                            label = "Medical",
                                            testTag = "checkin_slider_medical",
                                            tintColor = semantic.medical
                                        ),
                                        LiquidGlassSegmentOption(
                                            label = "Off Day",
                                            testTag = "checkin_slider_holiday",
                                            tintColor = semantic.poya
                                        )
                                    )
                                }
                                SlidableLiquidGlassSegmentedBar(
                                    options = reasonSliderOptions,
                                    selectedIndex = reasonStatuses.indexOf(selectedNoStatus).coerceAtLeast(0),
                                    onOptionSelected = { idx ->
                                        reasonStatuses.getOrNull(idx)?.let { selectedNoStatus = it }
                                    },
                                    barHeight = 42.dp,
                                    defaultTint = semantic.absent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("checkin_reason_slider")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    reasonOptions.forEach { (status, icon, desc) ->
                                        val selected = selectedNoStatus == status
                                        val accentColor = when (status) {
                                            AttendanceStatus.SCHOOL_EVENT -> semantic.schoolEvent
                                            AttendanceStatus.MEDICAL -> semantic.medical
                                            AttendanceStatus.ABSENT -> semantic.absent
                                            AttendanceStatus.HOLIDAY -> semantic.poya
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        val containerBg = when {
                                            selected && status == AttendanceStatus.SCHOOL_EVENT -> semantic.schoolEventContainer
                                            selected && status == AttendanceStatus.MEDICAL -> semantic.medicalContainer
                                            selected && status == AttendanceStatus.ABSENT -> semantic.absentContainer
                                            selected && status == AttendanceStatus.HOLIDAY -> semantic.poyaContainer
                                            selected -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = containerBg,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { selectedNoStatus = status }
                                                .testTag("checkin_reason_${status.raw}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = status.label,
                                                    tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = status.label,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = desc,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = note,
                                    onValueChange = { note = it.take(200) },
                                    label = { Text("Note (Optional)") },
                                    placeholder = { Text("e.g., Fever, Bradby practice, Big Match") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val selectedImpact = remember(dateKey, selectedNoStatus, previewForStatus) {
                                    previewForStatus?.invoke(selectedNoStatus)
                                }
                                if (selectedImpact != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    CheckInImpactPreviewBadge(
                                        label = "Impact (${selectedNoStatus.shortLabel})",
                                        preview = selectedImpact,
                                        isNegative = selectedNoStatus == AttendanceStatus.ABSENT,
                                        modifier = Modifier.testTag("checkin_no_live_impact_preview")
                                    )
                                }

                                val confirmTint by animateColorAsState(
                                    targetValue = when (selectedNoStatus) {
                                        AttendanceStatus.ABSENT -> semantic.absent
                                        AttendanceStatus.MEDICAL -> semantic.medical
                                        AttendanceStatus.SCHOOL_EVENT -> semantic.schoolEvent
                                        AttendanceStatus.HOLIDAY -> semantic.poya
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    animationSpec = tween(240),
                                    label = "checkin_confirm_tint"
                                )
                                val confirmContentColor by animateColorAsState(
                                    targetValue = when (selectedNoStatus) {
                                        AttendanceStatus.ABSENT -> semantic.onAbsentContainer
                                        AttendanceStatus.MEDICAL -> semantic.onMedicalContainer
                                        AttendanceStatus.SCHOOL_EVENT -> semantic.onSchoolEventContainer
                                        AttendanceStatus.HOLIDAY -> semantic.onPoyaContainer
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    animationSpec = tween(240),
                                    label = "checkin_confirm_content"
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { step = CheckInStep.ASK_YES_NO },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(0.72f)
                                            .height(56.dp)
                                    ) { Text("Back") }
                                    LiquidGlassButton(
                                        onClick = {
                                            onSave(
                                                selectedNoStatus,
                                                selectedSubjects.toList(),
                                                note.trim().ifEmpty { null }
                                            )
                                        },
                                        backdrop = dialogBackdrop,
                                        enabled = !isSaving,
                                        tint = confirmTint,
                                        contentColor = confirmContentColor,
                                        minHeight = 56.dp,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .weight(1.75f)
                                            .testTag("checkin_confirm_no_button")
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Save ${selectedNoStatus.shortLabel}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            if (selectedImpact != null) {
                                                val shortBuffer = if (selectedImpact.willStayEligible) {
                                                    "${selectedImpact.remainingSafeSkips} safe skip${if (selectedImpact.remainingSafeSkips == 1) "" else "s"} left"
                                                } else {
                                                    "Need +${selectedImpact.daysNeededToRecover}d"
                                                }
                                                Text(
                                                    text = String.format(
                                                        Locale.US,
                                                        "%.1f%% → %.1f%% · %s",
                                                        selectedImpact.currentPercent,
                                                        selectedImpact.projectedPercent,
                                                        shortBuffer
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = confirmTint,
                                                    maxLines = 1,
                                                    modifier = Modifier.testTag("checkin_confirm_live_badge")
                                                )
                                            }
                                        }
                                    }
                                }
                                if (existingRecord != null && onDelete != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    TextButton(
                                        onClick = onDelete,
                                        enabled = !isSaving,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("checkin_no_clear_attendance_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            tint = semantic.absent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Clear Attendance for This Day",
                                            color = semantic.absent,
                                            fontWeight = FontWeight.SemiBold
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
    }
}

@Composable
private fun CheckInImpactPreviewBadge(
    label: String,
    preview: CheckInImpactPreview,
    isNegative: Boolean,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAttendanceColors.current
    val tint = when {
        !preview.willStayEligible -> semantic.absent
        isNegative -> semantic.medical
        else -> semantic.present
    }
    val bg = when {
        !preview.willStayEligible -> semantic.absentContainer.copy(alpha = 0.7f)
        isNegative -> semantic.medicalContainer.copy(alpha = 0.65f)
        else -> semantic.presentContainer.copy(alpha = 0.65f)
    }
    val bufferText = if (preview.willStayEligible) {
        "${preview.remainingSafeSkips} safe skip${if (preview.remainingSafeSkips == 1) "" else "s"} left"
    } else {
        "Drops below 80% (need +${preview.daysNeededToRecover}d)"
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$label: ${String.format(Locale.US, "%.1f%%", preview.currentPercent)} → ${String.format(Locale.US, "%.1f%%", preview.projectedPercent)} · $bufferText",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeBoardSheet(
    notices: List<Notice>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "School & Class Notices",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (notices.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No notices posted yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Teacher absence alerts and schedule updates will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(notices, key = { it.id }) { notice ->
                        NoticeBannerCard(notice = notice, onClick = {})
                    }
                }
            }
        }
    }
}
