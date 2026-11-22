package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
 * Interactive Swipe-to-Catch-Up Card Stack for clearing a backlog of unlogged school days in seconds.
 * - Swipe Right → Present
 * - Swipe Left → Absent
 * - Tap quick pill → Medical or Excused
 */
@Composable
fun SwipeCatchUpDeckDialog(
    missedDays: List<String>,
    onLogDay: (dateKey: String, status: AttendanceStatus) -> Unit,
    onDismiss: () -> Unit
) {
    val orderedDays = remember(missedDays) { missedDays.sorted() }
    var currentIndex by remember(orderedDays) { mutableIntStateOf(0) }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    if (orderedDays.isEmpty() || currentIndex >= orderedDays.size) {
        onDismiss()
        return
    }

    val currentDate = orderedDays[currentIndex]
    val remainingCount = orderedDays.size - currentIndex

    fun commitStatus(status: AttendanceStatus, swipeDir: Float = 0f) {
        scope.launch {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (swipeDir != 0f) {
                offsetX.animateTo(swipeDir * 900f, animationSpec = tween(180))
            }
            onLogDay(currentDate, status)
            offsetX.snapTo(0f)
            if (currentIndex + 1 >= orderedDays.size) {
                onDismiss()
            } else {
                currentIndex += 1
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
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
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
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("swipe_deck_close")) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                // Swipe instruction strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "← Swipe Left: Absent",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StatusRed
                    )
                    Text(
                        text = "Swipe Right: Present →",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StatusGreen
                    )
                }

                // Card Stack
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Shadow preview of the next card underneath
                    if (currentIndex + 1 < orderedDays.size) {
                        val nextDate = orderedDays[currentIndex + 1]
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .scale(0.93f)
                                .offset(y = 14.dp)
                                .alpha(0.65f),
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

                    // Active draggable card
                    val dragFraction = (offsetX.value / 260f).coerceIn(-1f, 1f)
                    val stampColor = when {
                        dragFraction > 0.15f -> StatusGreen
                        dragFraction < -0.15f -> StatusRed
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(218.dp)
                            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                            .rotate(dragFraction * 9f)
                            .pointerInput(currentDate) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        when {
                                            offsetX.value > 180f -> commitStatus(AttendanceStatus.PRESENT, 1f)
                                            offsetX.value < -180f -> commitStatus(AttendanceStatus.ABSENT, -1f)
                                            else -> scope.launch {
                                                offsetX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 400f))
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch { offsetX.animateTo(0f, spring()) }
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                                    }
                                )
                            }
                            .testTag("swipe_deck_active_card"),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF0E2144),
                        border = BorderStroke(
                            width = if (abs(dragFraction) > 0.15f) 2.5.dp else 1.2.dp,
                            color = if (abs(dragFraction) > 0.15f) stampColor else RoyalGold.copy(alpha = 0.4f)
                        ),
                        shadowElevation = 6.dp
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
                            // Swipe stamp badge
                            if (abs(dragFraction) > 0.15f) {
                                Surface(
                                    modifier = Modifier
                                        .align(if (dragFraction > 0) Alignment.TopStart else Alignment.TopEnd)
                                        .alpha(abs(dragFraction).coerceIn(0f, 1f)),
                                    color = stampColor.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.5.dp, stampColor)
                                ) {
                                    Text(
                                        text = if (dragFraction > 0) "PRESENT ✓" else "ABSENT ✕",
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
                                    text = "Did you attend school on this day?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC5D8FA),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Text(
                                text = "${currentIndex + 1} / ${orderedDays.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.6f),
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
                            .height(44.dp)
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
                            .height(44.dp)
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
