package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.util.StudyPreferences

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryIndigoLight,
    onPrimary = DarkBackground,
    primaryContainer = PrimaryIndigo,
    onPrimaryContainer = DarkOnBackground,
    secondary = SecondaryTealLight,
    onSecondary = DarkBackground,
    secondaryContainer = SecondaryTeal,
    onSecondaryContainer = DarkOnBackground,
    tertiary = TertiaryAmberLight,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnBackground,
    error = ErrorCrimson
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryIndigo,
    onPrimary = LightSurface,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = PrimaryIndigo,
    secondary = SecondaryTeal,
    onSecondary = LightSurface,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = SecondaryTeal,
    tertiary = TertiaryAmber,
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurface,
    error = ErrorCrimson
)

private val MidnightOledColorScheme = darkColorScheme(
    primary = MidnightOledPrimary,
    onPrimary = Color.White,
    primaryContainer = MidnightOledSurfaceVariant,
    onPrimaryContainer = Color.White,
    secondary = MidnightOledSecondary,
    onSecondary = Color.Black,
    secondaryContainer = MidnightOledSurfaceVariant,
    onSecondaryContainer = MidnightOledSecondary,
    tertiary = MidnightOledTertiary,
    onTertiary = Color.White,
    background = MidnightOledBg,
    onBackground = Color(0xFFF8FAFC),
    surface = MidnightOledSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = MidnightOledSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    error = ErrorCrimson
)

private val CozyLofiColorScheme = darkColorScheme(
    primary = CozyLofiPrimary,
    onPrimary = Color.White,
    primaryContainer = CozyLofiSurfaceVariant,
    onPrimaryContainer = Color(0xFFFFEDD5),
    secondary = CozyLofiSecondary,
    onSecondary = Color.Black,
    secondaryContainer = CozyLofiSurfaceVariant,
    onSecondaryContainer = CozyLofiSecondary,
    tertiary = CozyLofiTertiary,
    onTertiary = Color.White,
    background = CozyLofiBg,
    onBackground = Color(0xFFFDFBF7),
    surface = CozyLofiSurface,
    onSurface = Color(0xFFFDFBF7),
    surfaceVariant = CozyLofiSurfaceVariant,
    onSurfaceVariant = Color(0xFFD6C7C2),
    error = ErrorCrimson
)

private val ForestZenColorScheme = darkColorScheme(
    primary = ForestZenPrimary,
    onPrimary = Color.Black,
    primaryContainer = ForestZenSurfaceVariant,
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = ForestZenSecondary,
    onSecondary = Color.Black,
    secondaryContainer = ForestZenSurfaceVariant,
    onSecondaryContainer = ForestZenSecondary,
    tertiary = ForestZenTertiary,
    onTertiary = Color.Black,
    background = ForestZenBg,
    onBackground = Color(0xFFF0FDF4),
    surface = ForestZenSurface,
    onSurface = Color(0xFFF0FDF4),
    surfaceVariant = ForestZenSurfaceVariant,
    onSurfaceVariant = Color(0xFFA7F3D0),
    error = ErrorCrimson
)

@Composable
fun StudyGuardTheme(
    themeMode: StudyPreferences.ThemeMode = StudyPreferences.ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        StudyPreferences.ThemeMode.DARK,
        StudyPreferences.ThemeMode.MIDNIGHT_OLED,
        StudyPreferences.ThemeMode.COZY_LOFI,
        StudyPreferences.ThemeMode.FOREST_ZEN -> true
        StudyPreferences.ThemeMode.LIGHT -> false
        StudyPreferences.ThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = when (themeMode) {
        StudyPreferences.ThemeMode.MIDNIGHT_OLED -> MidnightOledColorScheme
        StudyPreferences.ThemeMode.COZY_LOFI -> CozyLofiColorScheme
        StudyPreferences.ThemeMode.FOREST_ZEN -> ForestZenColorScheme
        StudyPreferences.ThemeMode.LIGHT -> LightColorScheme
        StudyPreferences.ThemeMode.DARK -> DarkColorScheme
        StudyPreferences.ThemeMode.SYSTEM -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (systemDark) {
                DarkColorScheme
            } else {
                LightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
