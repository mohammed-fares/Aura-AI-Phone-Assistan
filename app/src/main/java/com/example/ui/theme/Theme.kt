package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val PolishColorScheme = lightColorScheme(
    primary = PolishPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishOnPrimaryContainer,
    secondary = PolishSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = PolishSecondaryContainer,
    onSecondaryContainer = PolishOnSecondaryContainer,
    tertiary = PolishTertiary,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = PolishTertiaryContainer,
    background = PolishBackground,
    onBackground = PolishTextPrimary,
    surface = PolishSurface,
    onSurface = PolishTextPrimary,
    surfaceVariant = PolishSecondaryContainer,
    onSurfaceVariant = PolishTextSecondary,
    outline = PolishSurfaceBorder,
    outlineVariant = PolishSurfaceBorderDark
)

private val DarkColorScheme = PolishColorScheme
private val LightColorScheme = PolishColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> PolishColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

