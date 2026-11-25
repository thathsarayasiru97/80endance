package com.example.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Look presets for the AGSL Thin-Film Soap Bubble & Refractive Glass Lens.
 * Values map to: [interferenceAmount, baseTintR, baseTintG, baseTintB, hueShift]
 */
enum class BubbleLookPreset(
    val id: Int,
    val label: String,
    val uniforms: FloatArray,
    val fallbackRimColors: List<Color>
) {
    SOAP_BUBBLE(
        id = 0,
        label = "Soap Lens",
        uniforms = floatArrayOf(1.0f, 0.45f, 0.75f, 1.0f, 2.5f),
        fallbackRimColors = listOf(
            Color(0xFF60A5FA),
            Color(0xFFF472B6),
            Color(0xFFFBBF24),
            Color(0xFF34D399),
            Color(0xFF60A5FA)
        )
    ),
    ROYAL_GOLD(
        id = 1,
        label = "Royal Gold",
        uniforms = floatArrayOf(0.35f, 1.35f, 1.05f, 0.25f, 0.8f),
        fallbackRimColors = listOf(
            Color(0xFFFDB813),
            Color(0xFFFFE082),
            Color(0xFF3B82F6),
            Color(0xFFFDB813)
        )
    ),
    EMERALD_SAFE(
        id = 2,
        label = "80% Emerald",
        uniforms = floatArrayOf(0.15f, 0.18f, 1.15f, 0.55f, 0.0f),
        fallbackRimColors = listOf(
            Color(0xFF10B981),
            Color(0xFF6EE7B7),
            Color(0xFF059669),
            Color(0xFF10B981)
        )
    ),
    CYBER_CYAN(
        id = 3,
        label = "Cyber Cyan",
        uniforms = floatArrayOf(0.15f, 0.0f, 1.55f, 1.6f, 0.0f),
        fallbackRimColors = listOf(
            Color(0xFF06B6D4),
            Color(0xFF67E8F9),
            Color(0xFF3B82F6),
            Color(0xFF06B6D4)
        )
    ),
    FIRE_STREAK(
        id = 4,
        label = "Fire Streak",
        uniforms = floatArrayOf(0.08f, 1.5f, 0.52f, 0.05f, 0.0f),
        fallbackRimColors = listOf(
            Color(0xFFF97316),
            Color(0xFFFACC15),
            Color(0xFFEF4444),
            Color(0xFFF97316)
        )
    ),
    NEON_MAGENTA(
        id = 5,
        label = "Neon Orb",
        uniforms = floatArrayOf(0.08f, 1.1f, 0.1f, 0.85f, 0.0f),
        fallbackRimColors = listOf(
            Color(0xFFEC4899),
            Color(0xFFA855F7),
            Color(0xFFF43F5E),
            Color(0xFFEC4899)
        )
    )
}

/**
 * Option model for [SlidableLiquidGlassSegmentedBar].
 */
data class LiquidGlassSegmentOption(
    val label: String,
    val testTag: String,
    val icon: ImageVector? = null,
    val tintColor: Color? = null
)

/**
 * Draws a thin-film iridescent liquid-glass pill or orb inside a [DrawScope],
 * including soap-bubble thin-film chromatic interference along the curved rim,
 * inner Fresnel sheen, and velocity-reactive specular reflection.
 */
