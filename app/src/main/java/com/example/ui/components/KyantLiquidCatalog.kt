package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.clickable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Pill / capsule shape equivalent to `com.kyant.shapes.Capsule()` from the Kyant Backdrop catalog.
 */
@Suppress("FunctionName")
fun Capsule() = RoundedCornerShape(percent = 50)

/**
 * CompositionLocal used by [LiquidBottomTab] to magnify the underlying tinted tab layer
 * (`tabsBackdrop`) when pressed or dragged.
 */
internal val LocalLiquidBottomTabScale = staticCompositionLocalOf { { 1f } }

@Composable
fun RowScope.LiquidBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalLiquidBottomTabScale.current
    Column(
        modifier
            .clip(Capsule())
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            },
        verticalArrangement = Arrangement.spacedBy(2f.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

/**
 * Kyant Backdrop catalog `DampedDragAnimation` controller.
 *
 * Drives the real optical liquid-glass lens pill across `LiquidBottomTabs`, `LiquidSlider`,
 * `LiquidToggle`, and `SlidableLiquidGlassSegmentedBar`, providing:
 * - `value`: continuous animated position
 * - `progress`: normalized `[0f..1f]` progress across `valueRange`
 * - `pressProgress`: `[0f..1f]` spring-animated press intensity driving `lens(..., chromaticAberration = true)`,
 *   `InnerShadow`, `Shadow`, and `Highlight`
 * - `scaleX` / `scaleY`: pressed swell scale
 * - `velocity`: real-time spring velocity driving kinematic squash-and-stretch
 */
class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    initialValue: Float,
    private val valueRange: ClosedFloatingPointRange<Float>,
    private val visibilityThreshold: Float = 0.001f,
    private val initialScale: Float = 1f,
    private val pressedScale: Float = 1.4f,
    private val onDragStarted: DampedDragAnimation.(Offset) -> Unit = {},
    private val onDragStopped: DampedDragAnimation.() -> Unit = {},
    private val onDrag: DampedDragAnimation.(size: Size, dragAmount: Offset) -> Unit = { _, _ -> }
) {
    private val valueAnim = Animatable(initialValue.coerceIn(valueRange), visibilityThreshold)
    private val pressAnim = Animatable(0f, 0.001f)

    var targetValue by mutableFloatStateOf(initialValue.coerceIn(valueRange))
        private set

    var velocity by mutableFloatStateOf(0f)
        private set

    private var lastUpdateNanos: Long = 0L
    private var valueJob: Job? = null

    val value: Float
        get() = valueAnim.value

    val progress: Float
        get() {
            val span = (valueRange.endInclusive - valueRange.start)
            return if (span <= 0f) 0f else ((value - valueRange.start) / span).fastCoerceIn(0f, 1f)
        }

    val pressProgress: Float
        get() = pressAnim.value.fastCoerceIn(0f, 1f)

    val scaleX: Float
        get() = lerp(initialScale, pressedScale, pressProgress)

    val scaleY: Float
        get() = lerp(initialScale, pressedScale, pressProgress)

    fun updateValue(newValue: Float) {
        val clamped = newValue.coerceIn(valueRange)
        val now = System.nanoTime()
        if (lastUpdateNanos != 0L) {
            val dt = ((now - lastUpdateNanos) / 1_000_000_000f).coerceIn(0.002f, 0.06f)
            val instVel = (clamped - targetValue) / dt
            velocity = velocity * 0.55f + instVel * 0.45f
        }
        lastUpdateNanos = now
        targetValue = clamped
        valueJob?.cancel()
        valueJob = animationScope.launch {
            valueAnim.animateTo(
                targetValue = clamped,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 780f,
                    visibilityThreshold = visibilityThreshold
                )
            )
            velocity = 0f
        }
    }

    fun animateToValue(newValue: Float) {
        val clamped = newValue.coerceIn(valueRange)
        targetValue = clamped
        lastUpdateNanos = 0L
        valueJob?.cancel()
        valueJob = animationScope.launch {
            // Brief optical lens pulse when gliding on tap so the glass refraction activates
            if (pressAnim.value < 0.25f) {
                launch {
                    pressAnim.animateTo(
                        targetValue = 0.65f,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f)
                    )
                    pressAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f)
                    )
                }
            }
            valueAnim.animateTo(
                targetValue = clamped,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = 380f,
                    visibilityThreshold = visibilityThreshold
                )
            ) {
                this@DampedDragAnimation.velocity = this.velocity
            }
            velocity = 0f
        }
    }

    private fun setPressed(pressed: Boolean) {
        animationScope.launch {
            pressAnim.animateTo(
                targetValue = if (pressed) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = if (pressed) 520f else 340f
                )
            )
        }
    }

    val modifier: Modifier = Modifier
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                lastUpdateNanos = System.nanoTime()
                setPressed(true)
                onDragStarted(down.position)

                var pointerId = down.id
                var released = false
                while (!released) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == pointerId }
                        ?: event.changes.firstOrNull()
                    if (change == null || change.changedToUpIgnoreConsumed() || !change.pressed) {
                        released = true
                    } else {
                        pointerId = change.id
                        val delta = change.position - change.previousPosition
                        if (delta != Offset.Zero) {
                            change.consume()
                            onDrag(
                                Size(size.width.toFloat(), size.height.toFloat()),
                                delta
                            )
                        }
                    }
                }

                lastUpdateNanos = 0L
                setPressed(false)
                onDragStopped()
            }
        }
}

/**
 * Kyant Backdrop catalog `InteractiveHighlight` controller.
 *
 * Tracks touch press progress and pointer offset to render a soft radial specular glow
 * that follows the user's finger or the sliding liquid-glass lens.
 */
class InteractiveHighlight(
    private val animationScope: CoroutineScope,
    private val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
) {
    private val pressAnim = Animatable(0f, 0.001f)
    var offset by mutableStateOf(Offset.Zero)
        private set

    val pressProgress: Float
        get() = pressAnim.value.fastCoerceIn(0f, 1f)

    val modifier: Modifier = Modifier.drawWithContent {
        drawContent()
        val p = pressProgress
        if (p > 0.005f) {
            val center = position(size, offset)
            val radius = (size.maxDimension * 0.65f).coerceAtLeast(48f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.24f * p),
                        Color.White.copy(alpha = 0.08f * p),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center,
                blendMode = BlendMode.Plus
            )
        }
    }

    val gestureModifier: Modifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            offset = down.position
            animationScope.launch {
                pressAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f)
                )
            }
            var released = false
            while (!released) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull()
                if (change == null || !change.pressed) {
                    released = true
                } else {
                    offset = change.position
                }
            }
            animationScope.launch {
                pressAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f)
                )
            }
        }
    }
}
