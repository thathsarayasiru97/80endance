package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
 * Optimized Kyant Backdrop catalog `DampedDragAnimation` controller.
 *
 * Key optimizations for smooth 60/120fps Liquid Glass rendering:
 * - During active touch dragging, `updateValue()` uses a single non-canceling coroutine or immediate
 *   `snapTo` / lightweight spring follow without cancelling and spawning a new coroutine on every 120Hz touch sample.
 * - `animateToValue()` smoothly glides the pill when a tab or track is tapped, briefly pulsing `pressProgress`
 *   so the optical SDF lens refraction activates during tap-to-glide as well as drag.
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
    private val onDragCancelled: (DampedDragAnimation.() -> Unit)? = null,
    private val onDrag: DampedDragAnimation.(size: Size, dragAmount: Offset) -> Unit = { _, _ -> }
) {
    private val valueAnim = Animatable(initialValue.coerceIn(valueRange), visibilityThreshold)
    private val pressAnim = Animatable(0f, 0.001f)

    private var dragValue by mutableFloatStateOf(initialValue.coerceIn(valueRange))
    private var useDragValue by mutableStateOf(false)

    var targetValue by mutableFloatStateOf(initialValue.coerceIn(valueRange))
        private set

    var velocity by mutableFloatStateOf(0f)
        private set

    private var lastUpdateNanos: Long = 0L
    private var valueJob: Job? = null
    private var pressJob: Job? = null
    private var isPointerDown: Boolean = false

    val value: Float
        get() = if (useDragValue) dragValue else valueAnim.value

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
            val dt = ((now - lastUpdateNanos) / 1_000_000_000f).coerceIn(0.003f, 0.06f)
            val instVel = (clamped - targetValue) / dt
            velocity = (velocity * 0.62f + instVel * 0.38f).fastCoerceIn(-18f, 18f)
        }
        lastUpdateNanos = now
        targetValue = clamped
        if (isPointerDown) {
            // Zero-allocation, zero-shutter tracking at full 120Hz touch sampling rate
            valueJob?.cancel()
            dragValue = clamped
            useDragValue = true
        } else {
            valueJob?.cancel()
            val startVal = value
            valueJob = animationScope.launch {
                if (useDragValue) {
                    valueAnim.snapTo(startVal)
                    useDragValue = false
                }
                valueAnim.animateTo(
                    targetValue = clamped,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 520f,
                        visibilityThreshold = visibilityThreshold
                    )
                ) {
                    this@DampedDragAnimation.velocity = this.velocity
                }
                velocity = 0f
            }
        }
    }

    fun animateToValue(newValue: Float) {
        val clamped = newValue.coerceIn(valueRange)
        val currentVal = value
        val distance = kotlin.math.abs(clamped - currentVal)
        targetValue = clamped
        lastUpdateNanos = 0L
        if (!isPointerDown) {
            val alreadySwollenFromHold = pressAnim.value >= 0.32f
            pressJob?.cancel()
            pressJob = animationScope.launch {
                if (distance > 0.02f && !alreadySwollenFromHold) {
                    pressAnim.animateTo(
                        targetValue = 0.68f,
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 680f)
                    )
                }
                if (!isPointerDown) {
                    pressAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.84f, stiffness = 320f)
                    )
                }
            }
        }
        valueJob?.cancel()
        valueJob = animationScope.launch {
            if (useDragValue) {
                valueAnim.snapTo(currentVal)
                useDragValue = false
            }
            valueAnim.animateTo(
                targetValue = clamped,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = 410f,
                    visibilityThreshold = visibilityThreshold
                )
            ) {
                this@DampedDragAnimation.velocity = this.velocity
            }
            velocity = 0f
            if (!isPointerDown && pressAnim.value > 0.001f && pressJob?.isActive != true) {
                pressJob = animationScope.launch {
                    pressAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.84f, stiffness = 340f)
                    )
                }
            }
        }
    }

    fun ensureReleased() {
        if (!isPointerDown && pressAnim.value > 0.001f && pressJob?.isActive != true) {
            pressJob = animationScope.launch {
                pressAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.84f, stiffness = 340f)
                )
            }
        }
    }

    private fun setPressed(pressed: Boolean) {
        isPointerDown = pressed
        pressJob?.cancel()
        pressJob = animationScope.launch {
            pressAnim.animateTo(
                targetValue = if (pressed) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = if (pressed) 550f else 340f
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

                var cancelled = false
                try {
                    var pointerId = down.id
                    var released = false
                    var totalDx = 0f
                    var totalDy = 0f
                    var horizontalLocked = false
                    val touchSlop = viewConfiguration.touchSlop
                    while (!released) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == pointerId }
                            ?: event.changes.firstOrNull()
                        if (change == null || change.changedToUpIgnoreConsumed() || !change.pressed) {
                            released = true
                        } else if (change.isConsumed && !horizontalLocked) {
                            // Parent scroll container took over this gesture — release cleanly without toggling
                            cancelled = true
                            released = true
                        } else {
                            pointerId = change.id
                            val delta = change.position - change.previousPosition
                            if (delta != Offset.Zero) {
                                totalDx += kotlin.math.abs(delta.x)
                                totalDy += kotlin.math.abs(delta.y)
                                if (!horizontalLocked) {
                                    if (totalDy > touchSlop && totalDy > totalDx * 1.4f) {
                                        // User is scrolling vertically; release glass press cleanly
                                        cancelled = true
                                        released = true
                                    } else if (totalDx > touchSlop * 0.45f) {
                                        horizontalLocked = true
                                    }
                                }
                                if (!released) {
                                    if (horizontalLocked) {
                                        change.consume()
                                    }
                                    onDrag(
                                        Size(size.width.toFloat(), size.height.toFloat()),
                                        delta
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {
                    cancelled = true
                    throw e
                } finally {
                    lastUpdateNanos = 0L
                    velocity = 0f
                    setPressed(false)
                    if (cancelled && onDragCancelled != null) {
                        onDragCancelled.invoke(this@DampedDragAnimation)
                    } else {
                        onDragStopped()
                    }
                }
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
    private var pressJob: Job? = null

    val pressProgress: Float
        get() = pressAnim.value.fastCoerceIn(0f, 1f)

    val modifier: Modifier = Modifier.drawWithContent {
        drawContent()
        val p = pressProgress
        if (p > 0.01f) {
            val center = position(size, offset)
            val radius = (size.maxDimension * 0.65f).coerceAtLeast(48f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f * p),
                        Color.White.copy(alpha = 0.07f * p),
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
            pressJob?.cancel()
            pressJob = animationScope.launch {
                pressAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.84f, stiffness = 540f)
                )
            }
            try {
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
            } finally {
                pressJob?.cancel()
                pressJob = animationScope.launch {
                    pressAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f)
                    )
                }
            }
        }
    }
}