fun DrawScope.drawThinFilmLiquidBubble(
    topLeft: Offset,
    bubbleSize: Size,
    cornerRadius: CornerRadius,
    baseTint: Color,
    isDark: Boolean,
    velocityNorm: Float = 0f,
    phaseRad: Float = 0f,
    pressIntensity: Float = 0f
) {
    if (bubbleSize.width <= 1f || bubbleSize.height <= 1f) return

    val center = Offset(
        x = topLeft.x + bubbleSize.width / 2f,
        y = topLeft.y + bubbleSize.height / 2f
    )

    // 1. Soft outer thin-film halo glow
    val glowAlpha = (if (isDark) 0.24f else 0.18f) + pressIntensity * 0.08f
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                baseTint.copy(alpha = glowAlpha),
                Color(0xFF60A5FA).copy(alpha = glowAlpha * 0.6f),
                Color.Transparent
            ),
            center = center,
            radius = maxOf(bubbleSize.width, bubbleSize.height) * 0.72f
        ),
        topLeft = topLeft,
        size = bubbleSize,
        cornerRadius = cornerRadius
    )

    // 2. Translucent liquid-glass body with Fresnel edge brightness
    val bodyTopColor = if (isDark) {
        Color.White.copy(alpha = 0.24f + pressIntensity * 0.06f)
    } else {
        Color.White.copy(alpha = 0.86f + pressIntensity * 0.08f)
    }
    val bodyMidColor = baseTint.copy(alpha = if (isDark) 0.28f else 0.22f)
    val bodyBottomColor = if (isDark) {
        baseTint.copy(alpha = 0.20f)
    } else {
        Color.White.copy(alpha = 0.68f)
    }

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(bodyTopColor, bodyMidColor, bodyBottomColor),
            startY = topLeft.y,
            endY = topLeft.y + bubbleSize.height
        ),
        topLeft = topLeft,
        size = bubbleSize,
        cornerRadius = cornerRadius
    )

    // 3. Thin-film soap-bubble interference rim (chromatic dispersion shifted by velocity + phase)
    val shiftX = cos(phaseRad + velocityNorm * 1.4f) * bubbleSize.width * 0.35f
    val shiftY = sin(phaseRad) * bubbleSize.height * 0.35f
    val iridescentRim = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.85f else 0.96f),
            Color(0xFF60A5FA).copy(alpha = if (isDark) 0.78f else 0.72f),
            baseTint.copy(alpha = 0.90f),
            Color(0xFFF472B6).copy(alpha = if (isDark) 0.72f else 0.65f),
            Color(0xFFFBBF24).copy(alpha = if (isDark) 0.70f else 0.62f),
            Color(0xFF34D399).copy(alpha = if (isDark) 0.76f else 0.68f),
            Color.White.copy(alpha = if (isDark) 0.85f else 0.96f)
        ),
        start = Offset(topLeft.x + shiftX, topLeft.y + shiftY),
        end = Offset(
            topLeft.x + bubbleSize.width - shiftX,
            topLeft.y + bubbleSize.height - shiftY
        )
    )
    drawRoundRect(
        brush = iridescentRim,
        topLeft = topLeft,
        size = bubbleSize,
        cornerRadius = cornerRadius,
        style = Stroke(width = (1.5f + pressIntensity * 0.5f).dp.toPx())
    )

    // 4. Top specular lens highlight arc (shifts opposite to motion direction like a real water droplet)
    val highlightInsetX = bubbleSize.width * 0.14f - (velocityNorm * bubbleSize.width * 0.06f)
    val highlightWidth = (bubbleSize.width * 0.72f).coerceAtLeast(4f)
    val highlightHeight = (bubbleSize.height * 0.34f).coerceAtLeast(2f)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) 0.45f else 0.78f),
                Color.White.copy(alpha = 0f)
            ),
            startY = topLeft.y + 2.dp.toPx(),
            endY = topLeft.y + 2.dp.toPx() + highlightHeight
        ),
        topLeft = Offset(
            x = (topLeft.x + highlightInsetX).coerceIn(topLeft.x + 2f, topLeft.x + bubbleSize.width - 4f),
            y = topLeft.y + 2.dp.toPx()
        ),
        size = Size(
            width = highlightWidth.coerceAtMost(bubbleSize.width - 4f),
            height = highlightHeight
        ),
        cornerRadius = CornerRadius(cornerRadius.x * 0.85f, cornerRadius.y * 0.85f)
    )
}

/**
 * Slidable Liquid-Glass Segmented Bar powered by Kyant's [LiquidBottomTabs] & [LiquidBottomTab].
 *
 * Uses dual-layer `rememberCombinedBackdrop(backdrop, tabsBackdrop)` with real SDF
 * `lens(10.dp * progress, 14.dp * progress, chromaticAberration = true)`, `InnerShadow`,
 * `Shadow`, `Highlight.Default`, `InteractiveHighlight`, and `DampedDragAnimation`.
 */
