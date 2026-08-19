package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ObsidianColorScheme = darkColorScheme(
    primary = PolishPrimary,
    onPrimary = Color(0xFF0B0C0E),
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishOnPrimaryContainer,
    secondary = PolishSecondary,
    onSecondary = Color(0xFF0B0C0E),
    secondaryContainer = PolishSecondaryContainer,
    onSecondaryContainer = PolishOnSecondaryContainer,
    tertiary = PolishTertiary,
    onTertiary = Color.White,
    tertiaryContainer = PolishTertiaryContainer,
    background = PolishBackground,
    onBackground = PolishTextPrimary,
    surface = PolishSurface,
    onSurface = PolishTextPrimary,
    surfaceVariant = PolishSurfaceElevated,
    onSurfaceVariant = PolishTextSecondary,
    outline = PolishSurfaceBorder,
    outlineVariant = PolishSurfaceBorderDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ObsidianColorScheme,
        typography = Typography,
        content = content
    )
}
