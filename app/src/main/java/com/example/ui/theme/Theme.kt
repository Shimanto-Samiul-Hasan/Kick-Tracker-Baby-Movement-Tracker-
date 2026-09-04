package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AmoledDarkColorScheme = darkColorScheme(
    primary = RosePrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3E1220),
    onPrimaryContainer = RoseSecondaryDark,
    secondary = RoseSecondaryDark,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2E1720),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = MintSuccess,
    onTertiary = Color.Black,
    tertiaryContainer = MintSuccessContainer,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = BlackAmoled,
    onBackground = OnBackgroundDark,
    surface = DarkSurfaceAmoled,
    onSurface = OnSurfaceDark,
    surfaceVariant = DarkSurfaceVariantAmoled,
    onSurfaceVariant = Color(0xFFD4C2C8),
    outline = Color(0xFF3B3338)
)

private val WarmLightColorScheme = lightColorScheme(
    primary = RosePrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3E0018),
    secondary = RoseSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF3F0018),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA7F3D0),
    onTertiaryContainer = Color(0xFF064E3B),
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF524346),
    outline = Color(0xFFE2D0D4)
)

@Composable
fun KickTrackerTheme(
    amoledDarkMode: Boolean = false,
    useSystemDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = amoledDarkMode || (useSystemDark && isSystemInDarkTheme())
    val colorScheme = if (isDark) AmoledDarkColorScheme else WarmLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    KickTrackerTheme(
        amoledDarkMode = darkTheme,
        useSystemDark = true,
        content = content
    )
}