@Composable
fun SlidableLiquidGlassSegmentedBar(
    options: List<LiquidGlassSegmentOption>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 48.dp,
    defaultTint: Color = MaterialTheme.colorScheme.primary
) {
    if (options.isEmpty()) return
    val count = options.size
    val safeSelected = selectedIndex.coerceIn(0, count - 1)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()
    val fallbackBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    val ambientBackdrop = LocalGlassBackdrop.current ?: fallbackBackdrop

    val targetTint = options[safeSelected].tintColor ?: defaultTint
    val animatedTint by animateColorAsState(
        targetValue = targetTint,
        animationSpec = tween(220),
        label = "liquid_seg_tint"
    )
    val baseContentColor = if (isDark) {
        Color.White.copy(alpha = 0.88f)
    } else {
        Color(0xFF1C1C1E).copy(alpha = 0.86f)
    }

    LiquidBottomTabs(
        selectedTabIndex = { safeSelected },
        onTabSelected = onOptionSelected,
        backdrop = ambientBackdrop,
        tabsCount = count,
        accentColorOverride = animatedTint,
        barHeight = barHeight,
        pillHeight = (barHeight - 8.dp).coerceAtLeast(32.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == safeSelected
            LiquidBottomTab(
                onClick = { onOptionSelected(index) },
                modifier = Modifier.testTag(option.testTag)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    if (option.icon != null) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = baseContentColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelMedium.copy(
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
 * Interactive Liquid-Glass Slider powered directly by Kyant's [LiquidSlider].
 *
 * Captures the colored progress rail into `trackBackdrop` and combines it with `LocalGlassBackdrop`
 * via `rememberCombinedBackdrop`, so the clear glass capsule thumb (`40.dp x 24.dp`, `pressedScale = 1.5f`)
 * optically refracts and bends the track underneath with `chromaticAberration = true`.
 */
@Composable
fun LiquidGlassDropletSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..15f,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    @Suppress("UNUSED_PARAMETER") thumbBadgeText: String? = null
) {
    val fallbackBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    val ambientBackdrop = LocalGlassBackdrop.current ?: fallbackBackdrop
    val animatedAccent by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(240),
        label = "liquid_slider_accent"
    )

    LiquidSlider(
        value = { value },
        onValueChange = onValueChange,
        valueRange = valueRange,
        visibilityThreshold = 0.01f,
        backdrop = ambientBackdrop,
        accentColorOverride = animatedAccent,
        modifier = modifier
    )
}

private const val THIN_FILM_BUBBLE_SHADER_SRC = """
    uniform shader composable;
    uniform float2 touchCenter;
    uniform float radius;
    uniform float progress;
    uniform float2 deformation;
    uniform float popProgress;
    uniform float sysTime;
    uniform float interferenceAmount;
    uniform float3 baseTint;
    uniform float hueShift;

    float hash(float2 p) {
      return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
    }

    float smoothNoise(float2 p) {
      float2 i = floor(p);
      float2 f = fract(p);
      float2 u = f * f * (3.0 - 2.0 * f);
      return mix(mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
                 mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
    }

    half4 main(float2 fragCoord) {
      float THICKNESS_BASE = 300.0;
      float THICKNESS_GRAVITY = 120.0;
      float THICKNESS_SWIRL = 100.0;
      float THICKNESS_DETAIL = 40.0;
      float COLOR_INTENSITY = 2.0;
      float EDGE_FADE_END = 0.20;
      float ENV_REFLECTION_STRENGTH = 0.4;
      float ENV_BLUR_RADIUS = 50.0;

      half4 rawBackground = composable.eval(fragCoord);
      if (popProgress >= 1.0) return rawBackground;

      float2 rawUv = fragCoord - touchCenter;
      float speed = length(deformation);
      float2 moveDir = speed > 0.001 ? deformation / speed : float2(0.0, 1.0);

      float parallelDist = dot(rawUv, moveDir);
      float2 perpVector = rawUv - moveDir * parallelDist;

      float stretch = 1.0 + speed;
      float squash = 1.0 / sqrt(stretch);

      float2 uv = (moveDir * (parallelDist / stretch)) + (perpVector / squash);
      float dist = length(uv);
      float activeRadius = radius * (1.0 + popProgress * 1.5);

      if (dist >= activeRadius) {
        return rawBackground;
      }

      float2 nUv = uv / activeRadius;
      float distSq = dot(nUv, nUv);
      float z = sqrt(max(0.0, 1.0 - distSq));
      float3 normal = normalize(float3(nUv, z));
      float3 viewDir = float3(0.0, 0.0, 1.0);
      float NdotV = max(0.0, dot(normal, viewDir));

      float magnification = 0.45;
      float lensDeform = (1.0 - z) * magnification * (1.0 - popProgress);

      float2 refUvR = fragCoord - (nUv * activeRadius * (lensDeform * 0.88));
      float2 refUvG = fragCoord - (nUv * activeRadius * (lensDeform * 1.00));
      float2 refUvB = fragCoord - (nUv * activeRadius * (lensDeform * 1.12));

      half3 bgColor = half3(
        composable.eval(refUvR).r,
        composable.eval(refUvG).g,
        composable.eval(refUvB).b
      );

      float3 reflectionDir = reflect(-viewDir, normal);
      float3 lightDir1 = normalize(float3(0.6, 0.7, 0.8));
      float3 lightDir2 = normalize(float3(-0.5, -0.4, 0.6));
      float lightAlign1 = max(0.0, dot(reflectionDir, lightDir1));
      float lightAlign2 = max(0.0, dot(reflectionDir, lightDir2));

      float n_film = 1.33;
      float n_air = 1.0;
      float R0 = pow((n_film - n_air) / (n_film + n_air), 2.0);
      float fresnel = R0 + (1.0 - R0) * pow(1.0 - NdotV, 5.0);

      float sinThetaI = sqrt(max(0.0, 1.0 - NdotV * NdotV));
      float sinThetaT = sinThetaI / n_film;
      float cosThetaT = sqrt(max(0.0, 1.0 - sinThetaT * sinThetaT));

      float swirl = smoothNoise(nUv * 3.0 + sysTime * 0.12);
      float thicknessNoise = smoothNoise(nUv * 5.0 - sysTime * 0.08);
      float baseThickness = THICKNESS_BASE + nUv.y * THICKNESS_GRAVITY;
      float thickness = baseThickness
        + swirl * THICKNESS_SWIRL
        + thicknessNoise * THICKNESS_DETAIL;
      thickness = clamp(thickness, 80.0, 900.0);

      float opd = 2.0 * n_film * thickness * cosThetaT;
      float lambda_R = 650.0;
      float lambda_G = 532.0;
      float lambda_B = 450.0;

      float TWO_PI = 6.2831853;
      float oscR = 0.5 + 0.5 * cos(TWO_PI * opd / lambda_R);
      float oscG = 0.5 + 0.5 * cos(TWO_PI * opd / lambda_G);
      float oscB = 0.5 + 0.5 * cos(TWO_PI * opd / lambda_B);

      half3 interferenceColor = half3(oscR, oscG, oscB);

      if (abs(hueShift) > 0.001) {
        float Y = dot(float3(interferenceColor), float3(0.299, 0.587, 0.114));
        float I = dot(float3(interferenceColor), float3(0.596, -0.275, -0.321));
        float Q = dot(float3(interferenceColor), float3(0.212, -0.523, 0.311));
        float c = cos(hueShift);
        float s = sin(hueShift);
        float Inew = I * c - Q * s;
        float Qnew = I * s + Q * c;
        interferenceColor = half3(
          Y + 0.956 * Inew + 0.619 * Qnew,
          Y - 0.272 * Inew - 0.647 * Qnew,
          Y - 1.106 * Inew + 1.703 * Qnew
        );
      }

      half3 lookColor = mix(half3(baseTint), interferenceColor, interferenceAmount);

      float interferenceStrength = smoothstep(0.0, EDGE_FADE_END, NdotV);
      half3 filmReflection = lookColor * fresnel * COLOR_INTENSITY;
      half3 whiteReflection = half3(fresnel);
      half3 thinFilmColor = mix(whiteReflection, filmReflection, interferenceStrength);

      float spec1 = pow(lightAlign1, 250.0) * 2.5;
      float spec2 = pow(lightAlign2, 60.0) * 0.5;
      half3 highlights = half3(spec1 + spec2);

      float2 reflectOffset = normal.xy * ENV_BLUR_RADIUS;
      float2 envCenter = fragCoord + reflectOffset;
      float blurStep = ENV_BLUR_RADIUS * 0.4;
      half3 envSample = composable.eval(envCenter).rgb * 0.4
        + composable.eval(envCenter + float2(blurStep, 0.0)).rgb * 0.15
        + composable.eval(envCenter - float2(blurStep, 0.0)).rgb * 0.15
        + composable.eval(envCenter + float2(0.0, blurStep)).rgb * 0.15
        + composable.eval(envCenter - float2(0.0, blurStep)).rgb * 0.15;
      half3 envReflection = envSample * fresnel * ENV_REFLECTION_STRENGTH;

      float rimShadow = smoothstep(0.92, 1.0, sqrt(distSq));
      bgColor *= (1.0 - rimShadow * 0.25);

      half3 finalColor = bgColor * (1.0 - half3(fresnel))
        + thinFilmColor
        + envReflection
        + highlights;

      float fadeOut = 1.0 - pow(popProgress, 0.5);
      return half4(mix(rawBackground.rgb, finalColor, fadeOut), rawBackground.a);
    }
"""

/**
 * Interactive Thin-Film Soap Bubble & Refractive Glass Lens card.
 * Supports drag-to-lift with kinematic spring squash & stretch, tap-to-pop,
 * morphing Sun-to-Moon theme reveal, and live optical look presets.
 */
@Composable
fun ThinFilmBubbleInteractiveCard(
    RestingPromptText: String,
    LiftedTitleText: String,
    LiftedSubtitleText: String,
    initialPreset: BubbleLookPreset = BubbleLookPreset.SOAP_BUBBLE,
    initialDarkTheme: Boolean = true,
    cardHeight: Dp = 280.dp,
    maxOrbRadiusDp: Float = 112f,
    minOrbRadiusDp: Float = 62f,
    showPresetPicker: Boolean = true,
    onToggleExternalTheme: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedPreset by remember { mutableStateOf(initialPreset) }
    val lookValues = selectedPreset.uniforms

    val bottomOrbRatio = 0.84f
    val topOrbRatio = 0.26f
    val textYBottomRatio = 0.46f
    val textYTopRatio = 0.34f
    val snapUnlockThresholdPx = 10f
    val dragOvershootRatio = 0.05f

    val deformationFactor = 0.015f
    val deformationClamp = 0.6f
    val velocitySmoothing = 0.15f
    val springStiffness = 1500f
    val springDamping = 34.8f

    val popDurationMs = 150
    val popDelayMs = 1100L
    val themeRevealDurationMs = 950
    val textAnimDurationMs = 550

    val lightCenter = Color(0xFFFFFFFF)
    val lightMid1 = Color(0xFFF7F9FC)
    val lightMid2 = Color(0xFFEEF3FA)
    val lightEdge = Color(0xFFE3ECF8)
    val darkCenter = Color(0xFF1A2E5A)
    val darkMid = Color(0xFF0E2144)
    val darkEdge = Color(0xFF08142C)

    val lightMainText = Color(0xFF1B3358)
    val darkMainText = Color(0xFFE5EEFF)
    val lightTitle = Color(0xFF0F2242)
    val darkTitle = Color(0xFFF8FAFC)
    val lightSubtitle = Color(0xFF4C638A)
    val darkSubtitle = Color(0xFFA9C1E8)

    val sunColor = Color(0xFFFDB813)
    val moonColor = Color(0xFFE5E5EA)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clip(RoundedCornerShape(22.dp))
                .testTag("thin_film_bubble_card")
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
            val maxRadiusPx = with(density) { maxOrbRadiusDp.dp.toPx() }
            val minRadiusPx = with(density) { minOrbRadiusDp.dp.toPx() }

            val state = remember(
                widthPx,
                heightPx,
                bottomOrbRatio,
                topOrbRatio,
                textYBottomRatio,
                textYTopRatio,
                dragOvershootRatio
            ) {
                BubbleState(
                    screenHeightPx = heightPx,
                    centerX = widthPx / 2f,
                    bottomOrbRatio = bottomOrbRatio,
                    topOrbRatio = topOrbRatio,
                    textYBottomRatio = textYBottomRatio,
                    textYTopRatio = textYTopRatio,
                    dragOvershootRatio = dragOvershootRatio
                )
            }

            val radiusSpring = remember {
                spring<Float>(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
            }
            val animatedMaxRadiusPx by animateFloatAsState(
                targetValue = maxRadiusPx,
                animationSpec = radiusSpring,
                label = "bubbleMaxRadius"
            )
            val animatedMinRadiusPx by animateFloatAsState(
                targetValue = minRadiusPx,
                animationSpec = radiusSpring,
                label = "bubbleMinRadius"
            )

            val shader = remember {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    runCatching { RuntimeShader(THIN_FILM_BUBBLE_SHADER_SRC) }.getOrNull()
                } else {
                    null
                }
            }

            val scope = rememberCoroutineScope()
            var isDarkTheme by remember { mutableStateOf(initialDarkTheme) }
            var previousIsDark by remember { mutableStateOf(initialDarkTheme) }

            val lightBrush = remember(widthPx, heightPx) {
                radialBrush(widthPx, heightPx, lightCenter, lightMid1, lightMid2, lightEdge)
            }
            val darkBrush = remember(widthPx, heightPx) {
                radialBrush(widthPx, heightPx, darkCenter, darkMid, darkMid, darkEdge)
            }

            val textTween = remember(textAnimDurationMs) {
                tween<Color>(textAnimDurationMs, easing = FastOutLinearInEasing)
            }
            val mainTextColor by animateColorAsState(
                if (isDarkTheme) darkMainText else lightMainText,
                animationSpec = textTween,
                label = "bubbleMainText"
            )
            val titleColor by animateColorAsState(
                if (isDarkTheme) darkTitle else lightTitle,
                animationSpec = textTween,
                label = "bubbleTitle"
            )
            val subtitleColor by animateColorAsState(
                if (isDarkTheme) darkSubtitle else lightSubtitle,
                animationSpec = textTween,
                label = "bubbleSubtitle"
            )

            DeformationFrameLoop(
                state = state,
                deformationFactor = deformationFactor,
                deformationClamp = deformationClamp,
                velocitySmoothing = velocitySmoothing,
                stiffness = springStiffness,
                damping = springDamping
            )

            val lookSpring = remember {
                spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessVeryLow)
            }
            val animInterference = animateFloatAsState(lookValues[0], lookSpring, label = "lookInterference")
            val animTintR = animateFloatAsState(lookValues[1], lookSpring, label = "lookTintR")
            val animTintG = animateFloatAsState(lookValues[2], lookSpring, label = "lookTintG")
            val animTintB = animateFloatAsState(lookValues[3], lookSpring, label = "lookTintB")
            val animHueShift = animateFloatAsState(lookValues[4], lookSpring, label = "lookHueShift")

            val revealClipPath = remember { Path() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .bubbleDragInput(state, scope, snapUnlockThresholdPx)
                    .bubbleTapInput(state, scope, popDurationMs, popDelayMs)
                    .bubbleShaderLayer(
                        state = state,
                        shader = shader,
                        maxRadiusPx = animatedMaxRadiusPx,
                        minRadiusPx = animatedMinRadiusPx,
                        interferenceAmount = animInterference,
                        tintR = animTintR,
                        tintG = animTintG,
                        tintB = animTintB,
                        hueShift = animHueShift
                    )
                    .drawBehind {
                        drawThemeBackground(
                            isDarkTheme = isDarkTheme,
                            previousIsDark = previousIsDark,
                            revealProgress = state.themeRevealProgress.value,
                            lightBrush = lightBrush,
                            darkBrush = darkBrush,
                            reusablePath = revealClipPath
                        )
                    }
            ) {
                MorphingThemeToggleButton(
                    isDarkTheme = isDarkTheme,
                    sunColor = sunColor,
                    moonColor = moonColor,
                    onToggle = {
                        if (state.themeRevealProgress.isRunning) return@MorphingThemeToggleButton
                        previousIsDark = isDarkTheme
                        val nextDark = !isDarkTheme
                        isDarkTheme = nextDark
                        onToggleExternalTheme?.invoke(nextDark)
                        scope.launch {
                            state.themeRevealProgress.snapTo(0f)
                            state.themeRevealProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = themeRevealDurationMs,
                                    easing = CubicBezierEasing(0.1f, 0.8f, 0.2f, 1.0f)
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                        .testTag("bubble_theme_toggle_button")
                )

                Surface(
                    color = (if (isDarkTheme) Color.White else Color(0xFF0F2242)).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 14.dp, start = 14.dp)
                ) {
                    Text(
                        text = "Drag or Tap Bubble",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = mainTextColor.copy(alpha = 0.88f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Text(
                    text = RestingPromptText,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 25.sp,
                    color = mainTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-28).dp)
                        .padding(horizontal = 20.dp)
                        .graphicsLayer { alpha = 1f - (state.progress * 4f).coerceIn(0f, 1f) }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = state.textYOffsetPx.roundToInt()) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .graphicsLayer { alpha = (state.progress * 3f).coerceIn(0f, 1f) }
                    ) {
                        Text(
                            text = LiftedTitleText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            textAlign = TextAlign.Center,
                            color = titleColor
                        )
                        Text(
                            text = LiftedSubtitleText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                            color = subtitleColor,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || shader == null) {
                BubbleFallback(
                    state = state,
                    currentRadius = state.radiusFor(animatedMaxRadiusPx, animatedMinRadiusPx),
                    rimColors = selectedPreset.fallbackRimColors
                )
            }
        }

        if (showPresetPicker) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BubbleLookPreset.entries.forEach { preset ->
                    val isSelected = preset == selectedPreset
                    Surface(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            }
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { selectedPreset = preset }
                            .testTag("bubble_preset_${preset.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Brush.sweepGradient(preset.fallbackRimColors))
                            )
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Morphing Sun-to-Crescent-Moon theme toggle button using PathOperation.Difference
 * and spring-driven ray rotation. Works across all Android API levels (API 24+).
 */
@Composable
fun MorphingThemeToggleButton(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    sunColor: Color = Color(0xFFFDB813),
    moonColor: Color = Color(0xFFE5E5EA)
) {
    val progress by animateFloatAsState(
        targetValue = if (isDarkTheme) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "themeMorph"
    )

    val scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(isDarkTheme) {
        scaleAnim.snapTo(0.85f)
        scaleAnim.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 400f))
    }

    val mainPath = remember { Path() }
    val cutoutPath = remember { Path() }
    val finalPath = remember { Path() }

    Canvas(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
            .clip(CircleShape)
            .clickable(onClick = onToggle)
            .padding(10.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.width / 2f

        val currentColor = androidx.compose.ui.graphics.lerp(sunColor, moonColor, progress)

        rotate(degrees = progress * -90f, pivot = center) {
            val rayAlpha = (1f - progress * 2.5f).coerceIn(0f, 1f)
            if (rayAlpha > 0f) {
                val rayLength = maxRadius * 0.25f
                val rayOffset = maxRadius * 0.6f
                for (i in 0 until 8) {
                    rotate(degrees = i * 45f, pivot = center) {
                        drawLine(
                            color = currentColor.copy(alpha = rayAlpha),
                            start = center.copy(y = center.y - rayOffset),
                            end = center.copy(y = center.y - rayOffset - rayLength),
                            strokeWidth = maxRadius * 0.15f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            val sunRadius = maxRadius * 0.45f
            val moonRadius = maxRadius * 0.85f
            val currentRadius = sunRadius + (moonRadius - sunRadius) * progress

            mainPath.reset()
            mainPath.addOval(
                Rect(
                    left = center.x - currentRadius,
                    top = center.y - currentRadius,
                    right = center.x + currentRadius,
                    bottom = center.y + currentRadius
                )
            )

            val cutoutStartOffset = Offset(center.x + maxRadius * 2f, center.y - maxRadius * 2f)
            val cutoutEndOffset = Offset(center.x + currentRadius * 0.3f, center.y - currentRadius * 0.3f)
            val cutoutX = cutoutStartOffset.x + (cutoutEndOffset.x - cutoutStartOffset.x) * progress
            val cutoutY = cutoutStartOffset.y + (cutoutEndOffset.y - cutoutStartOffset.y) * progress
            val cutoutRadius = currentRadius * 0.95f

            cutoutPath.reset()
            cutoutPath.addOval(
                Rect(
                    left = cutoutX - cutoutRadius,
                    top = cutoutY - cutoutRadius,
                    right = cutoutX + cutoutRadius,
                    bottom = cutoutY + cutoutRadius
                )
            )

            finalPath.reset()
            finalPath.op(mainPath, cutoutPath, PathOperation.Difference)

            drawPath(path = finalPath, color = currentColor)
        }
    }
}

@Stable
private class BubbleState(
    screenHeightPx: Float,
    val centerX: Float,
    bottomOrbRatio: Float,
    topOrbRatio: Float,
    textYBottomRatio: Float,
    textYTopRatio: Float,
    dragOvershootRatio: Float
) {
    val bottomOrbCenterY = screenHeightPx * bottomOrbRatio
    val topOrbCenterY = screenHeightPx * topOrbRatio
    val midPoint = (bottomOrbCenterY + topOrbCenterY) / 2f
    val maxDragY = bottomOrbCenterY + (screenHeightPx * dragOvershootRatio)

    private val orbRange = (bottomOrbCenterY - topOrbCenterY).coerceAtLeast(1f)
    private val textYBottom = screenHeightPx * textYBottomRatio
    private val textYTop = screenHeightPx * textYTopRatio

    val bubblePos = Animatable(Offset(centerX, bottomOrbCenterY), Offset.VectorConverter)
    val deformationAnim = Animatable(Offset.Zero, Offset.VectorConverter)
    val popAnim = Animatable(0f)
    val themeRevealProgress = Animatable(1f)
    val shaderTime = floatArrayOf(0f)

    val progress: Float
        get() = ((bottomOrbCenterY - bubblePos.value.y) / orbRange).coerceIn(0f, 1f)

    fun radiusFor(maxPx: Float, minPx: Float): Float = lerp(maxPx, minPx, progress)

    val textYOffsetPx: Float
        get() = lerp(textYBottom, textYTop, progress)

    fun isAtTop(unlockThresholdPx: Float): Boolean =
        bubblePos.value.y <= topOrbCenterY + unlockThresholdPx
}

private val SnapBackSpring = spring<Offset>(
    dampingRatio = 0.65f,
    stiffness = Spring.StiffnessLow
)

private val UnlockedSnapSpring = spring<Offset>(
    dampingRatio = 0.45f,
    stiffness = Spring.StiffnessLow
)

@Composable
private fun DeformationFrameLoop(
    state: BubbleState,
    deformationFactor: Float,
    deformationClamp: Float,
    velocitySmoothing: Float,
    stiffness: Float,
    damping: Float
) {
    LaunchedEffect(
        state,
        deformationFactor,
        deformationClamp,
        velocitySmoothing,
        stiffness,
        damping
    ) {
        var previousActualPos = state.bubblePos.value
        val startTime = withFrameNanos { it }
        var lastFrameTime = startTime
        var smoothedVelocity = Offset.Zero
        var defVelocity = Offset.Zero

        while (true) {
            val frameTime = withFrameNanos { it }
            val dt = ((frameTime - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.032f)
            lastFrameTime = frameTime

            state.shaderTime[0] = (frameTime - startTime) / 1_000_000_000f

            val currentActualPos = state.bubblePos.value
            val rawVelocity = currentActualPos - previousActualPos

            smoothedVelocity = Offset(
                x = smoothedVelocity.x + (rawVelocity.x - smoothedVelocity.x) * velocitySmoothing,
                y = smoothedVelocity.y + (rawVelocity.y - smoothedVelocity.y) * velocitySmoothing
            )

            if (state.popAnim.value == 0f) {
                val targetDeformation = Offset(
                    x = (smoothedVelocity.x * deformationFactor).coerceIn(
                        -deformationClamp,
                        deformationClamp
                    ),
                    y = (smoothedVelocity.y * deformationFactor).coerceIn(
                        -deformationClamp,
                        deformationClamp
                    )
                )

                val currentDef = state.deformationAnim.value
                val forceX = (targetDeformation.x - currentDef.x) * stiffness - defVelocity.x * damping
                val forceY = (targetDeformation.y - currentDef.y) * stiffness - defVelocity.y * damping

                defVelocity = Offset(defVelocity.x + forceX * dt, defVelocity.y + forceY * dt)
                val nextDef = Offset(currentDef.x + defVelocity.x * dt, currentDef.y + defVelocity.y * dt)

                state.deformationAnim.snapTo(nextDef)
            } else {
                state.deformationAnim.snapTo(Offset.Zero)
                defVelocity = Offset.Zero
            }
            previousActualPos = currentActualPos
        }
    }
}

private fun Modifier.bubbleDragInput(
    state: BubbleState,
    scope: CoroutineScope,
    snapUnlockThresholdPx: Float
): Modifier = pointerInput(state, snapUnlockThresholdPx) {
    var isUnlocked = false
    detectDragGestures(
        onDragStart = { isUnlocked = state.isAtTop(snapUnlockThresholdPx) },
        onDragEnd = {
            scope.launch {
                if (isUnlocked) {
                    if (state.bubblePos.value.y < state.midPoint) {
                        state.bubblePos.animateTo(
                            Offset(state.centerX, state.topOrbCenterY),
                            UnlockedSnapSpring
                        )
                    } else {
                        state.bubblePos.animateTo(
                            Offset(state.centerX, state.bottomOrbCenterY),
                            SnapBackSpring
                        )
                    }
                } else {
                    val targetY = if (state.bubblePos.value.y < state.midPoint) {
                        state.topOrbCenterY
                    } else {
                        state.bottomOrbCenterY
                    }
                    state.bubblePos.animateTo(Offset(state.centerX, targetY), SnapBackSpring)
                }
            }
        }
    ) { change, dragAmount ->
        if (state.popAnim.value > 0f) return@detectDragGestures
        change.consume()
        val proposedY = state.bubblePos.value.y + dragAmount.y
        if (!isUnlocked && proposedY <= state.topOrbCenterY) isUnlocked = true
        if (isUnlocked) {
            scope.launch {
                state.bubblePos.snapTo(Offset(state.bubblePos.value.x + dragAmount.x, proposedY))
            }
        } else {
            val clampedY = proposedY.coerceAtMost(state.maxDragY)
            scope.launch { state.bubblePos.snapTo(Offset(state.centerX, clampedY)) }
        }
    }
}

private fun Modifier.bubbleTapInput(
    state: BubbleState,
    scope: CoroutineScope,
    popDurationMs: Int,
    popDelayMs: Long
): Modifier = pointerInput(state, popDurationMs, popDelayMs) {
    detectTapGestures(
        onTap = {
            if (state.popAnim.value == 0f) {
                scope.launch {
                    state.popAnim.animateTo(
                        1f,
                        tween(popDurationMs, easing = FastOutLinearInEasing)
                    )
                    delay(popDelayMs)
                    state.popAnim.snapTo(0f)
                    state.bubblePos.snapTo(Offset(state.centerX, state.bottomOrbCenterY))
                }
            }
        }
    )
}

private fun Modifier.bubbleShaderLayer(
    state: BubbleState,
    shader: RuntimeShader?,
    maxRadiusPx: Float,
    minRadiusPx: Float,
    interferenceAmount: State<Float>,
    tintR: State<Float>,
    tintG: State<Float>,
    tintB: State<Float>,
    hueShift: State<Float>
): Modifier = graphicsLayer {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null) {
        shader.setFloatUniform("touchCenter", state.bubblePos.value.x, state.bubblePos.value.y)
        shader.setFloatUniform("radius", state.radiusFor(maxRadiusPx, minRadiusPx))
        shader.setFloatUniform("progress", state.progress)
        shader.setFloatUniform(
            "deformation",
            state.deformationAnim.value.x,
            state.deformationAnim.value.y
        )
        shader.setFloatUniform("popProgress", state.popAnim.value)
        shader.setFloatUniform("sysTime", state.shaderTime[0])
        shader.setFloatUniform("interferenceAmount", interferenceAmount.value)
        shader.setFloatUniform("baseTint", tintR.value, tintG.value, tintB.value)
        shader.setFloatUniform("hueShift", hueShift.value)

        renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable")
            .asComposeRenderEffect()
    }
}

@Composable
private fun BubbleFallback(
    state: BubbleState,
    currentRadius: Float,
    rimColors: List<Color>
) {
    val pop = state.popAnim.value
    if (pop >= 1f) return
    val activeRadius = (currentRadius * (1f + pop * 1.4f)).coerceAtLeast(1f)
    val fadeAlpha = (1f - sqrt(pop)).coerceIn(0f, 1f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = state.bubblePos.value
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.08f * fadeAlpha),
                    rimColors.first().copy(alpha = 0.28f * fadeAlpha),
                    Color.Transparent
                ),
                center = center,
                radius = activeRadius
            ),
            center = center,
            radius = activeRadius
        )
        drawCircle(
            brush = Brush.sweepGradient(
                colors = rimColors.map { it.copy(alpha = 0.72f * fadeAlpha) },
                center = center
            ),
            center = center,
            radius = activeRadius * 0.96f,
            style = Stroke(width = 3.dp.toPx())
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.75f * fadeAlpha),
            radius = activeRadius * 0.14f,
            center = Offset(
                center.x - activeRadius * 0.35f,
                center.y - activeRadius * 0.35f
            )
        )
    }
}

private fun radialBrush(
    screenWidthPx: Float,
    screenHeightPx: Float,
    center: Color,
    mid1: Color,
    mid2: Color,
    edge: Color
): Brush = Brush.radialGradient(
    0.0f to center,
    0.3f to mid1,
    0.7f to mid2,
    1.0f to edge,
    center = Offset(screenWidthPx / 2f, screenHeightPx * 0.4f)
)

private fun DrawScope.drawThemeBackground(
    isDarkTheme: Boolean,
    previousIsDark: Boolean,
    revealProgress: Float,
    lightBrush: Brush,
    darkBrush: Brush,
    reusablePath: Path
) {
    val currentBrush = if (isDarkTheme) darkBrush else lightBrush
    val prevBrush = if (previousIsDark) darkBrush else lightBrush

    drawRect(brush = prevBrush)

    if (revealProgress < 1f) {
        val maxRadius = hypot(size.width, size.height)
        val currentRevealRadius = revealProgress * maxRadius
        val epicenter = Offset(size.width - 44f, 44f)

        reusablePath.reset()
        reusablePath.addOval(
            Rect(
                left = epicenter.x - currentRevealRadius,
                top = epicenter.y - currentRevealRadius,
                right = epicenter.x + currentRevealRadius,
                bottom = epicenter.y + currentRevealRadius
            )
        )

        clipPath(reusablePath) {
            drawRect(brush = currentBrush)
        }
    } else {
        drawRect(brush = currentBrush)
    }
}
