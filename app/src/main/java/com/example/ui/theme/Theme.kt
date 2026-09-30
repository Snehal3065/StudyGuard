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

@Composable
fun StudyGuardTheme(
    themeMode: StudyPreferences.ThemeMode = StudyPreferences.ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        StudyPreferences.ThemeMode.DARK -> true
        StudyPreferences.ThemeMode.LIGHT -> false
        StudyPreferences.ThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
