package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

/**
 * Staggered spring reveal for screen cards and sections.
 * Reserves stable LazyColumn layout bounds while gliding cards upward with soft spring scale & fade.
 */
@Composable
fun StaggeredReveal(
    index: Int,
    modifier: Modifier = Modifier,
    stepDelayMs: Long = 45L,
    slideOffsetPx: Float = 36f,
    content: @Composable () -> Unit
) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val delayMs = (index.coerceIn(0, 10) * stepDelayMs)
        if (delayMs > 0L) delay(delayMs)
        revealed = true
    }

    val progress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "staggered_reveal_$index"
    )
    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "staggered_alpha_$index"
    )

    androidx.compose.foundation.layout.Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.coerceIn(0f, 1f)
            translationY = (1f - progress) * slideOffsetPx
            val s = 0.95f + (0.05f * progress)
            scaleX = s
            scaleY = s
        }
    ) {
        content()
    }
}

/**
 * Tactile spring press scale (squish & bouncy rebound) paired with an optional click handler.
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
        label = "bouncy_press_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(),
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Odometer-style vertical rolling counter for live numbers (account count, streaks, day totals).
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
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            if (targetState >= initialState) {
                (slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { it / 2 } + fadeIn(tween(180)))
                    .togetherWith(
                        slideOutVertically(tween(150)) { -it / 2 } + fadeOut(tween(140))
                    )
            } else {
                (slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { -it / 2 } + fadeIn(tween(180)))
                    .togetherWith(
                        slideOutVertically(tween(150)) { it / 2 } + fadeOut(tween(140))
                    )
            }
        },
        label = "rolling_counter",
        modifier = modifier
    ) { value ->
        Text(
            text = "$prefix$value$suffix",
            style = style,
            fontWeight = fontWeight,
            color = color
        )
    }
}

/**
 * Finite gentle chime/wiggle rotation played once when a notification banner or achievement appears.
 */
@Composable
fun rememberChimeRotation(triggerKey: Any?): Float {
    val rotation = remember(triggerKey) { Animatable(0f) }
    LaunchedEffect(triggerKey) {
        rotation.snapTo(0f)
        rotation.animateTo(16f, tween(90, easing = FastOutSlowInEasing))
        rotation.animateTo(-14f, tween(100, easing = FastOutSlowInEasing))
        rotation.animateTo(10f, tween(90, easing = FastOutSlowInEasing))
        rotation.animateTo(-6f, tween(80, easing = FastOutSlowInEasing))
        rotation.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    return rotation.value
}
