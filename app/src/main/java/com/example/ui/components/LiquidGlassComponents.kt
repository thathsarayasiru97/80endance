package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.example.data.AmbientBackdropPreset
import com.example.ui.theme.LocalIsOledBlack
import com.example.ui.theme.LocalLiquidGlassStyle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Provides a non-recursive ambient [Backdrop] captured from the atmospheric background layer
 * so [LiquidGlassButton], [LiquidBottomTabs], [LiquidSlider], and [LiquidToggle] instances
 * anywhere in the screen hierarchy can sample and refract light without capturing themselves.
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
 * Captures a customizable ambient light-mesh background into a [Backdrop]
 * and provides it via [LocalGlassBackdrop] to foreground content.
 *
 * Supports 5 curated [AmbientBackdropPreset] styles plus optional gentle breathing motion
 * while automatically keeping True Black AMOLED mode ultra-subtle.
 */
@Composable
private fun rememberAmbientDriftPhaseState(shouldAnimate: Boolean): State<Float> {
    if (!shouldAnimate) {
        return remember { mutableFloatStateOf(0f) }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_mesh_motion")
    return infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_drift_phase"
    )
}

@Composable
fun LiquidGlassBackdropHost(
    modifier: Modifier = Modifier,
    backdropPresetOverride: AmbientBackdropPreset? = null,
    content: @Composable BoxScope.(ambientBackdrop: Backdrop) -> Unit
) {
    val ambientBackdrop = rememberLayerBackdrop()
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val isOled = LocalIsOledBlack.current
    val glassStyle = LocalLiquidGlassStyle.current
    val activePreset = backdropPresetOverride ?: glassStyle.ambientBackdropPreset

    val shouldAnimate = glassStyle.ambientMotionEnabled &&
        !isOled &&
        activePreset != AmbientBackdropPreset.PURE_MINIMAL

    // Read strictly inside Canvas draw scope (never during composition) to keep 120Hz zero-recomposition
    val driftPhaseState = rememberAmbientDriftPhaseState(shouldAnimate)

    val (orb1Base, orb2Base, orb3Base) = when (activePreset) {
        AmbientBackdropPreset.INDIGO_AURORA -> Triple(
            colorScheme.primary,
            colorScheme.secondary,
            colorScheme.tertiary
        )
        AmbientBackdropPreset.SUNSET_SAFFRON -> Triple(
            Color(0xFFF59E0B),
            Color(0xFFFB7185),
            Color(0xFF6366F1)
        )
        AmbientBackdropPreset.EMERALD_LAGOON -> Triple(
            Color(0xFF10B981),
            Color(0xFF06B6D4),
            Color(0xFF3B82F6)
        )
        AmbientBackdropPreset.MIDNIGHT_PRISM -> Triple(
            Color(0xFF8B5CF6),
            Color(0xFFEC4899),
            Color(0xFF38BDF8)
        )
        AmbientBackdropPreset.PURE_MINIMAL -> Triple(
            Color.Transparent,
            Color.Transparent,
            Color.Transparent
        )
    }

    val intensityBoost = lerp(0.82f, 1.18f, glassStyle.clampedIntensity)
    val primaryGlow = remember(orb1Base, isOled, isDark, intensityBoost) {
        orb1Base.copy(
            alpha = (when {
                isOled -> 0.08f
                isDark -> 0.24f
                else -> 0.16f
            } * intensityBoost).coerceAtMost(0.36f)
        )
    }
    val secondaryGlow = remember(orb2Base, isOled, isDark, intensityBoost) {
        orb2Base.copy(
            alpha = (when {
                isOled -> 0.05f
                isDark -> 0.19f
                else -> 0.14f
            } * intensityBoost).coerceAtMost(0.30f)
        )
    }
    val tertiaryGlow = remember(orb3Base, isOled, isDark, intensityBoost) {
        orb3Base.copy(
            alpha = (when {
                isOled -> 0.04f
                isDark -> 0.16f
                else -> 0.11f
            } * intensityBoost).coerceAtMost(0.26f)
        )
    }
    val bgColor = colorScheme.background

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 0: Atmospheric background captured into ambientBackdrop (sibling to foreground UI)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(ambientBackdrop)
        ) {
            drawRect(color = bgColor)

            if (activePreset != AmbientBackdropPreset.PURE_MINIMAL) {
                val effectivePhase = driftPhaseState.value
                val dx1 = cos(effectivePhase) * size.width * 0.04f
                val dy1 = sin(effectivePhase) * size.height * 0.03f
                val dx2 = sin(effectivePhase * 0.85f) * size.width * 0.045f
                val dy2 = cos(effectivePhase * 0.85f) * size.height * 0.035f
                val dx3 = cos(effectivePhase * 0.7f + 1.4f) * size.width * 0.04f
                val dy3 = sin(effectivePhase * 0.7f + 1.4f) * size.height * 0.03f

                val c1 = Offset(size.width * 0.14f + dx1, size.height * 0.12f + dy1)
                val r1 = size.minDimension * 0.72f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryGlow, Color.Transparent),
                        center = c1,
                        radius = r1
                    ),
                    center = c1,
                    radius = r1
                )

                val c2 = Offset(size.width * 0.88f + dx2, size.height * 0.36f + dy2)
                val r2 = size.minDimension * 0.65f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondaryGlow, Color.Transparent),
                        center = c2,
                        radius = r2
                    ),
                    center = c2,
                    radius = r2
                )

                val c3 = Offset(size.width * 0.32f + dx3, size.height * 0.86f + dy3)
                val r3 = size.minDimension * 0.68f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tertiaryGlow, Color.Transparent),
                        center = c3,
                        radius = r3
                    ),
                    center = c3,
                    radius = r3
                )
            }
        }

        // Layer 1+: Foreground UI with ambientBackdrop provided via CompositionLocal
        CompositionLocalProvider(LocalGlassBackdrop provides ambientBackdrop) {
            content(ambientBackdrop)
        }
    }
}

