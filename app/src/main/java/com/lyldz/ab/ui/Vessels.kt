package com.lyldz.ab.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.lyldz.ab.core.SloshSpring
import com.lyldz.ab.core.tiltDegrees
import com.lyldz.ab.core.unwrapNear
import com.lyldz.ab.core.wrap180
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import androidx.compose.ui.graphics.drawscope.rotate
import com.lyldz.ab.ui.theme.Ab
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
) {
    // Sistem "animasyon ölçeği" kapalıyken (pil tasarrufu, geliştirici seçeneği) Compose'un animate*
    // API'leri durur; gösterge canlı kalsın diye kare saatine bağlı kendi animasyonumuzu kullanıyoruz.
    val level by rememberSmoothed(fraction.coerceIn(0f, 1f))
    val phaseA by rememberPhase(3600)
    val phaseB by rememberPhase(5200, reverse = true)
    val tilt = rememberTilt(motion)

    val card = Ab.card
    val lineC = Ab.line
    val dark = Ab.isDark
    Canvas(modifier.semantics { contentDescription = description }) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f
        val ring = outer * 0.82f
        val inner = ring - 7.dp.toPx()

        // hale: doluluk arttıkça açık aquadan derin okyanus mavisine döner, güçlenir ve dışa yayılır
        val f = level.coerceIn(0f, 1f)
        val haloColor = lerp(AbColors.Aqua1, AbColors.Ocean, f)
        val haloAlpha = (if (dark) 0.16f else 0.22f) + (if (dark) 0.42f else 0.50f) * f
        val solid = ring / outer
        drawCircle(
            Brush.radialGradient(
                colorStops = arrayOf(
                    0f to haloColor.copy(alpha = haloAlpha),
                    solid to haloColor.copy(alpha = haloAlpha),
                    solid + (1f - solid) * 0.48f to haloColor.copy(alpha = haloAlpha * 0.42f),
                    1f to Color.Transparent,
                ),
                center = c, radius = outer,
            ),
            outer, c,
        )
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
                val m = tilt.value // x: eğim (derece), y: sallanma şiddeti 0..1 (yalnızca çizim aşamasında okunur)
                val sy = yOf(level)
                val wide = inner * 1.75f // döndürünce daireyi kaplasın diye geniş çizilir
                val span = inner * 2f
                fun waveY(x: Float, phase: Float, amp: Float, freq: Float, off: Float): Float {
                    val t = (x - c.x + inner) / span * 2f * PI.toFloat() * freq + phase
                    return sy + off + amp * (sin(t) + 0.22f * sin(2f * t + 1.1f))
                }
                fun wave(phase: Float, amp: Float, freq: Float, off: Float) = Path().apply {
                    moveTo(c.x - wide, c.y + inner * 2.4f)
                    lineTo(c.x - wide, waveY(c.x - wide, phase, amp, freq, off))
                    var x = c.x - wide
                    while (x <= c.x + wide) {
                        lineTo(x, waveY(x, phase, amp, freq, off))
                        x += 3f
                    }
                    lineTo(c.x + wide, c.y + inner * 2.4f)
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
                val k = 1f + 2.2f * m.y // telefon sallanınca dalgalar büyür
                val gradTop = (sy - 12.dp.toPx()).coerceAtLeast(top)
                val gradBottom = (bottom + inner * 0.35f).coerceAtLeast(gradTop + 1f)
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
                // su yer çekimine göre yatay kalır: sahne eğimin tersine döner
                rotate(m.x, c) {
                    // koyuda su biraz daha dengeli saydamlıkta: üstündeki açık yazı okunaklı kalsın
                    drawPath(wave(phaseB, 5.5.dp.toPx() * k, 1.05f, 5.dp.toPx()), backBrush)
                    val frontAmp = 4.8.dp.toPx() * k
                    drawPath(wave(phaseA, frontAmp, 1.55f, 0f), frontBrush)
                    // su yüzeyinde ince ışık kırılması / parıltı çizgisi
                    if (level in 0.03f..0.97f) {
                        drawPath(
                            crest(phaseA, frontAmp, 1.55f),
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

/**
 * Telefonun ivmeölçeri (izin gerekmez) suyu hareket ettirir: x = suyun dönüş açısı (derece), y = sallanma şiddeti 0..1.
 * İvmeölçer etkin yerçekimini verir (sallamayı da içerir), bu yüzden eğim ondan alınır: el titremesini süzmek için EMA
 * (alfa 0,15), sonra yaylı sönümle (sıvı gibi bir miktar sallanır). Ayrıca ani yan ivme (ham değerin farkı) ikinci bir
 * yayı iter: su dengeye gelirken yüzeyi birkaç kez gidip gelir. Yalnızca ekran açıkken (RESUME/PAUSE) ve Bugün sekmesi
 * görünürken (bileşen çizilirken) dinler; sensör yoksa ya da [enabled] kapalıysa 0.
 * shortcut: yalnızca ivmeölçer; telefon yüz yukarı/aşağı yatıkken son açı korunur.
 */
@Composable
private fun rememberTilt(enabled: Boolean): State<Offset> {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val out = remember { mutableStateOf(Offset.Zero) }
    val shared = remember { floatArrayOf(0f, 9.8f, 0f) } // [süzülmüş ax, süzülmüş ay, kare başına biriken yan ivme farkı]
    if (enabled) {
        DisposableEffect(owner) {
            val sm = ctx.getSystemService(SensorManager::class.java)
            val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            var lastAx = Float.NaN
            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    val ax = e.values[0]
                    val ay = e.values[1]
                    shared[0] += 0.15f * (ax - shared[0])
                    shared[1] += 0.15f * (ay - shared[1])
                    if (!lastAx.isNaN()) shared[2] += ax - lastAx
                    lastAx = ax
                }
                override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
            }
            val obs = LifecycleEventObserver { _, ev ->
                if (sm != null && sensor != null) {
                    when (ev) {
                        Lifecycle.Event.ON_RESUME -> sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
                        Lifecycle.Event.ON_PAUSE -> {
                            sm.unregisterListener(listener)
                            lastAx = Float.NaN
                        }
                        else -> {}
                    }
                }
            }
            owner.lifecycle.addObserver(obs)
            onDispose {
                owner.lifecycle.removeObserver(obs)
                sm?.unregisterListener(listener)
            }
        }
    }
    LaunchedEffect(enabled) {
        if (!enabled) {
            out.value = Offset.Zero
            return@LaunchedEffect
        }
        var angle = 0f
        var vel = 0f
        var goal = 0f // son geçerli hedef açı (telefon yatıkken korunur)
        val slosh = SloshSpring()
        var last = 0L
        while (true) {
            infiniteFrame { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                goal = unwrapNear(tiltDegrees(shared[0], shared[1], goal), angle) // en kısa yoldan, baş aşağıda da yer çekimini izler
                vel += (-70f * (angle - goal) - 9f * vel) * dt // eğim yayı (hafif sönümsüz: biraz sallanır)
                angle = wrap180(angle + vel * dt)
                slosh.step(-shared[2] * 40f, dt) // yan ivme: su ters yöne yığılır (5 m/s² fark ≈ 6° yüzey oynaması)
                shared[2] = 0f
                out.value = Offset(angle + slosh.pos * 18f, abs(slosh.pos))
            }
        }
    }
    return out
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
private fun rememberPhase(periodMs: Int, reverse: Boolean = false): State<Float> {
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
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
