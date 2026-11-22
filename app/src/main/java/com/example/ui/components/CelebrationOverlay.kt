package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
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
    val color: Color,
    val isRibbon: Boolean,
    val spinDeg: Float
)

/**
 * Celebratory particle burst (mixed dots & spinning ribbons) + spring-popping floating banner +
 * subtle icon chime rotation & haptic pulse for Perfect 5/5 Weeks, 10/20/50-Day Streaks, and
 * 80% Eligibility Recovery moments.
 */
@Composable
fun CelebrationOverlay(
    event: CelebrationEvent?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val burstProgress = remember(event) { Animatable(0f) }
    val iconChimeRotation = rememberChimeRotation(triggerKey = event)

    val particles = remember(event) {
        val colors = listOf(RoyalGold, StatusGreen, Color(0xFF60A5FA), Color(0xFFF472B6), Color.White)
        List(32) { idx ->
            val angle = (idx * 137.5f) * (Math.PI.toFloat() / 180f)
            ConfettiPiece(
                angleRad = angle,
                speed = 0.45f + ((idx % 5) * 0.16f),
                sizeDp = 4f + (idx % 4) * 1.8f,
                color = colors[idx % colors.size],
                isRibbon = idx % 2 == 0,
                spinDeg = if (idx % 3 == 0) 280f else -240f
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
        enter = fadeIn(tween(220)) +
            slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                initialOffsetY = { -it / 2 }
            ) +
            scaleIn(
                initialScale = 0.88f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        exit = fadeOut(tween(240)) +
            slideOutVertically(tween(240), targetOffsetY = { -it / 2 }) +
            scaleOut(targetScale = 0.94f, animationSpec = tween(220)),
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
                CelebrationType.ELIGIBILITY_RECOVERY -> Icons.AutoMirrored.Rounded.TrendingUp
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("celebration_banner")
            ) {
                // Confetti particle burst behind banner (dots + spinning ribbons)
                Canvas(
                    modifier = Modifier.matchParentSize()
                ) {
                    val p = burstProgress.value
                    val fade = (1f - p).coerceIn(0f, 1f)
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.5f
                    val maxRadius = size.width * 0.50f
                    particles.forEach { piece ->
                        val dist = maxRadius * piece.speed * p
                        val x = cx + cos(piece.angleRad) * dist
                        val y = cy + sin(piece.angleRad) * dist * 0.68f + (p * p * 38.dp.toPx())
                        val pieceColor = piece.color.copy(alpha = fade * 0.92f)
                        if (piece.isRibbon) {
                            val w = piece.sizeDp.dp.toPx() * 1.45f
                            val h = piece.sizeDp.dp.toPx() * 0.65f
                            rotate(degrees = piece.spinDeg * p, pivot = Offset(x, y)) {
                                drawRoundRect(
                                    color = pieceColor,
                                    topLeft = Offset(x - w / 2f, y - h / 2f),
                                    size = Size(w, h),
                                    cornerRadius = CornerRadius(h / 2f, h / 2f)
                                )
                            }
                        } else {
                            drawCircle(
                                color = pieceColor,
                                radius = piece.sizeDp.dp.toPx() * (0.5f + 0.5f * fade),
                                center = Offset(x, y)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0E2144),
                    border = BorderStroke(1.5.dp, accent.copy(alpha = 0.78f)),
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
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(22.dp)
                                    .graphicsLayer { rotationZ = iconChimeRotation }
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