/**
 * Dual-mode Hero Card: when [LocalLiquidGlassStyle.current.frostedHeroCardsEnabled] (or [forceFrosted])
 * is enabled and a [Backdrop] is available, renders a refractive frosted Liquid Glass surface with a
 * specular hairline rim; otherwise renders a clean Material 3 elevated container.
 */
@Composable
fun FrostedHeroCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    tint: Color = Color.Unspecified,
    forceFrosted: Boolean = false,
    backdrop: Backdrop? = LocalGlassBackdrop.current,
    content: @Composable BoxScope.() -> Unit
) {
    val glassStyle = LocalLiquidGlassStyle.current
    val activeBackdrop = if (forceFrosted || glassStyle.frostedHeroCardsEnabled) backdrop else null
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()
    val intensity = glassStyle.clampedIntensity

    if (activeBackdrop == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = 2.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth(), content = content)
        }
        return
    }

    val blurDp = lerp(6f, 20f, intensity)
    val lensHeightDp = lerp(6f, 14f, intensity)
    val lensAmountDp = lerp(10f, 22f, intensity)
    val veilAlpha = if (isDark) {
        lerp(0.10f, 0.24f, intensity)
    } else {
        lerp(0.46f, 0.72f, intensity)
    }
    val rimBrush = remember(isDark, intensity) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) lerp(0.28f, 0.48f, intensity) else lerp(0.75f, 0.95f, intensity)),
                Color.White.copy(alpha = if (isDark) 0.08f else 0.25f),
                Color.White.copy(alpha = if (isDark) 0.20f else 0.55f)
            )
        )
    }

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = activeBackdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(blurDp.dp.toPx())
                    lens(
                        refractionHeight = lensHeightDp.dp.toPx(),
                        refractionAmount = lensAmountDp.dp.toPx(),
                        chromaticAberration = intensity > 0.35f
                    )
                },
                highlight = { Highlight.Default.copy(alpha = lerp(0.45f, 0.9f, intensity)) },
                shadow = { Shadow(radius = 8.dp, color = Color.Black.copy(alpha = if (isDark) 0.24f else 0.08f)) },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint.copy(alpha = if (isDark) 0.16f else 0.10f))
                    }
                    drawRect(
                        if (isDark) Color(0xFF141B2D).copy(alpha = veilAlpha)
                        else Color.White.copy(alpha = veilAlpha)
                    )
                }
            )
            .border(width = 1.dp, brush = rimBrush, shape = shape)
            .clip(shape),
        content = content
    )
}

