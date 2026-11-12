package com.lyldz.ab.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Hafif eğimli, alt köşeleri yuvarlak bir bardak silueti (bardak seçici simgeleri için). */
private fun glassPath(w: Float, h: Float): Path = Path().apply {
    val t = h * 0.03f
    val b = h * 0.97f
    val rad = w * 0.09f
    val l1 = w * 0.06f
    val r1 = w * 0.94f
    val l2 = w * 0.20f
    val r2 = w * 0.80f
    moveTo(l1, t)
    lineTo(r1, t)
    lineTo(r2, b - rad)
    quadraticBezierTo(r2, b, r2 - rad, b)
    lineTo(l2 + rad, b)
    quadraticBezierTo(l2, b, l2, b - rad)
    close()
}

/** Sürahi gövdesi: düz kenarlı, sol üstte küçük bir ağız (gaga). Sap ayrıca çizilir. */
private fun pitcherBody(w: Float, h: Float): Path = Path().apply {
    val rad = w * 0.08f
    val b = h * 0.96f
    moveTo(w * 0.07f, h * 0.05f)
    lineTo(w * 0.64f, h * 0.06f)
    lineTo(w * 0.70f, h * 0.20f)
    lineTo(w * 0.70f, b - rad)
    quadraticBezierTo(w * 0.70f, b, w * 0.70f - rad, b)
    lineTo(w * 0.12f + rad, b)
    quadraticBezierTo(w * 0.12f, b, w * 0.12f, b - rad)
    lineTo(w * 0.12f, h * 0.20f)
    close()
}

/**
 * Sürahi: kapasite kişinin günlük hedefidir (tam dolu = hedef). Su seviyesi yaylı animasyonla
 * yükselir, yüzey dalgalanır; solda her 500 ml'de (küçük kapasitede 250) ölçek çizgisi,
 * [paceFraction] varsa şu an olman gereken seviyeyi gösteren kesik çizgi bulunur.
 */
@Composable
fun WaterPitcher(
    fraction: Float,
    paceFraction: Float?,
    capacityMl: Int,
    description: String,
    modifier: Modifier = Modifier,
) {
    val level by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "level",
    )
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "phase",
    )
    val water = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.colorScheme.onSurfaceVariant
    val mark = MaterialTheme.colorScheme.tertiary

    Canvas(modifier.semantics { contentDescription = description }) {
        val w = size.width
        val h = size.height
        val body = pitcherBody(w, h)
        val top = h * 0.10f
        val bottom = h * 0.96f
        fun yOf(f: Float) = bottom - (bottom - top) * f.coerceIn(0f, 1f)

        drawPath(body, track)
        clipPath(body) {
            if (level > 0.001f) {
                val surfaceY = yOf(level)
                val amp = 3.dp.toPx()
                val wave = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, surfaceY)
                    var x = 0f
                    while (x <= w) {
                        lineTo(x, surfaceY + amp * sin(x / w * 2f * PI.toFloat() * 1.6f + phase))
                        x += 4f
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(wave, Brush.verticalGradient(listOf(water.copy(alpha = 0.6f), water), startY = surfaceY, endY = bottom))
            }
            // ölçek çizgileri
            val step = if (capacityMl <= 1500) 250 else 500
            var ml = step
            while (ml < capacityMl) {
                val y = yOf(ml.toFloat() / capacityMl)
                val len = if (ml % 1000 == 0) w * 0.09f else w * 0.05f
                drawLine(line.copy(alpha = 0.55f), Offset(w * 0.12f, y), Offset(w * 0.12f + len, y), 1.5.dp.toPx())
                ml += step
            }
            // cam parlaması
            drawRoundRect(Color.White.copy(alpha = 0.28f), Offset(w * 0.19f, h * 0.24f), Size(w * 0.04f, h * 0.58f), CornerRadius(w * 0.02f))
            // şu an olman gereken seviye
            paceFraction?.let {
                val y = yOf(it)
                drawLine(
                    mark, Offset(w * 0.12f, y), Offset(w * 0.70f, y), 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                )
            }
        }
        drawPath(body, line, style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round))
        // sap
        val handle = Path().apply {
            moveTo(w * 0.70f, h * 0.28f)
            cubicTo(w * 0.98f, h * 0.22f, w * 0.98f, h * 0.76f, w * 0.70f, h * 0.70f)
        }
        drawPath(handle, line, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
    }
}

