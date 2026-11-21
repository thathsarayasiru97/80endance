package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.AppAccentPalette
import com.example.data.AppThemeMode

@Immutable
data class AttendanceSemanticColors(
    val present: Color,
    val presentContainer: Color,
    val onPresentContainer: Color,
    val absent: Color,
    val absentContainer: Color,
    val onAbsentContainer: Color,
    val medical: Color,
    val medicalContainer: Color,
    val onMedicalContainer: Color,
    val schoolEvent: Color,
    val schoolEventContainer: Color,
    val onSchoolEventContainer: Color,
    val poya: Color,
    val poyaContainer: Color,
    val onPoyaContainer: Color
)

private val LightSemanticColors = AttendanceSemanticColors(
    present = EmeraldLight,
    presentContainer = EmeraldContainerLight,
    onPresentContainer = OnEmeraldContainerLight,
    absent = LightError,
    absentContainer = LightErrorContainer,
    onAbsentContainer = LightOnErrorContainer,
    medical = SaffronLight,
    medicalContainer = SaffronContainerLight,
    onMedicalContainer = OnSaffronContainerLight,
    schoolEvent = SchoolEventLight,
    schoolEventContainer = SchoolEventContainerLight,
    onSchoolEventContainer = OnSchoolEventContainerLight,
    poya = VioletHolidayLight,
    poyaContainer = VioletHolidayContainerLight,
    onPoyaContainer = OnVioletHolidayContainerLight
)

private val DarkSemanticColors = AttendanceSemanticColors(
    present = EmeraldDark,
    presentContainer = EmeraldContainerDark,
    onPresentContainer = OnEmeraldContainerDark,
    absent = DarkError,
    absentContainer = DarkErrorContainer,
    onAbsentContainer = DarkOnErrorContainer,
    medical = SaffronDark,
    medicalContainer = SaffronContainerDark,
    onMedicalContainer = OnSaffronContainerDark,
    schoolEvent = SchoolEventDark,
    schoolEventContainer = SchoolEventContainerDark,
    onSchoolEventContainer = OnSchoolEventContainerDark,
    poya = VioletHolidayDark,
    poyaContainer = VioletHolidayContainerDark,
    onPoyaContainer = OnVioletHolidayContainerDark
)

val LocalAttendanceColors = staticCompositionLocalOf { LightSemanticColors }

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

val LocalIsOledBlack = staticCompositionLocalOf { false }

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

fun accentSwatchColor(palette: AppAccentPalette, isDark: Boolean): Color = when (palette) {
    AppAccentPalette.ROYAL_BLUE -> if (isDark) DarkPrimary else LightPrimary
    AppAccentPalette.EMERALD -> if (isDark) EmeraldAccentDarkPrimary else EmeraldAccentLightPrimary
    AppAccentPalette.CRIMSON -> if (isDark) CrimsonAccentDarkPrimary else CrimsonAccentLightPrimary
    AppAccentPalette.AMETHYST -> if (isDark) AmethystAccentDarkPrimary else AmethystAccentLightPrimary
}

private fun buildColorScheme(
    isDark: Boolean,
    isOledBlack: Boolean,
    accentPalette: AppAccentPalette
): ColorScheme {
    val base = when {
        isDark && isOledBlack -> DarkColorScheme.copy(
            background = OledBackground,
            onBackground = OledOnBackground,
            surface = OledSurface,
            onSurface = OledOnSurface,
            surfaceVariant = OledSurfaceVariant,
            onSurfaceVariant = OledOnSurfaceVariant,
            surfaceContainerLowest = OledSurfaceContainerLowest,
            surfaceContainerLow = OledSurfaceContainerLow,
            surfaceContainer = OledSurfaceContainer,
            surfaceContainerHigh = OledSurfaceContainerHigh,
            outline = OledOutline,
            outlineVariant = OledOutlineVariant
        )
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    return when (accentPalette) {
        AppAccentPalette.ROYAL_BLUE -> base
        AppAccentPalette.EMERALD -> if (isDark) {
            base.copy(
                primary = EmeraldAccentDarkPrimary,
                onPrimary = EmeraldAccentDarkOnPrimary,
                primaryContainer = EmeraldAccentDarkPrimaryContainer,
                onPrimaryContainer = EmeraldAccentDarkOnPrimaryContainer
            )
        } else {
            base.copy(
                primary = EmeraldAccentLightPrimary,
                primaryContainer = EmeraldAccentLightPrimaryContainer,
                onPrimaryContainer = EmeraldAccentLightOnPrimaryContainer,
                secondary = EmeraldAccentLightSecondary
            )
        }
        AppAccentPalette.CRIMSON -> if (isDark) {
            base.copy(
                primary = CrimsonAccentDarkPrimary,
                onPrimary = CrimsonAccentDarkOnPrimary,
                primaryContainer = CrimsonAccentDarkPrimaryContainer,
                onPrimaryContainer = CrimsonAccentDarkOnPrimaryContainer
            )
        } else {
            base.copy(
                primary = CrimsonAccentLightPrimary,
                primaryContainer = CrimsonAccentLightPrimaryContainer,
                onPrimaryContainer = CrimsonAccentLightOnPrimaryContainer,
                secondary = CrimsonAccentLightSecondary
            )
        }
        AppAccentPalette.AMETHYST -> if (isDark) {
            base.copy(
                primary = AmethystAccentDarkPrimary,
                onPrimary = AmethystAccentDarkOnPrimary,
                primaryContainer = AmethystAccentDarkPrimaryContainer,
                onPrimaryContainer = AmethystAccentDarkOnPrimaryContainer
            )
        } else {
            base.copy(
                primary = AmethystAccentLightPrimary,
                primaryContainer = AmethystAccentLightPrimaryContainer,
                onPrimaryContainer = AmethystAccentLightOnPrimaryContainer,
                secondary = AmethystAccentLightSecondary
            )
        }
    }
}

@Composable
fun EightyEndanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentPalette: AppAccentPalette = AppAccentPalette.ROYAL_BLUE,
    content: @Composable () -> Unit
) {
    val resolvedDark = when (themeMode) {
        AppThemeMode.SYSTEM -> darkTheme
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.OLED_BLACK -> true
    }
    val isOledBlack = themeMode == AppThemeMode.OLED_BLACK
    val colorScheme = buildColorScheme(resolvedDark, isOledBlack, accentPalette)
    val semanticColors = if (resolvedDark) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(
        LocalAttendanceColors provides semanticColors,
        LocalIsOledBlack provides isOledBlack
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
