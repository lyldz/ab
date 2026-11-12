package com.lyldz.ab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Marka belirteçleri (tasarım sistemi): Aqua gradyanı, Deep Navy, Pearl White. */
object AbColors {
    val Aqua1 = Color(0xFF3FB0C4)
    val Aqua2 = Color(0xFF1B7F95)
    val Navy = Color(0xFF12284A)
    val Pearl = Color(0xFFFFFDF9)
}

// Marka paleti: Aqua, Deep Navy, Pearl White (docs/design/brand). Yazılar lacivert, vurgu su yeşili-mavisi.
private val Light = lightColorScheme(
    primary = Color(0xFF1B7F95),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEFF3),
    onPrimaryContainer = Color(0xFF12284A),
    secondaryContainer = Color(0xFFE6EFF1),
    onSecondaryContainer = Color(0xFF12284A),
    background = Color(0xFFF7F4EE),
    onBackground = Color(0xFF12284A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF12284A),
    surfaceVariant = Color(0xFFE9EEEF),
    onSurfaceVariant = Color(0xFF55696F),
    outline = Color(0xFF9AAAAE),
    tertiary = Color(0xFFB08A3E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7CCFCB),
    onPrimary = Color(0xFF06302F),
    primaryContainer = Color(0xFF184A56),
    onPrimaryContainer = Color(0xFFDDF5F2),
    secondaryContainer = Color(0xFF1B3F55),
    onSecondaryContainer = Color(0xFFDDF5F2),
    background = Color(0xFF0B1D33),
    onBackground = Color(0xFFEAF1F3),
    surface = Color(0xFF122A45),
    onSurface = Color(0xFFEAF1F3),
    surfaceVariant = Color(0xFF1B3A52),
    onSurfaceVariant = Color(0xFFADC4CC),
    outline = Color(0xFF5F7E8C),
    tertiary = Color(0xFFD9B96A),
)

@Composable
fun AbTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
