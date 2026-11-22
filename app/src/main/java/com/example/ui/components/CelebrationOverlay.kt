package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CelebrationEvent
import com.example.data.CelebrationType
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusGreen
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private data class ConfettiPiece(
    val angleRad: Float,
    val speed: Float,
    val sizeDp: Float,
    val color: Color
)

/**
 * Celebratory particle burst + crisp floating banner + subtle haptic pulse
 * for Perfect 5/5 Weeks, 10/20/50-Day Streaks, and 80% Recovery moments.
 */
@Composable
fun CelebrationOverlay(
    event: CelebrationEvent?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val burstProgress = remember(event) { Animatable(0f) }

    val particles = remember(event) {
        val colors = listOf(RoyalGold, StatusGreen, Color(0xFF60A5FA), Color(0xFFF472B6), Color.White)
        List(28) { idx ->
            val angle = (idx * 137.5f) * (Math.PI.toFloat() / 180f)
            ConfettiPiece(
                angleRad = angle,
                speed = 0.45f + ((idx % 5) * 0.16f),
                sizeDp = 4f + (idx % 4) * 1.8f,
                color = colors[idx % colors.size]
            )
        }
    }

    LaunchedEffect(event) {
        if (event != null) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            burstProgress.snapTo(0f)
            burstProgress.animateTo(1f, animationSpec = tween(1400, easing = FastOutSlowInEasing))
            delay(3200L)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = event != null,
        enter = fadeIn(tween(220)) + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut(tween(260)) + slideOutVertically(targetOffsetY = { -it / 2 }),
        modifier = modifier
    ) {
        if (event != null) {
            val accent = when (event.type) {
                CelebrationType.PERFECT_WEEK -> RoyalGold
                CelebrationType.STREAK_MILESTONE -> StatusBlue
                CelebrationType.ELIGIBILITY_RECOVERY -> StatusGreen
            }
            val icon = when (event.type) {
                CelebrationType.PERFECT_WEEK -> Icons.Rounded.EmojiEvents
                CelebrationType.STREAK_MILESTONE -> Icons.Rounded.AutoAwesome
                CelebrationType.ELIGIBILITY_RECOVERY -> Icons.Rounded.TrendingUp
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("celebration_banner")
            ) {
                // Confetti particle burst behind banner
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                ) {
                    val p = burstProgress.value
                    val fade = (1f - p).coerceIn(0f, 1f)
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.5f
                    val maxRadius = size.width * 0.48f
                    particles.forEach { piece ->
                        val dist = maxRadius * piece.speed * p
                        val x = cx + cos(piece.angleRad) * dist
                        val y = cy + sin(piece.angleRad) * dist * 0.65f + (p * p * 36.dp.toPx())
                        drawCircle(
                            color = piece.color.copy(alpha = fade * 0.9f),
                            radius = piece.sizeDp.dp.toPx() * (0.5f + 0.5f * fade),
                            center = Offset(x, y)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0E2144),
                    border = BorderStroke(1.5.dp, accent.copy(alpha = 0.75f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = accent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.padding(10.dp).size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = accent.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = event.badgeLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                            Text(
                                text = event.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD6E4FF)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss celebration",
                                tint = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
}
