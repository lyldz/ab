package com.lyldz.ab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Marka belirteçleri (tasarım sistemi): Aqua gradyanı, Deep Navy, Pearl White. */
object AbColors {
    val Aqua1 = Color(0xFF88D9E6)
    val Aqua2 = Color(0xFFA0E7E5)
    val Navy = Color(0xFF0F203C)
    val Pearl = Color(0xFFF8FAFC)
}

// Marka paleti: Aqua, Deep Navy, Pearl White (docs/design/brand). Yazılar lacivert, vurgu su yeşili-mavisi.
private val Light = lightColorScheme(
    primary = Color(0xFF2C7C8C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3F0F4),
    onPrimaryContainer = Color(0xFF0F203C),
    secondaryContainer = Color(0xFFDCEFEC),
    onSecondaryContainer = Color(0xFF0F2A4A),
    background = Color(0xFFF3F6F9),
    onBackground = Color(0xFF0F203C),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F203C),
    surfaceVariant = Color(0xFFE3EEEA),
    onSurfaceVariant = Color(0xFF4B6470),
    outline = Color(0xFF8FA8AA),
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
