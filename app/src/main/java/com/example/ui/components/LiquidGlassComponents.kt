package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.example.ui.theme.LocalIsOledBlack
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Provides a non-recursive ambient [Backdrop] captured from the atmospheric background layer
 * so [LiquidGlassButton] instances anywhere in the screen hierarchy can sample and refract
 * light without capturing themselves.
 */
val LocalGlassBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * Navigation destination model used by [LiquidGlassBottomBar] and [LiquidGlassNavigationRail].
 */
data class LiquidGlassNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

/**
 * Captures a subtle Indigo & Saffron ambient light-mesh background into a [Backdrop]
 * and provides it via [LocalGlassBackdrop] to foreground content.
 */
@Composable
fun LiquidGlassBackdropHost(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(ambientBackdrop: Backdrop) -> Unit
) {
    val ambientBackdrop = rememberLayerBackdrop()
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val isOled = LocalIsOledBlack.current

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 0: Atmospheric background captured into ambientBackdrop (sibling to foreground UI)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(ambientBackdrop)
        ) {
            drawRect(color = colorScheme.background)

            val primaryGlow = colorScheme.primary.copy(
                alpha = when {
                    isOled -> 0.08f
                    isDark -> 0.22f
                    else -> 0.14f
                }
            )
            val secondaryGlow = colorScheme.secondary.copy(
                alpha = when {
                    isOled -> 0.05f
                    isDark -> 0.18f
                    else -> 0.13f
                }
            )
            val tertiaryGlow = colorScheme.tertiary.copy(
                alpha = when {
                    isOled -> 0.04f
                    isDark -> 0.14f
                    else -> 0.09f
                }
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryGlow, Color.Transparent),
                    center = Offset(size.width * 0.14f, size.height * 0.12f),
                    radius = size.minDimension * 0.72f
                ),
                center = Offset(size.width * 0.14f, size.height * 0.12f),
                radius = size.minDimension * 0.72f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryGlow, Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.36f),
                    radius = size.minDimension * 0.65f
                ),
                center = Offset(size.width * 0.88f, size.height * 0.36f),
                radius = size.minDimension * 0.65f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tertiaryGlow, Color.Transparent),
                    center = Offset(size.width * 0.32f, size.height * 0.86f),
                    radius = size.minDimension * 0.68f
                ),
                center = Offset(size.width * 0.32f, size.height * 0.86f),
                radius = size.minDimension * 0.68f
            )
        }

        // Layer 1+: Foreground UI with ambientBackdrop provided via CompositionLocal
        CompositionLocalProvider(LocalGlassBackdrop provides ambientBackdrop) {
            content(ambientBackdrop)
        }
    }
}

