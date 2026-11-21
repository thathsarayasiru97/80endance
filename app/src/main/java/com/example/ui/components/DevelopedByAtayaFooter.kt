package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Signature footer badge ("Developed by Ataya") with an organic vintage tungsten / neon bulb
 * flicker animation that draws the eye smoothly without being harsh.
 */
@Composable
fun DevelopedByAtayaFooter(
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()
    val transition = rememberInfiniteTransition(label = "ataya_bulb_flicker")

    // Realistic old tungsten filament intensity curve (0.18f = dim brownout, 1.0f = warm full glow)
    val filamentIntensity by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 5600
                // Quick vintage warm-up double-flicker
                0.92f at 0 using LinearEasing
                0.24f at 90 using LinearEasing
                1.00f at 170 using LinearEasing
                0.35f at 240 using LinearEasing
                0.98f at 330 using LinearEasing
                0.18f at 410 using LinearEasing
                1.00f at 540 using LinearEasing
                // Steady warm Edison hum
                0.94f at 1600 using LinearEasing
                0.98f at 2400 using LinearEasing
                0.90f at 3200 using LinearEasing
                // Brief realistic voltage dip / micro-stutter
                0.42f at 3320 using LinearEasing
                0.96f at 3420 using LinearEasing
                0.28f at 3510 using LinearEasing
                1.00f at 3640 using LinearEasing
                // Soft warm glow settle
                0.93f at 4800 using LinearEasing
                0.92f at 5600 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "ataya_filament_intensity"
    )

    // Interactive spark surge when tapped
    var sparkCount by remember { mutableIntStateOf(0) }
    var sparkBoost by remember { mutableStateOf(false) }
    LaunchedEffect(sparkCount) {
        if (sparkCount > 0) {
            sparkBoost = true
            delay(320L)
            sparkBoost = false
        }
    }

    val tapScale by animateFloatAsState(
        targetValue = if (sparkBoost) 1.06f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ataya_tap_scale"
    )

    val effectiveIntensity = if (sparkBoost) 1f else filamentIntensity

    // Warm tungsten gold/amber hues tailored for both dark/OLED and crisp light surfaces
    val filamentBright = if (isDark) Color(0xFFFFD54F) else Color(0xFFD97706)
    val filamentGlow = if (isDark) Color(0xFFFFB300) else Color(0xFFF59E0B)
    val filamentDim = if (isDark) Color(0xFF6D5524) else Color(0xFF9E7B3B)
    val activeFilamentColor = androidx.compose.ui.graphics.lerp(
        filamentDim,
        filamentBright,
        effectiveIntensity.coerceIn(0f, 1f)
    )

    val pillBg = if (isDark) {
        Color(0xFF1A1610).copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = pillBg,
            shape = RoundedCornerShape(999.dp),
            tonalElevation = if (isDark) 0.dp else 1.dp,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = tapScale
                    scaleY = tapScale
                }
                .clip(RoundedCornerShape(999.dp))
                .border(
                    width = 1.dp,
                    color = filamentGlow.copy(alpha = (0.14f + 0.44f * effectiveIntensity).coerceIn(0f, 0.7f)),
                    shape = RoundedCornerShape(999.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { sparkCount++ }
                .testTag("developed_by_ataya_footer")
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Warm radial Edison bulb halo behind the icon and wordmark
                Canvas(modifier = Modifier.matchParentSize()) {
                    val glowAlpha = (if (isDark) 0.34f else 0.22f) * effectiveIntensity
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                filamentGlow.copy(alpha = glowAlpha),
                                filamentGlow.copy(alpha = glowAlpha * 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(x = size.width * 0.22f, y = size.height * 0.5f),
                            radius = size.width * 0.52f
                        ),
                        center = Offset(x = size.width * 0.22f, y = size.height * 0.5f),
                        radius = size.width * 0.52f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                filamentBright.copy(alpha = glowAlpha * 0.75f),
                                Color.Transparent
                            ),
                            center = Offset(x = size.width * 0.72f, y = size.height * 0.5f),
                            radius = size.width * 0.45f
                        ),
                        center = Offset(x = size.width * 0.72f, y = size.height * 0.5f),
                        radius = size.width * 0.45f
                    )
                }

                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = "Developed by Ataya",
                        tint = activeFilamentColor,
                        modifier = Modifier
                            .size(17.dp)
                            .graphicsLayer {
                                alpha = (0.35f + 0.65f * effectiveIntensity).coerceIn(0.3f, 1f)
                            }
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "Developed by",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = (0.62f + 0.30f * effectiveIntensity).coerceIn(0.55f, 0.95f)
                        ),
                        letterSpacing = 0.3.sp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Ataya",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.9.sp,
                            shadow = Shadow(
                                color = filamentGlow.copy(alpha = (0.65f * effectiveIntensity).coerceIn(0f, 0.85f)),
                                offset = Offset.Zero,
                                blurRadius = 10f * effectiveIntensity + 2f
                            )
                        ),
                        color = activeFilamentColor
                    )
                }
            }
        }
    }
}
