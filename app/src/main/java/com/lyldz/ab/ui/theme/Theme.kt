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

/** Tasarım sistemi belirteçleri (docs/design/brand/spec): Aqua, Mint, Deep Navy, Pearl, Slate, Mist. */
object AbColors {
    val Aqua1 = Color(0xFF88D9E6)
    val Aqua2 = Color(0xFFA0E7E5)
    val Teal = Color(0xFF5BC0BE)
    val DeepTeal = Color(0xFF1F7F92)
    val Ocean = Color(0xFF0A7FA8)
    val Navy = Color(0xFF0F203C)
    val Pearl = Color(0xFFF8FAFC)
    val Ice = Color(0xFFF3FAFC)
    val Slate = Color(0xFF475B70)
    val SlateLight = Color(0xFF7B93A4)
    val Mist = Color(0xFFD6E9EF)
}

// Açık tema (tasarım kılavuzu). Etkileşimli metinler (düğme yazısı, anahtar) okunaklı olsun
// diye Material "primary" derin turkuaz (#1F7F92); saf aqua/mint görsellerde (gösterge, düğme gradyanı) kullanılır.
private val Light = lightColorScheme(
    primary = AbColors.DeepTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F5F8),
    onPrimaryContainer = AbColors.Navy,
    secondary = AbColors.Teal,
    onSecondary = AbColors.Navy,
    secondaryContainer = Color(0xFFE6F8F8),
    onSecondaryContainer = AbColors.Navy,
    background = AbColors.Ice,
    onBackground = AbColors.Navy,
    surface = Color.White,
    onSurface = AbColors.Navy,
    surfaceVariant = Color(0xFFEBF4F7),
    onSurfaceVariant = AbColors.Slate,
    outline = AbColors.SlateLight,
    outlineVariant = AbColors.Mist,
    tertiary = Color(0xFFB08A3E),
)

// Koyu tema: derin okyanus-siyah zemin, yüksek kontrastlı açık metin, parlak turkuaz/aqua vurgular.
private val Dark = darkColorScheme(
    primary = AbColors.Aqua1,
    onPrimary = AbColors.Navy,
    primaryContainer = Color(0xFF132D35),
    onPrimaryContainer = Color(0xFFE8FAFD),
    secondary = AbColors.Aqua2,
    onSecondary = AbColors.Navy,
    secondaryContainer = Color(0xFF142E33),
    onSecondaryContainer = Color(0xFFE8FAFD),
    background = Color(0xFF070C10),
    onBackground = Color(0xFFF1F7FA),
    surface = Color(0xFF101920),
    onSurface = Color(0xFFF1F7FA),
    surfaceVariant = Color(0xFF18252E),
    onSurfaceVariant = Color(0xFFA6BDCB),
    outline = Color(0xFF6A8392),
    outlineVariant = Color(0xFF213440),
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
