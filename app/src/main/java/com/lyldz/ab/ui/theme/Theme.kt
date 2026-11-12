package com.lyldz.ab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF0B5563),
    onPrimary = Color.White,
    background = Color(0xFFF6F1E7),
    surface = Color(0xFFF6F1E7),
    onBackground = Color(0xFF14232A),
    onSurface = Color(0xFF14232A),
    onSurfaceVariant = Color(0xFF5A6B70),
    tertiary = Color(0xFF9E4425),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF5FC7C4),
    onPrimary = Color(0xFF0E1A1F),
    background = Color(0xFF0E1A1F),
    surface = Color(0xFF0E1A1F),
    onBackground = Color(0xFFE9E4D8),
    onSurface = Color(0xFFE9E4D8),
    onSurfaceVariant = Color(0xFF9AAEB2),
    tertiary = Color(0xFFE08A5E),
)

@Composable
fun AbTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
