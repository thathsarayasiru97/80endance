package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAttendanceColors
import com.example.ui.theme.LocalIsOledBlack
import com.example.ui.theme.LocalLiquidGlassStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Adds a sleek, GPU-friendly diagonal specular glass sheen that gently glides across hero cards
 * every few seconds. Reads the animated progress strictly inside `drawWithContent` so it causes
 * zero recompositions at 120Hz.
 */
fun Modifier.specularSheenSweep(
    enabled: Boolean = true,
    durationMillis: Int = 5200,
    maxAlpha: Float = 0.22f
): Modifier = composed {
    val isOled = LocalIsOledBlack.current
    val glassStyle = LocalLiquidGlassStyle.current
    if (!enabled || isOled || !glassStyle.ambientMotionEnabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "specular_sheen_sweep")
    val progressState = infiniteTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheen_progress"
    )

    this.drawWithContent {
        drawContent()
        val p = progressState.value
        if (p in -0.35f..1.45f) {
            val bandWidth = size.width * 0.42f
            val x = p * size.width
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = maxAlpha),
                    Color.White.copy(alpha = maxAlpha * 0.45f),
                    Color.Transparent
                ),
                start = Offset(x - bandWidth, 0f),
                end = Offset(x + bandWidth * 0.45f, size.height)
            )
            drawRect(brush = brush)
        }
    }
}

/**
 * Tactile 3D Liquid Glass tilt that gently pitches and rolls a hero card toward the touch point
 * and springs smoothly back to flat on release without consuming child taps or scrolling.
 */
fun Modifier.interactiveGlassTilt(
    enabled: Boolean = true,
    maxTiltDegrees: Float = 3.2f
): Modifier = composed {
    if (!enabled) return@composed this
    val scope = rememberCoroutineScope()
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }

    this
        .graphicsLayer {
            rotationX = rotX.value
            rotationY = rotY.value
            cameraDistance = 16f * density
        }
        .pointerInput(enabled, maxTiltDegrees) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.coerceAtLeast(1).toFloat()
                    val h = size.height.coerceAtLeast(1).toFloat()
                    val normX = ((down.position.x / w) - 0.5f).coerceIn(-0.5f, 0.5f) * 2f
                    val normY = ((down.position.y / h) - 0.5f).coerceIn(-0.5f, 0.5f) * 2f
                    scope.launch {
                        rotY.animateTo(
                            normX * maxTiltDegrees,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                        )
                    }
                    scope.launch {
                        rotX.animateTo(
                            -normY * maxTiltDegrees,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                        )
                    }
                    waitForUpOrCancellation()
                    scope.launch {
                        rotX.animateTo(
                            0f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                    scope.launch {
                        rotY.animateTo(
                            0f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                }
            }
        }
}

/**
 * Custom Canvas-drawn animated living streak flame whose visual intensity and palette evolve
 * across streak tiers (0d ember, 1-6d golden flame, 7-14d fiery blaze, 15-29d solar flare, 30d+ prism fire).
 */
@Composable
fun LivingStreakFlameIcon(
    streak: Int,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp
) {
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val isActive = streak > 0
    val infiniteTransition = rememberInfiniteTransition(label = "living_streak_flame")
    val flickerState = if (isActive) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "flame_flicker_phase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val (outerColor, midColor, coreColor, haloColor) = when {
        streak <= 0 -> listOf(mutedColor, mutedColor, mutedColor.copy(alpha = 0.6f), Color.Transparent)
        streak < 7 -> listOf(Color(0xFFFF6D00), Color(0xFFFFAB00), Color(0xFFFFF59D), Color(0xFFFF9100))
        streak < 15 -> listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFFFF8D), Color(0xFFFF6D00))
        streak < 30 -> listOf(Color(0xFFE91E63), Color(0xFFFF6D00), Color(0xFFFFF176), Color(0xFFFF4081))
        else -> listOf(Color(0xFF6366F1), Color(0xFF06B6D4), Color(0xFFE0F7FA), Color(0xFF38BDF8))
    }

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val phase = flickerState.value
        val sway = if (isActive) sin(phase) * (w * 0.055f) else 0f
        val stretch = if (isActive) 1f + cos(phase * 2f) * 0.06f else 1f

        // Soft glowing aura behind active flames
        if (isActive) {
            val haloAlpha = 0.24f + (sin(phase) * 0.08f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(haloColor.copy(alpha = haloAlpha), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.58f),
                    radius = w * 0.62f
                ),
                center = Offset(w * 0.5f, h * 0.58f),
                radius = w * 0.62f
            )
        }

        // Outer flame silhouette
        val outerPath = Path().apply {
            val tipY = (h * 0.08f) / stretch
            moveTo(w * 0.5f + sway, tipY.coerceAtLeast(h * 0.02f))
            cubicTo(
                w * 0.86f, h * 0.34f,
                w * 0.94f, h * 0.72f,
                w * 0.5f, h * 0.94f
            )
            cubicTo(
                w * 0.06f, h * 0.72f,
                w * 0.14f, h * 0.34f,
                w * 0.5f + sway, tipY.coerceAtLeast(h * 0.02f)
            )
            close()
        }
        drawPath(
            path = outerPath,
            brush = Brush.verticalGradient(
                colors = listOf(outerColor, midColor),
                startY = h * 0.05f,
                endY = h * 0.95f
            )
        )

        // Bright inner flame tongue
        val innerPath = Path().apply {
            moveTo(w * 0.5f - sway * 0.5f, h * 0.36f)
            cubicTo(
                w * 0.72f, h * 0.54f,
                w * 0.74f, h * 0.78f,
                w * 0.5f, h * 0.90f
            )
            cubicTo(
                w * 0.26f, h * 0.78f,
                w * 0.28f, h * 0.54f,
                w * 0.5f - sway * 0.5f, h * 0.36f
            )
            close()
        }
        drawPath(
            path = innerPath,
            brush = Brush.verticalGradient(
                colors = listOf(midColor, coreColor),
                startY = h * 0.34f,
                endY = h * 0.90f
            )
        )

        // Floating rising ember spark for 7+ day streaks
        if (streak >= 7) {
            val sparkProgress = ((phase / (2.0 * PI)).toFloat())
            val sparkY = h * (0.28f - sparkProgress * 0.24f)
            val sparkX = w * 0.5f + sin(phase * 2f) * w * 0.18f
            val sparkAlpha = (1f - sparkProgress).coerceIn(0f, 1f) * 0.85f
            drawCircle(
                color = coreColor.copy(alpha = sparkAlpha),
                radius = w * 0.065f,
                center = Offset(sparkX, sparkY)
            )
        }
    }
}

