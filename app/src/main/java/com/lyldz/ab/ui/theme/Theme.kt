package com.lyldz.ab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.lyldz.ab.R

/** Palet 2 — Derin Gece & Elektrik Turkuaz (Konsept A "ab" ligatürüyle uyumlu). */
object AbColors {
    val Aqua1 = Color(0xFF5CE1E6)
    val Aqua2 = Color(0xFFA0E7E5)
    val Teal = Color(0xFF22B8CF)
    val DeepTeal = Color(0xFF0E7490)
    val Ocean = Color(0xFF0A7FA8)
    val Navy = Color(0xFF0B253A)
    val Pearl = Color(0xFFF8FAFC)
    val Ice = Color(0xFFF4F9FB)
    val Slate = Color(0xFF475B70)
    val SlateLight = Color(0xFF7B93A4)
    val Mist = Color(0xFFD5E8F0)
}

// Açık tema: ferah buzul zemin (#F4F9FB), derin lacivert metin (#0B253A), keskin okyanus-teal vurgu (#0E7490).
private val Light = lightColorScheme(
    primary = AbColors.DeepTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFF6FA),
    onPrimaryContainer = AbColors.Navy,
    secondary = AbColors.Teal,
    onSecondary = AbColors.Navy,
    secondaryContainer = Color(0xFFE6F8FA),
    onSecondaryContainer = AbColors.Navy,
    background = AbColors.Ice,
    onBackground = AbColors.Navy,
    surface = Color.White,
    onSurface = AbColors.Navy,
    surfaceVariant = Color(0xFFEAF4F8),
    onSurfaceVariant = AbColors.Slate,
    outline = AbColors.SlateLight,
    outlineVariant = AbColors.Mist,
    tertiary = Color(0xFFB08A3E),
)

// Koyu tema: derin gece laciverti (#06131F), parlak elektrik turkuaz vurgu (#5CE1E6).
private val Dark = darkColorScheme(
    primary = AbColors.Aqua1,
    onPrimary = AbColors.Navy,
    primaryContainer = Color(0xFF102A3C),
    onPrimaryContainer = Color(0xFFF0F9FF),
    secondary = Color(0xFF38BDF8),
    onSecondary = AbColors.Navy,
    secondaryContainer = Color(0xFF122E40),
    onSecondaryContainer = Color(0xFFF0F9FF),
    background = Color(0xFF06131F),
    onBackground = Color(0xFFF0F9FF),
    surface = Color(0xFF0D1F30),
    onSurface = Color(0xFFF0F9FF),
    surfaceVariant = Color(0xFF142B40),
    onSurfaceVariant = Color(0xFFA3BED0),
    outline = Color(0xFF668399),
    outlineVariant = Color(0xFF1E374D),
    tertiary = Color(0xFFD9B96A),
)

/** Temaya duyarlı yüzey/metin renkleri (açıkta lacivert/slate/mist, koyuda beyaz/gri). */
object Ab {
    val strong @Composable get() = MaterialTheme.colorScheme.onSurface
    val muted @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val faint @Composable get() = MaterialTheme.colorScheme.outline
    val line @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val card @Composable get() = MaterialTheme.colorScheme.surface
    val isDark @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f
}

@OptIn(ExperimentalTextApi::class)
private fun jakarta(w: Int) = Font(
    R.font.plus_jakarta_sans, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

private val Jakarta = FontFamily(jakarta(400), jakarta(500), jakarta(600), jakarta(700), jakarta(800))

private fun Typography.withFamily(f: FontFamily) = Typography(
    displayLarge = displayLarge.fam(f), displayMedium = displayMedium.fam(f), displaySmall = displaySmall.fam(f),
    headlineLarge = headlineLarge.fam(f), headlineMedium = headlineMedium.fam(f), headlineSmall = headlineSmall.fam(f),
    titleLarge = titleLarge.fam(f), titleMedium = titleMedium.fam(f), titleSmall = titleSmall.fam(f),
    bodyLarge = bodyLarge.fam(f), bodyMedium = bodyMedium.fam(f), bodySmall = bodySmall.fam(f),
    labelLarge = labelLarge.fam(f), labelMedium = labelMedium.fam(f), labelSmall = labelSmall.fam(f),
)

private fun TextStyle.fam(f: FontFamily) = copy(fontFamily = f)

/** [mode]: 0 sistem, 1 açık, 2 koyu. */
@Composable
fun AbTheme(mode: Int = 0, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Typography().withFamily(Jakarta), content = content)
}
