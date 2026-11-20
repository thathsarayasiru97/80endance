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
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

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

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 0: Atmospheric background captured into ambientBackdrop (sibling to foreground UI)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(ambientBackdrop)
        ) {
            drawRect(color = colorScheme.background)

            val primaryGlow = colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.14f)
            val secondaryGlow = colorScheme.secondary.copy(alpha = if (isDark) 0.18f else 0.13f)
            val tertiaryGlow = colorScheme.tertiary.copy(alpha = if (isDark) 0.14f else 0.09f)

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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()
    val barShape = RoundedCornerShape(34.dp)

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
        Row(
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
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                LiquidGlassNavTabButton(
                    item = item,
                    selected = selected,
                    isDark = isDark,
                    onClick = { onItemSelected(index) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
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
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pillScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.93f
            selected -> 1.0f
            else -> 0.97f
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
        label = "nav_tab_content_tint"
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
