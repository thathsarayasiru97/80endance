package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AttendanceStatus
import com.example.data.DateUtils
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusOrange
import com.example.ui.theme.StatusRed
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Interactive Swipe-to-Catch-Up Card Stack for clearing a 3–5+ day backlog in seconds.
 * - Swipe Right → Present
 * - Swipe Left → Absent
 * - Tap the card → Medical
 * - Accessible action buttons below for Present / Absent / Medical / Excused + Undo Last
 *
 * Snapshots [missedDays] once when opened so real-time Firestore updates mid-swipe never reset
 * [currentIndex] or skip cards.
 */
@Composable
fun SwipeCatchUpDeckDialog(
    missedDays: List<String>,
    onLogDay: (dateKey: String, status: AttendanceStatus) -> Unit,
    onUndoLastDay: ((dateKey: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    // Snapshot the backlog for this session so live Firestore updates do not reset index mid-swipe
    val orderedDays = remember { missedDays.distinct().sorted() }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isAnimatingOut by remember { mutableStateOf(false) }
    var medicalFlashActive by remember { mutableStateOf(false) }
    val offsetX = remember { Animatable(0f) }
    val cardScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    if (orderedDays.isEmpty() || currentIndex >= orderedDays.size) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val currentDate = orderedDays[currentIndex]
    val remainingCount = orderedDays.size - currentIndex

    fun commitStatus(status: AttendanceStatus, swipeDir: Float = 0f) {
        if (isAnimatingOut) return
        val dateToLog = currentDate
        isAnimatingOut = true
        scope.launch {
            try {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (swipeDir != 0f) {
                    offsetX.animateTo(swipeDir * 900f, animationSpec = tween(160))
                } else {
                    medicalFlashActive = (status == AttendanceStatus.MEDICAL)
                    cardScale.animateTo(0.93f, animationSpec = tween(75))
                    cardScale.animateTo(1.03f, animationSpec = tween(85))
                    cardScale.snapTo(1f)
                    medicalFlashActive = false
                }
                onLogDay(dateToLog, status)
                offsetX.snapTo(0f)
                if (currentIndex + 1 >= orderedDays.size) {
                    onDismiss()
                } else {
                    currentIndex += 1
                }
            } finally {
                medicalFlashActive = false
                cardScale.snapTo(1f)
                isAnimatingOut = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("swipe_catchup_deck_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Swipe,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Quick Catch-Up Stack",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                text = "$remainingCount school day${if (remainingCount == 1) "" else "s"} left to clear",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentIndex > 0) {
                            TextButton(
                                onClick = {
                                    if (!isAnimatingOut && currentIndex > 0) {
                                        val prevDate = orderedDays[currentIndex - 1]
                                        currentIndex -= 1
                                        onUndoLastDay?.invoke(prevDate)
                                    }
                                },
                                modifier = Modifier.testTag("swipe_deck_undo_btn")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Undo,
                                    contentDescription = "Undo previous card",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.size(4.dp))
                                Text("Undo", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("swipe_deck_close")) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }
                }

                // Swipe + Tap instruction strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "← Left: Absent",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StatusRed
                    )
                    Text(
                        text = "Tap: Medical",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StatusOrange
                    )
                    Text(
                        text = "Right: Present →",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StatusGreen
                    )
                }

                // Card Stack with depth-linked background card rise physics
                val dragFraction = (offsetX.value / 260f).coerceIn(-1f, 1f)
                val absDrag = abs(dragFraction)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(234.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Shadow preview of the next card underneath rises & scales up smoothly as top card drags
                    if (currentIndex + 1 < orderedDays.size) {
                        val nextDate = orderedDays[currentIndex + 1]
                        val nextScale = 0.92f + (0.07f * absDrag)
                        val nextOffsetY = (14f - (12f * absDrag)).dp
                        val nextAlpha = (0.62f + (0.34f * absDrag)).coerceIn(0f, 1f)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(212.dp)
                                .scale(nextScale)
                                .offset(y = nextOffsetY)
                                .alpha(nextAlpha),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = DateUtils.formatReadableShort(nextDate),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Active draggable + tappable card (Right=Present, Left=Absent, Tap=Medical)
                    val stampColor = when {
                        medicalFlashActive -> StatusOrange
                        dragFraction > 0.15f -> StatusGreen
                        dragFraction < -0.15f -> StatusRed
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .offset { IntOffset(offsetX.value.roundToInt(), -(absDrag * 8f).roundToInt()) }
                            .scale(cardScale.value)
                            .rotate(dragFraction * 9f)
                            .pointerInput(currentDate) {
                                detectTapGestures(
                                    onTap = { commitStatus(AttendanceStatus.MEDICAL, 0f) }
                                )
                            }
                            .pointerInput(currentDate) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        when {
                                            offsetX.value > 165f -> commitStatus(AttendanceStatus.PRESENT, 1f)
                                            offsetX.value < -165f -> commitStatus(AttendanceStatus.ABSENT, -1f)
                                            else -> scope.launch {
                                                offsetX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 400f))
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch { offsetX.animateTo(0f, spring()) }
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        if (!isAnimatingOut) {
                                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                                        }
                                    }
                                )
                            }
                            .testTag("swipe_deck_active_card"),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF0E2144),
                        border = BorderStroke(
                            width = if (medicalFlashActive || absDrag > 0.15f) 2.5.dp else 1.2.dp,
                            color = if (medicalFlashActive || absDrag > 0.15f) stampColor else RoyalGold.copy(alpha = 0.4f)
                        ),
                        shadowElevation = (6f + absDrag * 6f).dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF142E5E), Color(0xFF0B1936))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            // Swipe or Medical tap stamp badge
                            if (medicalFlashActive || absDrag > 0.15f) {
                                Surface(
                                    modifier = Modifier
                                        .align(
                                            when {
                                                medicalFlashActive -> Alignment.TopCenter
                                                dragFraction > 0 -> Alignment.TopStart
                                                else -> Alignment.TopEnd
                                            }
                                        )
                                        .alpha(if (medicalFlashActive) 1f else absDrag.coerceIn(0f, 1f)),
                                    color = stampColor.copy(alpha = 0.24f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.5.dp, stampColor)
                                ) {
                                    Text(
                                        text = when {
                                            medicalFlashActive -> "MEDICAL +"
                                            dragFraction > 0 -> "PRESENT ✓"
                                            else -> "ABSENT ✕"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = DateUtils.formatWeekdayName(currentDate).uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalGold
                                    )
                                )
                                Text(
                                    text = DateUtils.formatReadableShort(currentDate),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Swipe right for Present, left for Absent, or tap for Medical",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC5D8FA),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Text(
                                text = "${currentIndex + 1} / ${orderedDays.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }

                // Quick tap action buttons (for accessibility + Medical/Excused options)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { commitStatus(AttendanceStatus.ABSENT, -1f) },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("swipe_deck_absent_btn")
                    ) {
                        Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Absent", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { commitStatus(AttendanceStatus.PRESENT, 1f) },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("swipe_deck_present_btn")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Present", fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { commitStatus(AttendanceStatus.MEDICAL, 0f) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("swipe_deck_medical_btn")
                    ) {
                        Icon(
                            Icons.Rounded.LocalHospital,
                            contentDescription = null,
                            tint = StatusOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(5.dp))
                        Text("Medical", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    OutlinedButton(
                        onClick = { commitStatus(AttendanceStatus.SCHOOL_EVENT, 0f) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("swipe_deck_excused_btn")
                    ) {
                        Icon(
                            Icons.Rounded.EventAvailable,
                            contentDescription = null,
                            tint = StatusBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(5.dp))
                        Text("Excused", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
