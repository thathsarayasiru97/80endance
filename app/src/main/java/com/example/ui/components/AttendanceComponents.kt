package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.LocalFireDepartment
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStats
import com.example.data.AttendanceStatus
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
import java.util.Locale
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedEligibilityGauge(
    stats: AttendanceStats,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAttendanceColors.current
    val targetPercent = stats.percentage.toFloat().coerceIn(0f, 100f)

    val animatedPercent by animateFloatAsState(
        targetValue = targetPercent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "gauge_percent"
    )

    val accentColor by animateColorAsState(
        targetValue = if (stats.eligible) semantic.present else semantic.absent,
        animationSpec = tween(400),
        label = "gauge_color"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val thresholdColor = MaterialTheme.colorScheme.secondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(188.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 16.dp.toPx()
            val inset = strokePx / 2f + 4.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)
            val startAngle = 140f
            val totalSweep = 260f

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

            // Animated progress sweep
            val progressSweep = (animatedPercent / 100f) * totalSweep
            if (progressSweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(accentColor.copy(alpha = 0.78f), accentColor)
                    ),
                    startAngle = startAngle,
                    sweepAngle = progressSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
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

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.US, "%.1f%%", animatedPercent),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                color = if (stats.eligible) semantic.presentContainer else semantic.absentContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = if (stats.eligible) "Eligible (≥${stats.thresholdPercent}%)" else "At Risk (<${stats.thresholdPercent}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (stats.eligible) semantic.onPresentContainer else semantic.onAbsentContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            if (stats.totalCountedDays > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stats.effectiveAttended} / ${stats.totalCountedDays} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

    // Auto-advance every 5 seconds when multiple pinned notices exist (pauses during user drag)
    LaunchedEffect(pinnedNotices.size, pagerState.isScrollInProgress) {
        if (pinnedNotices.size > 1 && !pagerState.isScrollInProgress) {
            while (true) {
                delay(5000L)
                val nextPage = (pagerState.currentPage + 1) % pinnedNotices.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
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
                    text = if (pinnedNotices.size > 1) "Pinned Updates (${pinnedNotices.size})" else "Pinned Update",
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
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pinned_notices_slideshow")
        ) { page ->
            val notice = pinnedNotices[page]
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val scale = (1f - (pageOffset.coerceIn(0f, 1f) * 0.04f))
            val alpha = (1f - (pageOffset.coerceIn(0f, 1f) * 0.25f))

            NoticeBannerCard(
                notice = notice,
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
                        targetValue = if (selected) 20.dp else 6.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun NoticeBannerCard(
    notice: Notice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrgent = notice.urgentFlag || notice.categoryEnum == NoticeCategory.TEACHER_ABSENT
    val containerColor = if (isUrgent) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    }
    val contentColor = if (isUrgent) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
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
    }
}

@Composable
fun SyncStatusPill(
    syncStatus: SyncStatus,
    onClick: () -> Unit
) {
    val semantic = LocalAttendanceColors.current
    val (icon, bg, fg) = when (syncStatus) {
        SyncStatus.SYNCED -> Triple(Icons.Rounded.CloudDone, semantic.presentContainer, semantic.onPresentContainer)
        SyncStatus.SYNCING -> Triple(Icons.Rounded.CloudSync, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        SyncStatus.OFFLINE_CACHED -> Triple(Icons.Rounded.CloudOff, semantic.poyaContainer, semantic.onPoyaContainer)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
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
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = syncStatus.label,
                style = MaterialTheme.typography.labelSmall,
                color = fg
            )
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
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = "Attendance streak",
                            tint = if (streak > 0) semantic.poya else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${streak}d",
                            style = MaterialTheme.typography.titleMedium,
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
                weeklySummary.forEach { day ->
                    val (bg, fg, symbol) = when {
                        day.holidayLabel != null -> Triple(semantic.poyaContainer, semantic.onPoyaContainer, "P")
                        day.status == AttendanceStatus.PRESENT -> Triple(semantic.presentContainer, semantic.present, "✓")
                        day.status == AttendanceStatus.ABSENT -> Triple(semantic.absentContainer, semantic.absent, "✕")
                        day.status == AttendanceStatus.MEDICAL -> Triple(semantic.medicalContainer, semantic.medical, "M")
                        day.status == AttendanceStatus.SCHOOL_EVENT -> Triple(semantic.schoolEventContainer, semantic.schoolEvent, "E")
                        day.status == AttendanceStatus.HOLIDAY -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "H")
                        else -> Triple(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurfaceVariant, "·")
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDayClick(day.dateKey) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = day.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                            Text(
                                text = symbol,
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
    onDismiss: () -> Unit,
    onSave: (status: AttendanceStatus, subjectTags: List<String>, note: String?) -> Unit
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
                Column(modifier = Modifier.padding(22.dp)) {
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
                        (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(150))
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

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LiquidGlassButton(
                                        onClick = { step = CheckInStep.YES_DETAILS },
                                        backdrop = dialogBackdrop,
                                        tint = semantic.present,
                                        contentColor = semantic.onPresentContainer,
                                        minHeight = 60.dp,
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
                                        minHeight = 60.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("checkin_no_button")
                                    ) {
                                        Icon(Icons.Rounded.EventBusy, contentDescription = null, tint = semantic.absent)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("No", style = MaterialTheme.typography.titleMedium)
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

                                Spacer(modifier = Modifier.height(16.dp))
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
                                                selectedNoStatus,
                                                selectedSubjects.toList(),
                                                note.trim().ifEmpty { null }
                                            )
                                        },
                                        backdrop = dialogBackdrop,
                                        enabled = !isSaving,
                                        tint = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        minHeight = 48.dp,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .testTag("checkin_confirm_no_button")
                                    ) { Text("Save ${selectedNoStatus.shortLabel}") }
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