/**
 * Floating "+0.4%" / "-0.6%" delta pill that drifts upward with a crisp spring pop whenever
 * a student logs or updates attendance, giving instant emotional feedback.
 */
@Composable
fun FloatingDeltaPillOverlay(
    deltaPercent: Double?,
    triggerKey: Int,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAttendanceColors.current
    var visible by remember { mutableStateOf(false) }
    val riseOffset = remember { Animatable(18f) }
    val popScale = remember { Animatable(0.75f) }

    LaunchedEffect(triggerKey) {
        if (triggerKey > 0 && deltaPercent != null && kotlin.math.abs(deltaPercent) >= 0.05) {
            visible = true
            riseOffset.snapTo(20f)
            popScale.snapTo(0.72f)
            launch {
                popScale.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                )
            }
            launch {
                riseOffset.animateTo(
                    -10f,
                    tween(durationMillis = 680, easing = FastOutSlowInEasing)
                )
            }
            delay(1850L)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible && deltaPercent != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(340)),
        modifier = modifier
    ) {
        val value = deltaPercent ?: 0.0
        val positive = value >= 0.0
        val sign = if (positive) "+" else ""
        val formatted = String.format(java.util.Locale.US, "%s%.1f%%", sign, value)
        Surface(
            color = if (positive) semantic.present else semantic.absent,
            shape = RoundedCornerShape(999.dp),
            shadowElevation = 6.dp,
            modifier = Modifier
                .graphicsLayer {
                    translationY = riseOffset.value
                    scaleX = popScale.value
                    scaleY = popScale.value
                }
                .testTag("floating_delta_pill")
        ) {
            Text(
                text = formatted,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

/**
 * Micro radial spark burst overlay triggered when cheering a classmate in the Streak Arena.
 */
@Composable
fun CheerSparkBurstOverlay(
    triggerKey: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(triggerKey) {
        if (triggerKey > 0) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing)
            )
        }
    }

    if (triggerKey > 0 && progress.value < 0.99f) {
        Canvas(modifier = modifier.size(64.dp)) {
            val p = progress.value
            val alpha = (1f - p).coerceIn(0f, 1f)
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.48f
            val sparkCount = 8
            for (i in 0 until sparkCount) {
                val angle = (i.toFloat() / sparkCount) * 2f * PI.toFloat() + 0.3f
                val dist = maxR * (0.25f + p * 0.75f)
                val cx = center.x + cos(angle) * dist
                val cy = center.y + sin(angle) * dist
                val r = (3.4.dp.toPx() * (1f - p * 0.55f)).coerceAtLeast(1f)
                drawCircle(
                    color = if (i % 2 == 0) accentColor.copy(alpha = alpha) else Color(0xFFFFD54F).copy(alpha = alpha),
                    radius = r,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

/**
 * Sleek frosted Liquid Glass loading shimmer skeleton replacing bare spinners while
 * initial profile & cloud data hydrates on launch.
 */
@Composable
fun LiquidGlassShimmerSkeleton(
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val baseColor = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f)
    }
    val highlightColor = if (isDark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.White.copy(alpha = 0.92f)
    }
    val transition = rememberInfiniteTransition(label = "glass_shimmer")
    val phase by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_phase"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(phase * 600f - 240f, 0f),
        end = Offset(phase * 600f + 240f, 240f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("liquid_glass_loading_skeleton"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header row shimmer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .width(170.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                }
            }

            // Hero gauge card shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(shimmerBrush),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(164.dp)
                        .clip(CircleShape)
                        .background(baseColor)
                )
            }

            // Quick check-in card shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(shimmerBrush)
            )

            // Weekly streak card shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(shimmerBrush)
            )
        }
    }
}
