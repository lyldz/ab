package com.lyldz.ab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
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
    val Navy = Color(0xFF0F203C)
    val Pearl = Color(0xFFF8FAFC)
    val Slate = Color(0xFF64748B)
    val SlateLight = Color(0xFF94A3B8)
    val Mist = Color(0xFFE2E8F0)
    val Teal = Color(0xFF5BC0BE)
}

// Yalnızca açık tema (tasarım kılavuzu). Etkileşimli metinler (düğme yazısı, anahtar) okunaklı olsun
// diye Material "primary" biraz koyu teal; saf aqua/mint görsellerde (gösterge, düğme gradyanı) kullanılır.
private val Light = lightColorScheme(
    primary = Color(0xFF1F7F92),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F6F9),
    onPrimaryContainer = AbColors.Navy,
    secondary = AbColors.Teal,
    secondaryContainer = Color(0xFFE3F6F9),
    onSecondaryContainer = AbColors.Navy,
    background = AbColors.Pearl,
    onBackground = AbColors.Navy,
    surface = Color.White,
    onSurface = AbColors.Navy,
    surfaceVariant = Color(0xFFEEF2F6),
    onSurfaceVariant = AbColors.Slate,
    outline = AbColors.SlateLight,
    outlineVariant = AbColors.Mist,
    tertiary = Color(0xFFB08A3E),
)

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

@Composable
fun AbTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Light, typography = Typography().withFamily(Jakarta), content = content)
}
