package com.lyldz.ab.core

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Suyun yüzeyi etkin yerçekimine diktir; ivmeölçer (ax, ay) bunu verir. Dönüş: Compose'ta saat yönünde pozitif derece
 * (eksi: saat yönünün tersi), tam tur (-180..180): telefon baş aşağıyken ay < 0 olur ve açı ±180°'ye yaklaşır, su
 * yine yer çekimini izler (ekranın üst tarafında toplanır). Telefon yüz yukarı/aşağı yatıyorsa (düzlem bileşeni çok
 * küçük) eğim belirsizdir: [previous] korunur, su ani zıplamaz.
 */
fun tiltDegrees(ax: Float, ay: Float, previous: Float = 0f): Float {
    if (sqrt(ax * ax + ay * ay) < 2f) return previous
    return Math.toDegrees(atan2(ax.toDouble(), ay.toDouble())).toFloat()
}

/** Açıyı (-180, 180] aralığına getirir. */
fun wrap180(deg: Float): Float {
    var d = deg % 360f
    if (d > 180f) d -= 360f
    if (d <= -180f) d += 360f
    return d
}

/** [target] ile aynı yönü gösteren açılardan [reference]'a en yakın olanı: yay ±180° sınırında uzun yoldan dönmez. */
fun unwrapNear(target: Float, reference: Float): Float = reference + wrap180(target - reference)

/** Kütle-yay-sönümleyici: [force] ani itme verir, su kendiliğinden sakinleşir. [pos] -1..1 aralığında kalır. */
class SloshSpring(private val stiffness: Float = 90f, private val damping: Float = 5f) {
    var pos = 0f
        private set
    var vel = 0f
        private set

    fun step(force: Float, dt: Float) {
        vel += (force - stiffness * pos - damping * vel) * dt
        pos = (pos + vel * dt).coerceIn(-1f, 1f)
        if (abs(pos) >= 1f) vel = 0f // duvara çarpınca durur
    }
}