/**
 * Clear crystal iOS-style Liquid Glass pill button backed by [Modifier.drawBackdrop].
 *
 * Features:
 * - Optical backdrop blur, vibrancy, and subtle SDF lens refraction.
 * - Thin translucent white specular highlight rim.
 * - Smooth spring scale-down press animation with ripple + surface flash feedback.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalGlassBackdrop.current,
    enabled: Boolean = true,
    tint: Color? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = CircleShape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    minHeight: Dp = 50.dp,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquid_glass_button_scale"
    )

    val pressOverlayAlpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.20f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "liquid_glass_button_press_alpha"
    )

    // Clear crystal glass surface veil + optional subtle semantic tint whisper
    val baseGlassSurface = if (isDark) {
        Color.White.copy(alpha = if (enabled) 0.13f else 0.06f)
    } else {
        Color.White.copy(alpha = if (enabled) 0.44f else 0.22f)
    }
    val semanticTintOverlay = tint?.copy(alpha = if (isDark) 0.24f else 0.16f) ?: Color.Transparent

    val specularBorderBrush = remember(isDark, enabled, tint) {
        val topAlpha = when {
            !enabled -> 0.22f
            isDark -> 0.50f
            else -> 0.88f
        }
        val midAlpha = if (isDark) 0.14f else 0.26f
        val bottomAlpha = if (isDark) 0.30f else 0.62f
        val accentRim = tint?.copy(alpha = if (isDark) 0.45f else 0.32f) ?: Color.White.copy(alpha = bottomAlpha)
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = topAlpha),
                Color.White.copy(alpha = midAlpha),
                accentRim
            )
        )
    }

    val effectiveContentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.45f)

    val backdropModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(12f.dp.toPx())
                lens(
                    refractionHeight = 10f.dp.toPx(),
                    refractionAmount = 18f.dp.toPx(),
                    chromaticAberration = true
                )
            },
            highlight = { Highlight.Default },
            shadow = { Shadow.Default },
            onDrawSurface = {
                drawRect(baseGlassSurface)
                if (semanticTintOverlay != Color.Transparent) {
                    drawRect(semanticTintOverlay)
                }
                if (pressOverlayAlpha > 0f) {
                    drawRect(Color.White.copy(alpha = pressOverlayAlpha))
                }
            }
        )
    } else {
        Modifier
            .clip(shape)
            .background(baseGlassSurface)
            .then(
                if (semanticTintOverlay != Color.Transparent) {
                    Modifier.background(semanticTintOverlay)
                } else {
                    Modifier
                }
            )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .defaultMinSize(minWidth = 48.dp, minHeight = minHeight)
            .then(backdropModifier)
            .border(width = 1.dp, brush = specularBorderBrush, shape = shape)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding)
    ) {
        CompositionLocalProvider(LocalContentColor provides effectiveContentColor) {
            ProvideTextStyle(
                value = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = effectiveContentColor
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    content = content
                )
            }
        }
    }
}

/**
 * Floating, pill-shaped clear crystal Liquid Glass bottom navigation bar.
 *
 * Floats above the system navigation bar with horizontal margin so scrollable screen content
 * passes visibly and refracts underneath it.
 */