/**
 * Daire biçimli su göstergesi (marka görseli): kapasite günlük hedeftir, dalgalı su yükselir,
 * dış çift halka altın tonudur, kesik çizgi şu an olman gereken seviyedir.
 */
@Composable
fun WaterOrb(
    fraction: Float,
    paceFraction: Float?,
    description: String,
    modifier: Modifier = Modifier,
) {
    val level by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "orbLevel",
    )
    val phase by rememberInfiniteTransition(label = "orbWave").animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "orbPhase",
    )
    val water = MaterialTheme.colorScheme.primary
    val deep = MaterialTheme.colorScheme.onPrimaryContainer
    val track = MaterialTheme.colorScheme.surfaceVariant
    val ring = MaterialTheme.colorScheme.tertiary
    val mark = MaterialTheme.colorScheme.onSurface

    Canvas(modifier.semantics { contentDescription = description }) {
        val d = size.minDimension
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = d / 2f - 2.dp.toPx()
        val inner = outer - 9.dp.toPx()
        val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, inner)) }
        val top = c.y - inner
        val bottom = c.y + inner
        fun yOf(f: Float) = bottom - (bottom - top) * f.coerceIn(0f, 1f)

        drawCircle(ring.copy(alpha = 0.85f), outer, c, style = Stroke(1.5.dp.toPx()))
        drawCircle(ring.copy(alpha = 0.45f), outer - 4.dp.toPx(), c, style = Stroke(1.dp.toPx()))
        drawCircle(track, inner, c)
        clipPath(disc) {
            if (level > 0.001f) {
                val sy = yOf(level)
                val amp = 3.dp.toPx()
                fun wave(shift: Float, off: Float) = Path().apply {
                    moveTo(c.x - inner, bottom)
                    lineTo(c.x - inner, sy + off)
                    var x = c.x - inner
                    while (x <= c.x + inner) {
                        lineTo(x, sy + off + amp * sin((x - c.x + inner) / d * 2f * PI.toFloat() * 1.4f + phase + shift))
                        x += 4f
                    }
                    lineTo(c.x + inner, bottom)
                    close()
                }
                drawPath(wave(1.6f, 3.dp.toPx()), water.copy(alpha = 0.45f))
                drawPath(wave(0f, 0f), Brush.verticalGradient(listOf(water, deep.copy(alpha = 0.9f)), startY = sy, endY = bottom))
            }
            paceFraction?.let {
                val y = yOf(it)
                drawLine(
                    mark.copy(alpha = 0.7f), Offset(c.x - inner, y), Offset(c.x + inner, y), 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                )
            }
            drawCircle(Color.White.copy(alpha = 0.18f), inner * 0.5f, Offset(c.x - inner * 0.35f, c.y - inner * 0.45f))
        }
        drawCircle(ring.copy(alpha = 0.6f), inner, c, style = Stroke(1.dp.toPx()))
    }
}

/** Bardak boyutunu gösteren küçük simge; büyük hacim daha büyük çizilir. */
@Composable
fun CupIcon(ml: Int, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.onSurface
    Canvas(modifier.size(36.dp)) {
        val s = 0.55f + 0.45f * (ml / 500f).coerceAtMost(1f)
        val w = size.width * s
        val h = size.height * s
        translate((size.width - w) / 2f, size.height - h) {
            drawPath(glassPath(w, h), line, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
        }
    }
}
