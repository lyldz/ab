package com.lyldz.ab.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.lyldz.ab.core.SloshSpring
import com.lyldz.ab.core.tiltDegrees
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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

        // hale: doluluk arttıkça açık aquadan derin maviye döner, güçlenir ve dışa yayılır
        val f = level.coerceIn(0f, 1f)
        val haloColor = lerp(AbColors.Aqua1, Color(0xFF0A7FA8), f)
        val haloAlpha = 0.20f + 0.55f * f
        val solid = ring / outer
        drawCircle(
            Brush.radialGradient(
                colorStops = arrayOf(
                    0f to haloColor.copy(alpha = haloAlpha),
                    solid to haloColor.copy(alpha = haloAlpha),
                    solid + (1f - solid) * 0.45f to haloColor.copy(alpha = haloAlpha * 0.45f),
                    1f to Color.Transparent,
                ),
                center = c, radius = outer,
            ),
            outer, c,
        )
        drawCircle(card, ring, c)
        drawCircle(lineC, ring, c, style = Stroke(1.5.dp.toPx()))
        drawCircle(
            Brush.verticalGradient(listOf(card, lineC), c.y - ring, c.y + ring),
            ring - 2.dp.toPx(), c, style = Stroke(6.dp.toPx()),
        )

        val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, inner)) }
        val top = c.y - inner
        val bottom = c.y + inner
        fun yOf(f: Float) = bottom - (bottom - top) * f.coerceIn(0f, 1f)
        clipPath(disc) {
            if (level > 0.001f) {
                val m = tilt.value // x: eğim (derece), y: sallanma şiddeti 0..1 (yalnızca çizim aşamasında okunur)
                val sy = yOf(level)
                val wide = inner * 1.7f // döndürünce daireyi kaplasın diye geniş çizilir
                fun wave(phase: Float, amp: Float, freq: Float, off: Float) = Path().apply {
                    moveTo(c.x - wide, c.y + inner * 2.4f)
                    lineTo(c.x - wide, sy + off)
                    var x = c.x - wide
                    while (x <= c.x + wide) {
                        lineTo(x, sy + off + amp * sin((x - c.x + inner) / (inner * 2f) * 2f * PI.toFloat() * freq + phase))
                        x += 4f
                    }
                    lineTo(c.x + wide, c.y + inner * 2.4f)
                    close()
                }
                val k = 1f + 2.2f * m.y // telefon sallanınca dalgalar büyür
                // su yer çekimine göre yatay kalır: sahne eğimin tersine döner
                rotate(m.x, c) {
                    // koyuda su biraz daha sönük: üstündeki beyaz yazı okunaklı kalsın
                    drawPath(wave(phaseB, 6.dp.toPx() * k, 1.1f, 6.dp.toPx()), AbColors.Aqua2.copy(alpha = if (dark) 0.35f else 0.65f))
                    drawPath(wave(phaseA, 5.dp.toPx() * k, 1.7f, 0f), AbColors.Aqua1.copy(alpha = if (dark) 0.55f else 0.85f))
                }
            }
        }
    }
}

/**
 * Telefonun ivmeölçeri (izin gerekmez) suyu hareket ettirir: x = suyun dönüş açısı (derece), y = sallanma şiddeti 0..1.
 * İvmeölçer etkin yerçekimini verir (sallamayı da içerir), bu yüzden eğim ondan alınır: el titremesini süzmek için EMA
 * (alfa 0,15), sonra yaylı sönümle (sıvı gibi bir miktar sallanır). Ayrıca ani yan ivme (ham değerin farkı) ikinci bir
 * yayı iter: su dengeye gelirken yüzeyi birkaç kez gidip gelir. Yalnızca ekran açıkken (RESUME/PAUSE) ve Bugün sekmesi
 * görünürken (bileşen çizilirken) dinler; sensör yoksa ya da [enabled] kapalıysa 0.
 * shortcut: yalnızca ivmeölçer; telefon yüz yukarıyken eğim 0 sayılır.
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
        val slosh = SloshSpring()
        var last = 0L
        while (true) {
            infiniteFrame { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                val target = tiltDegrees(shared[0], shared[1])
                vel += (-70f * (angle - target) - 9f * vel) * dt // eğim yayı (hafif sönümsüz: biraz sallanır)
                angle += vel * dt
                slosh.step(-shared[2] * 3f, dt) // yan ivme: su ters yöne yığılır
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
