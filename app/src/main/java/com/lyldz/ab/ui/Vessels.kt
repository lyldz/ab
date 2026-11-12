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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.graphics.drawscope.rotate
import com.lyldz.ab.ui.theme.AbColors
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
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
 * Günlük ilerleme göstergesi (tasarım sistemi): dışta neon aqua doluluk halkası, içte iki katmanlı,
 * farklı faz ve hızda dalgalanan su. Kapasite günlük hedeftir; [paceFraction] varsa şu an olman
 * gereken seviye lacivert kesik çizgiyle gösterilir. Ortadaki yazıyı çağıran koyar.
 */
@Composable
fun WaterOrb(
    fraction: Float,
    paceFraction: Float?,
    description: String,
    modifier: Modifier = Modifier,
) {
    // Sistem "animasyon ölçeği" kapalıyken (pil tasarrufu, geliştirici seçeneği) Compose'un animate*
    // API'leri durur; gösterge canlı kalsın diye kare saatine bağlı kendi animasyonumuzu kullanıyoruz.
    val level by rememberSmoothed(fraction.coerceIn(0f, 1f))
    val sweep = level
    val phaseA by rememberPhase(3600)
    val phaseB by rememberPhase(5200, reverse = true)
    val navy = AbColors.Navy

    Canvas(modifier.semantics { contentDescription = description }) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val stroke = 12.dp.toPx()
        val ringR = size.minDimension / 2f - stroke
        val inner = ringR - stroke / 2f - 8.dp.toPx()
        val arcTopLeft = Offset(c.x - ringR, c.y - ringR)
        val arcSize = Size(ringR * 2, ringR * 2)

        // halka: iz, yumuşak parlama, doluluk
        drawCircle(AbColors.Aqua1.copy(alpha = 0.22f), ringR, c, style = Stroke(stroke))
        if (sweep > 0.002f) {
            val brush = Brush.sweepGradient(listOf(AbColors.Aqua1, AbColors.Aqua2, AbColors.Aqua1), c)
            rotate(-90f, c) {
                drawArc(brush, 0f, 360f * sweep, false, arcTopLeft, arcSize, 0.3f, Stroke(stroke + 10.dp.toPx(), cap = StrokeCap.Round))
                drawArc(brush, 0f, 360f * sweep, false, arcTopLeft, arcSize, 1f, Stroke(stroke, cap = StrokeCap.Round))
            }
        }

        // iç: inci zemin + iki katman dalga
        val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, inner)) }
        drawCircle(AbColors.Pearl, inner, c)
        val top = c.y - inner
        val bottom = c.y + inner
        fun yOf(f: Float) = bottom - (bottom - top) * f.coerceIn(0f, 1f)
        clipPath(disc) {
            if (level > 0.001f) {
                val sy = yOf(level)
                fun wave(phase: Float, amp: Float, freq: Float, off: Float) = Path().apply {
                    moveTo(c.x - inner, bottom)
                    lineTo(c.x - inner, sy + off)
                    var x = c.x - inner
                    while (x <= c.x + inner) {
                        lineTo(x, sy + off + amp * sin((x - c.x + inner) / (inner * 2f) * 2f * PI.toFloat() * freq + phase))
                        x += 4f
                    }
                    lineTo(c.x + inner, bottom)
                    close()
                }
                drawPath(wave(phaseB, 4.dp.toPx(), 1.1f, 4.dp.toPx()), AbColors.Aqua1.copy(alpha = 0.45f))
                drawPath(
                    wave(phaseA, 3.dp.toPx(), 1.6f, 0f),
                    Brush.verticalGradient(listOf(AbColors.Aqua2.copy(alpha = 0.85f), AbColors.Aqua1), startY = sy, endY = bottom),
                )
            }
            paceFraction?.let {
                val y = yOf(it)
                drawLine(
                    navy.copy(alpha = 0.75f), Offset(c.x - inner, y), Offset(c.x + inner, y), 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 9f)),
                )
            }
        }
    }
}

/** [target]'a her karede üstel yaklaşan değer (yaklaşık 0,3 sn); sistem animasyon ölçeğinden bağımsız. */
@Composable
private fun rememberSmoothed(target: Float): State<Float> {
    val shown = remember { mutableFloatStateOf(0f) }
    val goal = rememberUpdatedState(target)
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else (now - last) / 1e9f
                last = now
                val k = 1f - exp(-dt / 0.3f)
                val cur = shown.floatValue
                shown.floatValue = if (abs(goal.value - cur) < 0.0005f) goal.value else cur + (goal.value - cur) * k
            }
        }
    }
    return shown
}

/** 0..2π arasında dönen faz; [periodMs] bir tur süresi. */
@Composable
private fun rememberPhase(periodMs: Int, reverse: Boolean = false): State<Float> {
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        val twoPi = (2 * PI).toFloat()
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val step = (now - last) / 1e6f / periodMs * twoPi
                    phase.floatValue = ((phase.floatValue + if (reverse) -step else step) % twoPi + twoPi) % twoPi
                }
                last = now
            }
        }
    }
    return phase
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
