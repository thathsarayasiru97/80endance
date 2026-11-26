package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

/**
 * Rolling odometer counter for integer counts with optional prefix/suffix (streaks, votes, A/L countdown).
 */
@Composable
fun RollingCounterText(
    count: Int,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified
) {
    AnimatedOdometerText(
        text = "$prefix$count$suffix",
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight
    )
}

/**
 * Optimized 120Hz rolling odometer counter:
 * - Only wraps numeric digits in [AnimatedContent] (static punctuation like `%`, `.`, `/` renders
 *   with zero animation overhead).
 * - Uses right-aligned digit slot keys so length changes (`9%` -> `10%` -> `100%`) roll cleanly
 *   without shifting neighboring digits.
 * - Smoothly interpolates container width via [animateContentSize].
 */
@Composable
fun AnimatedOdometerText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    val resolvedStyle = if (fontWeight != null) style.copy(fontWeight = fontWeight) else style
    Row(
        modifier = modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val len = text.length
        text.forEachIndexed { index, char ->
            val slotKey = len - index
            if (char.isDigit()) {
                key(slotKey) {
                    AnimatedContent(
                        targetState = char,
                        transitionSpec = {
                            val rollingUp = targetState >= initialState
                            val enter = slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) { height -> if (rollingUp) (height * 0.45f).toInt() else -(height * 0.45f).toInt() } +
                                fadeIn(tween(130))
                            val exit = slideOutVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) { height -> if (rollingUp) -(height * 0.45f).toInt() else (height * 0.45f).toInt() } +
                                fadeOut(tween(100))
                            (enter togetherWith exit).using(SizeTransform(clip = false))
                        },
                        label = "odometer_digit_$slotKey"
                    ) { targetChar ->
                        Text(
                            text = targetChar.toString(),
                            style = resolvedStyle,
                            color = color,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            } else {
                Text(
                    text = char.toString(),
                    style = resolvedStyle,
                    color = color,
                    softWrap = false,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Shared 120Hz directional spring tab transition used across main tabs and Admin Panel tabs.
 */
@Composable
fun <T> SmoothTabContent(
    targetState: T,
    orderOf: (T) -> Int,
    modifier: Modifier = Modifier,
    label: String = "smooth_tab_transition",
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            val movingForward = orderOf(targetState) >= orderOf(initialState)
            val slideSpec = spring<androidx.compose.ui.unit.IntOffset>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
            val enter = slideInHorizontally(animationSpec = slideSpec) { width ->
                if (movingForward) width / 12 else -width / 12
            } + fadeIn(tween(180)) + scaleIn(
                initialScale = 0.985f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            val exit = slideOutHorizontally(animationSpec = slideSpec) { width ->
                if (movingForward) -width / 14 else width / 14
            } + fadeOut(tween(120))
            enter.togetherWith(exit)
        },
        label = label
    ) { tabState ->
        content(tabState)
    }
}

/**
 * Calm one-shot staggered entrance reveal for dashboard, calendar, and report sections.
 * Never replays when scrolling or recomposing.
 */
@Composable
fun StaggeredReveal(
    index: Int = 0,
    delayMillis: Int = (index.coerceIn(0, 6)) * 34,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(delayMillis <= 0) }
    LaunchedEffect(Unit) {
        if (!visible) {
            delay(delayMillis.toLong())
            visible = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "staggered_reveal"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 14f
        }
    ) {
        content()
    }
}

/**
 * Tactile 120Hz bouncy clickable modifier for cards, pills, and calendar cells.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncy_click_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Tactile 120Hz press-scale spring feedback for clickable cards and action chips without consuming click.
 */
fun Modifier.springPressScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.965f
): Modifier = composed {
    if (!enabled) return@composed this
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "spring_press_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
        }
}

/**
 * Micro-celebration scale pop triggered whenever [triggerKey] increments (e.g., cheering a classmate).
 */
@Composable
fun CheerBurstBounce(
    triggerKey: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(triggerKey) {
        if (triggerKey > 0) {
            scale.snapTo(1f)
            scale.animateTo(
                targetValue = 1.24f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
    ) {
        content()
    }
}

/**
 * Subtle damped bell/trophy chime rotation in degrees triggered whenever [triggerKey] changes.
 */
@Composable
fun rememberChimeRotation(triggerKey: Any?): Float {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(triggerKey) {
        if (triggerKey != null) {
            rotation.snapTo(0f)
            val peaks = listOf(-14f, 12f, -8f, 5f, 0f)
            for (angle in peaks) {
                rotation.animateTo(
                    targetValue = angle,
                    animationSpec = tween(durationMillis = 85)
                )
            }
        }
    }
    return rotation.value
}
