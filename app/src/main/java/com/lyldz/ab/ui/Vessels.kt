package com.lyldz.ab.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.runtime.withFrameNanos
import kotlin.coroutines.coroutineContext
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.lyldz.ab.ui.theme.Ab
import com.lyldz.ab.ui.theme.AbColors
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
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

/**
 * Günlük ilerleme göstergesi (tasarım sistemi): yumuşak aqua ışıltı halkası, inci iç yüzey ve iki
 * katmanlı su (arka #A0E7E5 %65, ön #88D9E6 %85) farklı faz ve hızda dalgalanır. Kapasite günlük
 * hedeftir. Çevredeki hale su doldukça koyulaşır, güçlenir ve rengini çevreye yayar.
 * Ortadaki yazıyı çağıran koyar.
 */
@Composable
fun WaterOrb(
    fraction: Float,
    description: String,
    modifier: Modifier = Modifier,
    motion: Boolean = true,
    waveAnimation: Boolean = true,
    showOverflow: Boolean = true,
) {
    // Sistem "animasyon ölçeği" kapalıyken (pil tasarrufu, geliştirici seçeneği) Compose'un animate*
    // API'leri durur; gösterge canlı kalsın diye kare saatine bağlı kendi animasyonumuzu kullanıyoruz.
    val level by rememberSmoothed(fraction.coerceIn(0f, 1f))
    val overflow by rememberSmoothed(if (showOverflow) ((fraction - 1f) / 0.5f).coerceIn(0f, 1f) else 0f)
    val phaseA by rememberPhase(4600, enabled = waveAnimation)
    val phaseB by rememberPhase(6400, reverse = true, enabled = waveAnimation)
    val ripple by rememberFillRipple(fraction, enabled = motion)

    val card = Ab.card
    val lineC = Ab.line
    val dark = Ab.isDark
    Canvas(modifier.semantics { contentDescription = description }) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f
        val ring = outer * 0.82f
        val inner = ring - 7.dp.toPx()

        // hale: doluluk arttıkça açık aquadan derin okyanus mavisine döner; açık temada damlacıkları yutmaması için dengeli
        val f = level.coerceIn(0f, 1f)
        val ov = overflow.coerceIn(0f, 1f)
        val haloColor = if (dark) lerp(AbColors.Aqua1, AbColors.Ocean, f) else lerp(AbColors.Aqua1, AbColors.Ocean, 0.20f + 0.55f * f)
        val haloAlpha = if (dark) {
            (0.16f + 0.42f * f + 0.14f * ov).coerceAtMost(0.88f)
        } else {
            (0.09f + 0.23f * f + 0.06f * ov).coerceAtMost(0.38f)
        }
        val solid = ring / outer
        drawCircle(
            Brush.radialGradient(
                colorStops = arrayOf(
                    0f to haloColor.copy(alpha = haloAlpha),
                    solid to haloColor.copy(alpha = haloAlpha),
                    solid + (1f - solid) * 0.48f to haloColor.copy(alpha = haloAlpha * if (dark) 0.42f else 0.30f),
                    1f to Color.Transparent,
                ),
                center = c, radius = outer,
            ),
            outer, c,
        )
        // hedef aşıldığında kabın kenarından dışarı taşan dalgalı su halkası ve damlacıklar
        if (ov > 0.01f) {
            val maxSpill = (outer - ring) * (0.42f + 0.48f * ov)
            val spillPath = Path().apply {
                val steps = 72
                for (i in 0..steps) {
                    val ang = i.toFloat() / steps * 2f * PI.toFloat()
                    val wave = 0.5f + 0.5f * sin(ang * 5f + phaseA) * 0.65f + 0.35f * cos(ang * 3f - phaseB)
                    val r = ring + maxSpill * (0.35f + 0.65f * wave)
                    val x = c.x + r * cos(ang)
                    val y = c.y + r * sin(ang)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            val spillStop = (ring / outer).coerceIn(0f, 0.9f)
            if (dark) {
                drawPath(
                    spillPath,
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            spillStop to AbColors.Aqua1.copy(alpha = 0.42f * ov),
                            1f to AbColors.Aqua2.copy(alpha = 0.06f * ov),
                        ),
                        center = c,
                        radius = outer,
                    ),
                )
            } else {
                drawPath(
                    spillPath,
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            spillStop to AbColors.Teal.copy(alpha = 0.48f * ov),
                            (spillStop + 1f) * 0.5f to AbColors.Aqua1.copy(alpha = 0.28f * ov),
                            1f to AbColors.DeepTeal.copy(alpha = 0.14f * ov),
                        ),
                        center = c,
                        radius = outer,
                    ),
                )
                drawPath(
                    spillPath,
                    color = AbColors.DeepTeal.copy(alpha = 0.58f * ov),
                    style = Stroke(width = 1.25.dp.toPx(), join = StrokeJoin.Round),
                )
            }
            // kenardan dışarı süzülen 8 adet taşma damlacığı (açık temada 4 katmanlı cam su zerreciği)
            val twoPi = (2 * PI).toFloat()
            val dropCount = 8
            for (i in 0 until dropCount) {
                val progress = ((phaseA / twoPi) + i.toFloat() / dropCount) % 1f
                val ang = i * (twoPi / dropCount) + 0.26f * sin(phaseB + i * 1.3f)
                val dist = ring + (outer - ring) * (0.16f + 0.78f * progress)
                val sizeScale = when (i % 3) { 0 -> 1.15f; 1 -> 0.92f; else -> 0.76f }
                val dropR = ((2.3f + 2.3f * ov) * sizeScale).dp.toPx() * (1f - 0.32f * progress)
                val center = Offset(c.x + dist * cos(ang), c.y + dist * sin(ang))
                if (dark) {
                    val dropAlpha = sin(progress * PI.toFloat()) * 0.78f * (0.45f + 0.55f * ov)
                    drawCircle(
                        color = if (i % 2 == 0) AbColors.Aqua1.copy(alpha = dropAlpha) else AbColors.Aqua2.copy(alpha = dropAlpha),
                        radius = dropR,
                        center = center,
                    )
                } else {
                    val dropAlpha = sin(progress * PI.toFloat()) * 0.92f * (0.50f + 0.50f * ov)
                    // 1) hafif dış gölge
                    drawCircle(
                        color = AbColors.Navy.copy(alpha = 0.12f * dropAlpha),
                        radius = dropR * 1.12f,
                        center = center + Offset(0f, 0.8.dp.toPx()),
                    )
                    // 2) kontrastlı cam damlacık gövdesi
                    val edgeColor = when (i % 3) { 0 -> AbColors.Ocean; 1 -> AbColors.DeepTeal; else -> AbColors.Teal }
                    val coreColor = when (i % 3) { 0 -> AbColors.Teal; 1 -> AbColors.Aqua1; else -> AbColors.Aqua2 }
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(coreColor.copy(alpha = dropAlpha), edgeColor.copy(alpha = dropAlpha)),
                            center = center - Offset(dropR * 0.2f, dropR * 0.2f),
                            radius = (dropR * 1.15f).coerceAtLeast(1f),
                        ),
                        radius = dropR,
                        center = center,
                    )
                    // 3) ince yüzey gerilimi kenarlığı
                    drawCircle(
                        color = AbColors.DeepTeal.copy(alpha = 0.62f * dropAlpha),
                        radius = dropR,
                        center = center,
                        style = Stroke(width = 0.85.dp.toPx()),
                    )
                    // 4) sol üst beyaz ışık yansıması
                    drawCircle(
                        color = Color.White.copy(alpha = 0.88f * dropAlpha),
                        radius = (dropR * 0.28f).coerceAtLeast(0.7.dp.toPx()),
                        center = center + Offset(-dropR * 0.32f, -dropR * 0.32f),
                    )
                }
            }
        }
        drawCircle(card, ring, c)
        drawCircle(lineC, ring, c, style = Stroke(1.5.dp.toPx()))
        drawCircle(
            Brush.verticalGradient(
                listOf(
                    if (dark) AbColors.Aqua1.copy(alpha = 0.18f) else Color.White,
                    lineC,
                ),
                c.y - ring,
                c.y + ring,
            ),
            ring - 2.dp.toPx(), c, style = Stroke(5.5.dp.toPx()),
        )

        val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, inner)) }
        val top = c.y - inner
        val bottom = c.y + inner
        fun yOf(f: Float) = bottom - (bottom - top) * f.coerceIn(0f, 1f)
        clipPath(disc) {
            if (level > 0.001f) {
                val sy = yOf(level)
                val wide = inner * 1.15f
                val span = inner * 2f
                fun waveY(x: Float, phase: Float, amp: Float, freq: Float, off: Float): Float {
                    val t = (x - c.x + inner) / span * 2f * PI.toFloat() * freq + phase
                    return sy + off + amp * (sin(t) + 0.18f * sin(2f * t + 1.1f))
                }
                fun wave(phase: Float, amp: Float, freq: Float, off: Float) = Path().apply {
                    moveTo(c.x - wide, c.y + inner * 1.5f)
                    lineTo(c.x - wide, waveY(c.x - wide, phase, amp, freq, off))
                    var x = c.x - wide
                    while (x <= c.x + wide) {
                        lineTo(x, waveY(x, phase, amp, freq, off))
                        x += 3f
                    }
                    lineTo(c.x + wide, c.y + inner * 1.5f)
                    close()
                }
                fun crest(phase: Float, amp: Float, freq: Float) = Path().apply {
                    var x = c.x - wide
                    moveTo(x, waveY(x, phase, amp, freq, 0f))
                    while (x <= c.x + wide) {
                        lineTo(x, waveY(x, phase, amp, freq, 0f))
                        x += 3f
                    }
                }
                val k = 1f + 0.85f * ripple // su eklenince kısa, sakin dolum dalgalanması
                val gradTop = (sy - 12.dp.toPx()).coerceAtLeast(top)
                val gradBottom = (bottom + inner * 0.25f).coerceAtLeast(gradTop + 1f)
                val backBrush = Brush.verticalGradient(
                    colors = listOf(
                        AbColors.Aqua2.copy(alpha = if (dark) 0.32f else 0.62f),
                        AbColors.Teal.copy(alpha = if (dark) 0.42f else 0.72f),
                    ),
                    startY = gradTop,
                    endY = gradBottom,
                )
                val frontBrush = Brush.verticalGradient(
                    colors = listOf(
                        AbColors.Aqua1.copy(alpha = if (dark) 0.50f else 0.84f),
                        AbColors.Teal.copy(alpha = if (dark) 0.56f else 0.88f),
                        AbColors.DeepTeal.copy(alpha = if (dark) 0.64f else 0.90f),
                    ),
                    startY = gradTop,
                    endY = gradBottom,
                )
                // sakin, akışkan çift katmanlı dalga (ivmeölçer sallanması yok)
                drawPath(wave(phaseB, 4.2.dp.toPx() * k, 1.0f, 4.dp.toPx()), backBrush)
                val frontAmp = 3.6.dp.toPx() * k
                drawPath(wave(phaseA, frontAmp, 1.4f, 0f), frontBrush)
                // su yüzeyinde ince ışık kırılması / parıltı çizgisi
                if (level in 0.03f..0.97f) {
                    drawPath(
                        crest(phaseA, frontAmp, 1.4f),
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = if (dark) 0.42f else 0.72f),
                                Color.White.copy(alpha = 0.08f),
                            ),
                            startX = c.x - inner,
                            endX = c.x + inner,
                        ),
                        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
            // sol üstte yumuşak cam yansıması (minimalist derinlik)
            val arcInset = inner * 0.14f
            drawArc(
                color = Color.White.copy(alpha = if (dark) 0.10f else 0.42f),
                startAngle = 205f,
                sweepAngle = 52f,
                useCenter = false,
                topLeft = Offset(c.x - inner + arcInset, c.y - inner + arcInset),
                size = Size((inner - arcInset) * 2f, (inner - arcInset) * 2f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

/** Su eklendiğinde (~1,4 sn) sönümlenen zarif dolum dalgalanması; ivmeölçer kullanmaz. */
@Composable
private fun rememberFillRipple(fraction: Float, enabled: Boolean): State<Float> {
    val ripple = remember { mutableFloatStateOf(0f) }
    val prev = remember { mutableFloatStateOf(fraction) }
    LaunchedEffect(fraction, enabled) {
        if (!enabled) {
            ripple.floatValue = 0f
            prev.floatValue = fraction
            return@LaunchedEffect
        }
        val delta = fraction - prev.floatValue
        prev.floatValue = fraction
        if (delta > 0.005f) {
            ripple.floatValue = (ripple.floatValue + (delta * 4f).coerceIn(0.4f, 1f)).coerceAtMost(1f)
            var last = 0L
            while (ripple.floatValue > 0.01f) {
                infiniteFrame { now ->
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                    last = now
                    ripple.floatValue = (ripple.floatValue * exp(-dt / 0.55f)).let { if (it < 0.01f) 0f else it }
                }
            }
        }
    }
    return ripple
}

/** Sonsuz kare döngüsü: testlerde InfiniteAnimationPolicy döngüyü sonlandırır, uygulama boşta kalabilir. */
private suspend fun <R> infiniteFrame(onFrame: (Long) -> R): R {
    val policy = coroutineContext[InfiniteAnimationPolicy]
    return if (policy != null) policy.onInfiniteOperation { withFrameNanos(onFrame) } else withFrameNanos(onFrame)
}

/** [target]'a her karede üstel yaklaşan değer (yaklaşık 0,3 sn); sistem animasyon ölçeğinden bağımsız. */
@Composable
private fun rememberSmoothed(target: Float): State<Float> {
    val shown = remember { mutableFloatStateOf(target) }
    val running = remember { mutableStateOf(false) }
    val goal = rememberUpdatedState(target)
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            infiniteFrame { now ->
                running.value = true
                val dt = if (last == 0L) 0f else (now - last) / 1e9f
                last = now
                val k = 1f - exp(-dt / 0.3f)
                val cur = shown.floatValue
                shown.floatValue = if (abs(goal.value - cur) < 0.0005f) goal.value else cur + (goal.value - cur) * k
            }
        }
    }
    // Döngü hiç çalışmazsa (ör. testlerde sonsuz animasyonlar kapalıyken) hedef değeri doğrudan göster
    return remember { derivedStateOf { if (running.value) shown.floatValue else goal.value } }
}

/** 0..2π arasında dönen faz; [periodMs] bir tur süresi. */
@Composable
private fun rememberPhase(periodMs: Int, reverse: Boolean = false, enabled: Boolean = true): State<Float> {
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        var last = 0L
        val twoPi = (2 * PI).toFloat()
        while (true) {
            infiniteFrame { now ->
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
