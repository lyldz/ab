package com.lyldz.ab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// İkon paletinden (lacivert #0A2A43, mavi, açık mavi) türetilmiş mavi tema.
private val Light = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9FB),
    onPrimaryContainer = Color(0xFF0A2A43),
    secondaryContainer = Color(0xFFDCEAF8),
    onSecondaryContainer = Color(0xFF0A2A43),
    background = Color(0xFFF4F8FC),
    onBackground = Color(0xFF0A2A43),
    surface = Color.White,
    onSurface = Color(0xFF0A2A43),
    surfaceVariant = Color(0xFFE4EEF8),
    onSurfaceVariant = Color(0xFF4A6078),
    outline = Color(0xFF8AA0B6),
    tertiary = Color(0xFFC25B00),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF06233A),
    primaryContainer = Color(0xFF17405F),
    onPrimaryContainer = Color(0xFFE6F3FF),
    secondaryContainer = Color(0xFF1F4468),
    onSecondaryContainer = Color(0xFFE6F3FF),
    background = Color(0xFF0B1B2A),
    onBackground = Color(0xFFE6EEF6),
    surface = Color(0xFF12283D),
    onSurface = Color(0xFFE6EEF6),
    surfaceVariant = Color(0xFF1D3550),
    onSurfaceVariant = Color(0xFFA9BED2),
    outline = Color(0xFF5E7790),
    tertiary = Color(0xFFFFB074),
)

@Composable
fun AbTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
