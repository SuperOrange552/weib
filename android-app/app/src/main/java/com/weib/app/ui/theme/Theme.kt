package com.weib.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WeibColors = lightColorScheme(
    primary = WeibPrimary,
    onPrimary = Color.White,
    secondary = WeibAccent,
    onSecondary = Color.White,
    background = WeibBackground,
    onBackground = WeibTitle,
    surface = WeibSurface,
    onSurface = WeibTitle,
    surfaceVariant = WeibLightAccent,
    onSurfaceVariant = WeibBody,
    outline = WeibBorder,
    error = Color(0xFFB91C1C)
)

@Composable
fun WeibTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WeibColors, typography = WeibTypography, content = content)
}
