package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.example.ui.theme.LocalLiquidGlassStyle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Optimized Kyant Backdrop catalog `LiquidBottomTabs` implementation.
 *
 * Free-sliding physics:
 * - While your finger is dragging, the liquid-glass pill follows your exact fractional finger position
 *   (`0.00f .. (tabsCount - 1).toFloat()`) with zero mid-drag snapping or tab-switch preemption!
 * - `onTabSelected` is ONLY invoked when you release your finger (`onDragStopped`), at which point
 *   the pill settles into the nearest tab slot with damped liquid spring physics.
 * - Tapping any tab item directly also glides the pill smoothly from its current slot to the tapped slot.
 */
@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    accentColorOverride: Color = Color.Unspecified,
    barHeight: Dp = 64.dp,
    pillHeight: Dp = 56.dp,
    enableBarEffects: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()
    val isLightTheme = !isDark
    val glassIntensity = LocalLiquidGlassStyle.current.clampedIntensity
    val accentColor = when {
        accentColorOverride.isSpecified -> accentColorOverride
        isLightTheme -> Color(0xFF0088FF)
        else -> Color(0xFF0091FF)
    }
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(lerp(0.28f, 0.62f, glassIntensity))
        else Color(0xFF121212).copy(lerp(0.32f, 0.64f, glassIntensity))

    val haptic = LocalHapticFeedback.current
    val latestSelectedTabIndex by rememberUpdatedState(selectedTabIndex)
    val latestOnTabSelected by rememberUpdatedState(onTabSelected)

    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val maxSlot = (tabsCount - 1).coerceAtLeast(0)
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount.coerceAtLeast(1)
        }.coerceAtLeast(1f)
        val latestTabWidth by rememberUpdatedState(tabWidth)

        val offsetAnimation = remember { Animatable(0f) }
        var isPanelDragging by remember { mutableStateOf(false) }
        var rawPanelOffset by remember { mutableFloatStateOf(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val maxW = constraints.maxWidth.coerceAtLeast(1)
                val raw = if (isPanelDragging) rawPanelOffset else offsetAnimation.value
                val fraction = (raw / maxW).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val latestIsLtr by rememberUpdatedState(isLtr)
        val animationScope = rememberCoroutineScope()

        var currentIndex by remember {
            mutableIntStateOf(latestSelectedTabIndex().coerceIn(0, maxSlot))
        }
        var isUserDragging by remember { mutableStateOf(false) }
        var lastHoverSlot by remember { mutableIntStateOf(currentIndex) }

        val dampedDragAnimation = remember(animationScope, tabsCount) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = currentIndex.toFloat(),
                valueRange = 0f..maxSlot.toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 76f / 56f,
                onDragStarted = {
                    isUserDragging = true
                    isPanelDragging = true
                    rawPanelOffset = 0f
                    lastHoverSlot = targetValue.fastRoundToInt().fastCoerceIn(0, maxSlot)
                },
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, maxSlot)
                    val releaseOffset = rawPanelOffset
                    isUserDragging = false
                    isPanelDragging = false
                    rawPanelOffset = 0f
                    currentIndex = targetIndex
                    // Settle into the nearest tab slot with liquid spring physics
                    animateToValue(targetIndex.toFloat())
                    latestOnTabSelected(targetIndex)
                    animationScope.launch {
                        offsetAnimation.snapTo(releaseOffset)
                        offsetAnimation.animateTo(
                            0f,
                            spring(1f, 300f, 0.5f)
                        )
                    }
                },
                onDragCancelled = {
                    val releaseOffset = rawPanelOffset
                    isUserDragging = false
                    isPanelDragging = false
                    rawPanelOffset = 0f
                    val restIndex = latestSelectedTabIndex().coerceIn(0, maxSlot)
                    currentIndex = restIndex
                    animateToValue(restIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.snapTo(releaseOffset)
                        offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                    }
                },
                onDrag = { _, dragAmount ->
                    val w = latestTabWidth
                    val nextVal = (targetValue + dragAmount.x / w * if (latestIsLtr) 1f else -1f)
                        .fastCoerceIn(0f, maxSlot.toFloat())
                    // Free continuous sliding while finger is down — DO NOT call latestOnTabSelected mid-drag!
                    updateValue(nextVal)
                    val hoverSlot = nextVal.fastRoundToInt().fastCoerceIn(0, maxSlot)
                    if (hoverSlot != lastHoverSlot) {
                        lastHoverSlot = hoverSlot
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    rawPanelOffset += dragAmount.x
                }
            )
        }

        // Only animate to external selectedTabIndex when the user is NOT actively dragging the pill
        val externalSelectedIndex = latestSelectedTabIndex().coerceIn(0, maxSlot)
        LaunchedEffect(externalSelectedIndex, isUserDragging) {
            if (!isUserDragging) {
                currentIndex = externalSelectedIndex
                lastHoverSlot = externalSelectedIndex
                if (abs(dampedDragAnimation.targetValue - externalSelectedIndex.toFloat()) > 0.001f ||
                    abs(dampedDragAnimation.value - externalSelectedIndex.toFloat()) > 0.005f
                ) {
                    dampedDragAnimation.animateToValue(externalSelectedIndex.toFloat())
                } else {
                    dampedDragAnimation.ensureReleased()
                }
            }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    val w = latestTabWidth
                    Offset(
                        if (latestIsLtr) (dampedDragAnimation.value + 0.5f) * w + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * w + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        Row(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        if (enableBarEffects) {
                            vibrancy()
                            blur(lerp(4f, 16f, glassIntensity).dp.toPx())
                            lens(
                                lerp(14f, 24f, glassIntensity).dp.toPx(),
                                lerp(14f, 24f, glassIntensity).dp.toPx()
                            )
                        }
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 12f.dp.toPx() / size.width.coerceAtLeast(1f), progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(interactiveHighlight.modifier)
                .height(barHeight)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(1f, 1.18f, dampedDragAnimation.pressProgress)
            }
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            if (enableBarEffects) {
                                vibrancy()
                                blur(8f.dp.toPx())
                            }
                            if (progress > 0.01f) {
                                lens(
                                    20f.dp.toPx() * progress,
                                    20f.dp.toPx() * progress
                                )
                            }
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(pillHeight)
                    .fillMaxWidth()
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    val w = latestTabWidth
                    translationX =
                        if (latestIsLtr) dampedDragAnimation.value * w + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * w + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = combinedBackdrop,
                    shape = { Capsule() },
                    effects = {
                        val lensFactor = lerp(0.32f, 1f, dampedDragAnimation.pressProgress)
                        lens(
                            10f.dp.toPx() * lensFactor,
                            14f.dp.toPx() * lensFactor,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = lerp(0.38f, 1f, dampedDragAnimation.pressProgress)
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = {
                        val progress = lerp(0.28f, 1f, dampedDragAnimation.pressProgress)
                        Shadow(alpha = progress)
                    },
                    innerShadow = {
                        val progress = lerp(0.32f, 1f, dampedDragAnimation.pressProgress)
                        InnerShadow(
                            radius = 8f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.72f).fastCoerceIn(-0.22f, 0.22f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.20f, 0.20f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            if (isLightTheme) Color.Black.copy(0.09f)
                            else Color.White.copy(0.11f),
                            alpha = 1f - progress * 0.85f
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    }
                )
                .height(pillHeight)
                .fillMaxWidth(1f / tabsCount.coerceAtLeast(1))
        )
    }
}

/**
 * Kyant Backdrop catalog `LiquidButton` implementation.
 */
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope
        )
    }

    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(12f.dp.toPx(), 24f.dp.toPx())
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width.coerceAtLeast(1f)
                        val height = size.height.coerceAtLeast(1f)

                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4f.dp.toPx() / height, progress)

                        val maxOffset = size.minDimension.coerceAtLeast(1f)
                        val initialDerivative = 0.08f
                        val rawOffset = interactiveHighlight.offset
                        val relX = (rawOffset.x - width * 0.5f) * progress
                        val relY = (rawOffset.y - height * 0.5f) * progress
                        translationX = maxOffset * tanh(initialDerivative * relX / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * relY / maxOffset)

                        val maxDragScale = 4f.dp.toPx() / height
                        val offsetAngle = atan2(relY, relX)
                        val maxDim = size.maxDimension.coerceAtLeast(1f)
                        scaleX =
                            scale +
                                maxDragScale * abs(cos(offsetAngle) * relX / maxDim) *
                                (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                maxDragScale * abs(sin(offsetAngle) * relY / maxDim) *
                                (height / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                }
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .height(48f.dp)
            .padding(horizontal = 16f.dp),
        horizontalArrangement = Arrangement.spacedBy(8f.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * Optimized Kyant Backdrop catalog `LiquidSlider` implementation.
 *
 * Free-sliding physics:
 * - While your finger is dragging, the liquid-glass capsule thumb moves continuously with zero
 *   integer-stepping jumps.
 * - If [snapToStepsOnRelease] is true (e.g., for whole school days in the What-If simulator),
 *   when you release your finger the thumb settles smoothly into the nearest step with liquid spring physics.
 */
@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    accentColorOverride: Color = Color.Unspecified,
    snapToStepsOnRelease: Boolean = true
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()
    val isLightTheme = !isDark
    val accentColor = when {
        accentColorOverride.isSpecified -> accentColorOverride
        isLightTheme -> Color(0xFF0088FF)
        else -> Color(0xFF0091FF)
    }
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val haptic = LocalHapticFeedback.current
    val latestValue by rememberUpdatedState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(36.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth.coerceAtLeast(1)
        val latestTrackWidth by rememberUpdatedState(trackWidth)

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val latestIsLtr by rememberUpdatedState(isLtr)
        val animationScope = rememberCoroutineScope()
        var isUserDragging by remember { mutableStateOf(false) }
        var lastHapticStep by remember { mutableIntStateOf(latestValue().roundToInt()) }

        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = latestValue(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.48f,
                onDragStarted = {
                    isUserDragging = true
                    lastHapticStep = targetValue.roundToInt()
                },
                onDragStopped = {
                    isUserDragging = false
                    val finalValue = if (snapToStepsOnRelease) {
                        targetValue.roundToInt().toFloat().coerceIn(valueRange)
                    } else {
                        targetValue.coerceIn(valueRange)
                    }
                    animateToValue(finalValue)
                    ensureReleased()
                    latestOnValueChange(finalValue)
                },
                onDragCancelled = {
                    isUserDragging = false
                    animateToValue(latestValue().coerceIn(valueRange))
                    ensureReleased()
                },
                onDrag = { _, dragAmount ->
                    val delta = (valueRange.endInclusive - valueRange.start) * (dragAmount.x / latestTrackWidth)
                    val next = if (latestIsLtr) (targetValue + delta).coerceIn(valueRange)
                    else (targetValue - delta).coerceIn(valueRange)
                    // Continuous fractional thumb movement while dragging
                    updateValue(next)
                    val step = next.roundToInt()
                    if (step != lastHapticStep) {
                        lastHapticStep = step
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    latestOnValueChange(next)
                }
            )
        }

        val currentExternalVal = latestValue()
        LaunchedEffect(currentExternalVal, isUserDragging) {
            if (!isUserDragging) {
                if (abs(dampedDragAnimation.targetValue - currentExternalVal) > 0.001f ||
                    abs(dampedDragAnimation.value - currentExternalVal) > 0.005f
                ) {
                    dampedDragAnimation.animateToValue(currentExternalVal)
                } else {
                    dampedDragAnimation.ensureReleased()
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .pointerInput(animationScope) {
                    detectTapGestures { position ->
                        val delta = (valueRange.endInclusive - valueRange.start) * (position.x / latestTrackWidth)
                        val rawTarget =
                            (if (latestIsLtr) valueRange.start + delta
                            else valueRange.endInclusive - delta)
                                .coerceIn(valueRange)
                        val targetVal = if (snapToStepsOnRelease) {
                            rawTarget.roundToInt().toFloat().coerceIn(valueRange)
                        } else {
                            rawTarget
                        }
                        dampedDragAnimation.animateToValue(targetVal)
                        latestOnValueChange(targetVal)
                    }
                }
                .layerBackdrop(trackBackdrop)
                .padding(vertical = 10.dp)
        ) {
            Box(
                Modifier
                    .clip(Capsule())
                    .background(trackColor)
                    .height(6f.dp)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .clip(Capsule())
                    .background(accentColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        val combinedBackdrop = rememberCombinedBackdrop(
            backdrop,
            rememberBackdrop(trackBackdrop) { drawBackdrop ->
                val progress = dampedDragAnimation.pressProgress
                val sX = lerp(2f / 3f, 1f, progress)
                val sY = lerp(0f, 1f, progress)
                scale(sX, sY) {
                    drawBackdrop()
                }
            }
        )

        Box(
            Modifier
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + latestTrackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(-size.width / 4f, latestTrackWidth - size.width * 3f / 4f) * if (latestIsLtr) 1f else -1f
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = combinedBackdrop,
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.70f).fastCoerceIn(-0.20f, 0.20f)
                        scaleY *= 1f - (velocity * 0.24f).fastCoerceIn(-0.18f, 0.18f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}

/**
 * Optimized Kyant Backdrop catalog `LiquidToggle` implementation.
 *
 * Free-sliding + tap-to-toggle physics:
 * - While your finger is dragging, the glass capsule thumb slides continuously across `0f..1f`.
 * - When you release your finger after dragging, it settles into `0f` or `1f` with liquid spring physics
 *   and invokes `onSelect`.
 * - Tapping or pressing-and-holding anywhere on the switch toggles and glides the thumb with liquid refraction,
 *   always settling smoothly back to the resting pill state when released.
 */
@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop? = LocalGlassBackdrop.current,
    modifier: Modifier = Modifier
) {
    val fallbackBackdrop = rememberLayerBackdrop()
    val activeBackdrop = backdrop ?: fallbackBackdrop
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()
    val isLightTheme = !isDark
    val accentColor =
        if (isLightTheme) Color(0xFF34C759)
        else Color(0xFF30D158)
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val latestSelected by rememberUpdatedState(selected)
    val latestOnSelect by rememberUpdatedState(onSelect)

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val latestIsLtr by rememberUpdatedState(isLtr)
    val dragWidth = with(density) { 20f.dp.toPx() }
    val tapSlopPx = with(density) { 8f.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var isUserDragging by remember { mutableStateOf(false) }
    var dragDistancePx by remember { mutableFloatStateOf(0f) }
    var dragStartFraction by remember { mutableFloatStateOf(if (latestSelected()) 1f else 0f) }
    var fraction by remember { mutableFloatStateOf(if (latestSelected()) 1f else 0f) }
    var lastHalfSide by remember { mutableStateOf(latestSelected()) }

    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.48f,
            onDragStarted = {
                isUserDragging = true
                dragDistancePx = 0f
                val startFrac = if (latestSelected()) 1f else 0f
                dragStartFraction = startFrac
                fraction = startFrac
                lastHalfSide = startFrac >= 0.5f
            },
            onDragStopped = {
                val netDelta = abs(fraction - dragStartFraction)
                val dragged = dragDistancePx > tapSlopPx && netDelta > 0.16f
                isUserDragging = false
                dragDistancePx = 0f
                val prevBool = latestSelected()
                val newBool = if (dragged) {
                    targetValue >= 0.5f
                } else {
                    !prevBool
                }
                val targetFrac = if (newBool) 1f else 0f
                fraction = targetFrac
                lastHalfSide = newBool
                animateToValue(targetFrac)
                ensureReleased()
                if (newBool != prevBool) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    latestOnSelect(newBool)
                }
            },
            onDragCancelled = {
                isUserDragging = false
                dragDistancePx = 0f
                val restBool = latestSelected()
                val restFrac = if (restBool) 1f else 0f
                fraction = restFrac
                lastHalfSide = restBool
                animateToValue(restFrac)
                ensureReleased()
            },
            onDrag = { _, dragAmount ->
                dragDistancePx += abs(dragAmount.x)
                val delta = dragAmount.x / dragWidth
                fraction =
                    if (latestIsLtr) (fraction + delta).fastCoerceIn(0f, 1f)
                    else (fraction - delta).fastCoerceIn(0f, 1f)
                updateValue(fraction)
                val side = fraction >= 0.5f
                if (side != lastHalfSide) {
                    lastHalfSide = side
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        )
    }

    val currentSelectedBool = latestSelected()
    LaunchedEffect(currentSelectedBool, isUserDragging) {
        if (!isUserDragging) {
            val target = if (currentSelectedBool) 1f else 0f
            fraction = target
            if (abs(dampedDragAnimation.targetValue - target) > 0.001f ||
                abs(dampedDragAnimation.value - target) > 0.005f
            ) {
                dampedDragAnimation.animateToValue(target)
            } else {
                dampedDragAnimation.ensureReleased()
            }
        }
    }

    val trackBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = rememberCombinedBackdrop(
        activeBackdrop,
        rememberBackdrop(trackBackdrop) { drawBackdrop ->
            val progress = dampedDragAnimation.pressProgress
            val sX = lerp(2f / 3f, 0.75f, progress)
            val sY = lerp(0f, 0.75f, progress)
            scale(sX, sY) {
                drawBackdrop()
            }
        }
    )

    Box(
        modifier
            .semantics {
                role = Role.Switch
            }
            .then(dampedDragAnimation.modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(Capsule())
                .drawBehind {
                    val f = dampedDragAnimation.value
                    drawRect(lerp(trackColor, accentColor, f))
                }
                .size(64f.dp, 28f.dp)
        )

        Box(
            Modifier
                .graphicsLayer {
                    val f = dampedDragAnimation.value
                    val padding = 2f.dp.toPx()
                    translationX =
                        if (latestIsLtr) lerp(padding, padding + dragWidth, f)
                        else lerp(-padding, -(padding + dragWidth), f)
                }
                .drawBackdrop(
                    backdrop = combinedBackdrop,
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            5f.dp.toPx() * progress,
                            10f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 50f
                        scaleX /= 1f - (velocity * 0.70f).fastCoerceIn(-0.20f, 0.20f)
                        scaleY *= 1f - (velocity * 0.24f).fastCoerceIn(-0.18f, 0.18f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}