@Composable
fun LiquidGlassBottomBar(
    backdrop: Backdrop,
    items: List<LiquidGlassNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val count = items.size
    val safeSelected = selectedIndex.coerceIn(0, count - 1)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()
    val barShape = RoundedCornerShape(34.dp)
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val latestOnItemSelected by rememberUpdatedState(onItemSelected)
    val primaryColor = MaterialTheme.colorScheme.primary

    // Continuous fractional tab slot position in [0f, (count - 1).toFloat()]
    val slotAnim = remember { Animatable(safeSelected.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var lastHapticSlot by remember { mutableStateOf(safeSelected) }

    // Kinematic velocity squash & stretch state
    var stretchX by remember { mutableFloatStateOf(1f) }
    var squashY by remember { mutableFloatStateOf(1f) }
    var velocityNorm by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(safeSelected, isDragging) {
        if (!isDragging) {
            lastHapticSlot = safeSelected
            slotAnim.animateTo(
                targetValue = safeSelected.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.66f,
                    stiffness = 380f
                )
            )
        }
    }

    val isBubbleActive = isDragging || slotAnim.isRunning || abs(stretchX - 1f) > 0.004f
    LaunchedEffect(isBubbleActive) {
        if (!isBubbleActive) {
            stretchX = 1f
            squashY = 1f
            velocityNorm = 0f
            return@LaunchedEffect
        }
        var prevSlot = slotAnim.value
        var lastNanos = withFrameNanos { it }
        var smoothedVel = 0f
        var defX = stretchX - 1f
        var defVelX = 0f

        while (isDragging || slotAnim.isRunning || abs(defX) > 0.003f || abs(defVelX) > 0.01f) {
            val now = withFrameNanos { it }
            val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.032f)
            lastNanos = now

            val curSlot = slotAnim.value
            val rawVel = (curSlot - prevSlot) / dt // tab slots per second
            prevSlot = curSlot

            smoothedVel += (rawVel - smoothedVel) * 0.22f
            velocityNorm = (smoothedVel / 6f).coerceIn(-1f, 1f)

            val targetDef = (abs(smoothedVel) * 0.045f).coerceIn(0f, 0.28f)
            val force = (targetDef - defX) * 980f - defVelX * 28f
            defVelX += force * dt
            defX = (defX + defVelX * dt).coerceIn(-0.08f, 0.30f)

            val sX = (1f + defX).coerceIn(0.92f, 1.28f)
            stretchX = sX
            squashY = (1f / sqrt(sX)).coerceIn(0.85f, 1.05f)
        }
        stretchX = 1f
        squashY = 1f
        velocityNorm = 0f
    }

    val pressScale by animateFloatAsState(
        targetValue = if (isDragging) 1.07f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_bubble_press_scale"
    )
    val pressIntensity by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(180),
        label = "nav_bubble_press_intensity"
    )

    val infinite = rememberInfiniteTransition(label = "nav_bubble_thin_film")
    val shimmerPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nav_shimmer_phase"
    )

    val surfaceVeil = if (isDark) {
        Color.White.copy(alpha = 0.11f)
    } else {
        Color.White.copy(alpha = 0.40f)
    }
    val rimBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) 0.48f else 0.90f),
                Color.White.copy(alpha = if (isDark) 0.14f else 0.30f),
                Color.White.copy(alpha = if (isDark) 0.28f else 0.62f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .height(68.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { barShape },
                    effects = {
                        vibrancy()
                        blur(16f.dp.toPx())
                        lens(
                            refractionHeight = 14f.dp.toPx(),
                            refractionAmount = 26f.dp.toPx(),
                            chromaticAberration = true
                        )
                    },
                    highlight = { Highlight.Default },
                    shadow = { Shadow.Default },
                    onDrawSurface = {
                        drawRect(surfaceVeil)
                    }
                )
                .border(width = 1.dp, brush = rimBrush, shape = barShape)
                .clip(barShape)
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            val density = LocalDensity.current
            val innerWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val slotWidthPx = (innerWidthPx / count.toFloat()).coerceAtLeast(1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(count, slotWidthPx) {
                        detectHorizontalDragGestures(
                            onDragStart = { startOffset ->
                                isDragging = true
                                val touchedSlot = ((startOffset.x / slotWidthPx) - 0.5f)
                                    .coerceIn(0f, (count - 1).toFloat())
                                val nearest = touchedSlot.roundToInt().coerceIn(0, count - 1)
                                lastHapticSlot = nearest
                                scope.launch {
                                    slotAnim.animateTo(
                                        targetValue = touchedSlot,
                                        animationSpec = spring(
                                            dampingRatio = 0.8f,
                                            stiffness = 700f
                                        )
                                    )
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val nextSlot = (slotAnim.value + (dragAmount / slotWidthPx))
                                    .coerceIn(-0.08f, (count - 1).toFloat() + 0.08f)
                                scope.launch { slotAnim.snapTo(nextSlot) }

                                val nearest = nextSlot.roundToInt().coerceIn(0, count - 1)
                                if (nearest != lastHapticSlot) {
                                    lastHapticSlot = nearest
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    latestOnItemSelected(nearest)
                                }
                            },
                            onDragEnd = {
                                val targetIdx = slotAnim.value.roundToInt().coerceIn(0, count - 1)
                                isDragging = false
                                if (targetIdx != safeSelected) {
                                    latestOnItemSelected(targetIdx)
                                }
                                scope.launch {
                                    slotAnim.animateTo(
                                        targetValue = targetIdx.toFloat(),
                                        animationSpec = spring(
                                            dampingRatio = 0.62f,
                                            stiffness = 380f
                                        )
                                    )
                                }
                            },
                            onDragCancel = {
                                val targetIdx = slotAnim.value.roundToInt().coerceIn(0, count - 1)
                                isDragging = false
                                scope.launch {
                                    slotAnim.animateTo(
                                        targetValue = targetIdx.toFloat(),
                                        animationSpec = spring(
                                            dampingRatio = 0.66f,
                                            stiffness = 380f
                                        )
                                    )
                                }
                            }
                        )
                    }
                    .drawBehind {
                        val itemW = size.width / count.toFloat()
                        val itemH = size.height
                        val centerSlot = slotAnim.value
                        val baseCenterX = (centerSlot + 0.5f) * itemW
                        val centerY = itemH / 2f

                        val bubbleW = ((itemW - 4.dp.toPx()) * stretchX * pressScale).coerceAtLeast(16f)
                        val bubbleH = (itemH * squashY * pressScale).coerceAtLeast(16f)
                        val topLeft = Offset(
                            x = baseCenterX - bubbleW / 2f,
                            y = centerY - bubbleH / 2f
                        )
                        val cornerR = (bubbleH / 2f).coerceAtMost(28.dp.toPx())

                        drawThinFilmLiquidBubble(
                            topLeft = topLeft,
                            bubbleSize = Size(bubbleW, bubbleH),
                            cornerRadius = CornerRadius(cornerR, cornerR),
                            baseTint = primaryColor,
                            isDark = isDark,
                            velocityNorm = velocityNorm,
                            phaseRad = shimmerPhase,
                            pressIntensity = pressIntensity
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val distance = abs(slotAnim.value - index.toFloat()).coerceIn(0f, 1f)
                        val selectedFraction = 1f - distance
                        LiquidGlassNavTabButton(
                            item = item,
                            selected = index == safeSelected,
                            isDark = isDark,
                            selectedFraction = selectedFraction,
                            drawOwnPill = false,
                            onClick = {
                                if (index != safeSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                latestOnItemSelected(index)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating vertical clear crystal Liquid Glass navigation rail for Medium/Expanded screens
 * (tablets and landscape layouts >= 600dp).
 */
@Composable
fun LiquidGlassNavigationRail(
    backdrop: Backdrop,
    items: List<LiquidGlassNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()
    val railShape = RoundedCornerShape(32.dp)

    val surfaceVeil = if (isDark) {
        Color.White.copy(alpha = 0.11f)
    } else {
        Color.White.copy(alpha = 0.40f)
    }
    val rimBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) 0.48f else 0.90f),
                Color.White.copy(alpha = if (isDark) 0.14f else 0.30f),
                Color.White.copy(alpha = if (isDark) 0.28f else 0.62f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 14.dp, top = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .width(84.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { railShape },
                    effects = {
                        vibrancy()
                        blur(16f.dp.toPx())
                        lens(
                            refractionHeight = 14f.dp.toPx(),
                            refractionAmount = 24f.dp.toPx(),
                            chromaticAberration = true
                        )
                    },
                    highlight = { Highlight.Default },
                    shadow = { Shadow.Default },
                    onDrawSurface = {
                        drawRect(surfaceVeil)
                    }
                )
                .border(width = 1.dp, brush = rimBrush, shape = railShape)
                .clip(railShape)
                .padding(horizontal = 6.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                LiquidGlassNavTabButton(
                    item = item,
                    selected = selected,
                    isDark = isDark,
                    onClick = { onItemSelected(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassNavTabButton(
    item: LiquidGlassNavItem,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedFraction: Float = if (selected) 1f else 0f,
    drawOwnPill: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val lensMagnification = lerp(0.96f, 1.06f, selectedFraction.coerceIn(0f, 1f))
    val pillScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.93f
            else -> lensMagnification
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_tab_scale"
    )

    val activePillBg by animateColorAsState(
        targetValue = when {
            !drawOwnPill -> Color.Transparent
            selected && isDark -> Color.White.copy(alpha = 0.22f)
            selected && !isDark -> Color.White.copy(alpha = 0.72f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 220),
        label = "nav_tab_pill_bg"
    )

    val activePillBorder by animateColorAsState(
        targetValue = when {
            !drawOwnPill -> Color.Transparent
            selected && isDark -> Color.White.copy(alpha = 0.42f)
            selected && !isDark -> Color.White.copy(alpha = 0.95f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 220),
        label = "nav_tab_pill_border"
    )

    val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.76f else 0.72f)
    val selectedColor = if (isDark && !drawOwnPill) Color.White else MaterialTheme.colorScheme.primary
    val contentTint = androidx.compose.ui.graphics.lerp(
        unselectedColor,
        selectedColor,
        selectedFraction.coerceIn(0f, 1f)
    )

    val pillShape = RoundedCornerShape(26.dp)
    val visuallySelected = selectedFraction >= 0.5f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = pillScale
                scaleY = pillScale
            }
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(pillShape)
            .background(activePillBg)
            .border(width = 1.dp, color = activePillBorder, shape = pillShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary),
                role = Role.Tab,
                onClick = onClick
            )
            .testTag(item.testTag)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (visuallySelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint = contentTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (visuallySelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