/**
 * Kyant Backdrop `LiquidButton` implementation adapted for 80endance's [LiquidGlassButton] API.
 *
 * Uses `vibrancy()`, `blur(2.dp)`, `lens(12.dp, 24.dp)`, `InteractiveHighlight`, `tanh` magnetic
 * touch pull, directional drag stretch, and `BlendMode.Hue` surface tinting.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalGlassBackdrop.current,
    enabled: Boolean = true,
    tint: Color? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = Capsule(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    minHeight: Dp = 48.dp,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()
    val glassIntensity = LocalLiquidGlassStyle.current.clampedIntensity
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val effectiveTint = tint ?: Color.Unspecified
    val defaultSurfaceColor = if (isDark) {
        Color.White.copy(alpha = if (enabled) lerp(0.08f, 0.22f, glassIntensity) else 0.06f)
    } else {
        Color.White.copy(alpha = if (enabled) lerp(0.36f, 0.64f, glassIntensity) else 0.25f)
    }
    val effectiveContentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.45f)

    val backdropModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(lerp(1.5f, 8f, glassIntensity).dp.toPx())
                lens(
                    lerp(8f, 15f, glassIntensity).dp.toPx(),
                    lerp(16f, 28f, glassIntensity).dp.toPx()
                )
            },
            highlight = { Highlight.Default },
            shadow = { Shadow.Default },
            layerBlock = if (enabled) {
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
                if (effectiveTint.isSpecified) {
                    drawRect(effectiveTint, blendMode = BlendMode.Hue)
                    drawRect(effectiveTint.copy(alpha = if (isDark) 0.34f else 0.24f))
                }
                drawRect(defaultSurfaceColor)
            }
        )
    } else {
        Modifier
            .clip(shape)
            .background(defaultSurfaceColor)
    }

    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = minHeight)
            .then(backdropModifier)
            .clickable(
                interactionSource = null,
                indication = if (enabled) null else LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (enabled) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides effectiveContentColor) {
            ProvideTextStyle(
                value = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = effectiveContentColor
                )
            ) {
                content()
            }
        }
    }
}

/**
 * Floating Bottom Navigation Bar powered directly by Kyant's [LiquidBottomTabs] and [LiquidBottomTab].
 *
 * Uses dual-layer `rememberCombinedBackdrop(backdrop, tabsBackdrop)` with real SDF
 * `lens(10.dp * progress, 14.dp * progress, chromaticAberration = true)`, `InnerShadow`,
 * `Shadow`, `Highlight.Default`, `InteractiveHighlight`, and `DampedDragAnimation`.
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    val safeSelected = selectedIndex.coerceIn(0, items.lastIndex)
    val baseContentColor = if (isDark) {
        Color.White.copy(alpha = 0.88f)
    } else {
        Color(0xFF1C1C1E).copy(alpha = 0.86f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidBottomTabs(
            selectedTabIndex = { safeSelected },
            onTabSelected = onItemSelected,
            backdrop = backdrop,
            tabsCount = items.size,
            accentColorOverride = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == safeSelected
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.12f else 0.96f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "bottom_tab_icon_scale_$index"
                )
                val iconLiftPx by animateFloatAsState(
                    targetValue = if (isSelected) -2.5f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "bottom_tab_icon_lift_$index"
                )
                LiquidBottomTab(
                    onClick = {
                        if (index != safeSelected) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onItemSelected(index)
                    },
                    modifier = Modifier.testTag(item.testTag)
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = baseContentColor,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                                translationY = iconLiftPx
                            }
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = baseContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
    val glassIntensity = LocalLiquidGlassStyle.current.clampedIntensity
    val railShape = RoundedCornerShape(32.dp)

    val surfaceVeil = if (isDark) {
        Color.White.copy(alpha = lerp(0.07f, 0.20f, glassIntensity))
    } else {
        Color.White.copy(alpha = lerp(0.28f, 0.58f, glassIntensity))
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
                        blur(lerp(8f, 22f, glassIntensity).dp.toPx())
                        lens(
                            refractionHeight = lerp(10f, 18f, glassIntensity).dp.toPx(),
                            refractionAmount = lerp(16f, 28f, glassIntensity).dp.toPx(),
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
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pillScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.93f
            selected -> 1.03f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_tab_scale"
    )

    val activePillBg by animateColorAsState(
        targetValue = when {
            selected && isDark -> Color.White.copy(alpha = 0.22f)
            selected && !isDark -> Color.White.copy(alpha = 0.72f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 220),
        label = "nav_tab_pill_bg"
    )

    val activePillBorder by animateColorAsState(
        targetValue = when {
            selected && isDark -> Color.White.copy(alpha = 0.42f)
            selected && !isDark -> Color.White.copy(alpha = 0.95f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 220),
        label = "nav_tab_pill_border"
    )

    val contentTint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.76f else 0.72f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "nav_tab_tint"
    )

    val pillShape = RoundedCornerShape(26.dp)

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
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint = contentTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
