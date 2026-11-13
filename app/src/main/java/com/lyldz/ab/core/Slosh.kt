package com.lyldz.ab.core

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Suyun yüzeyi etkin yerçekimine diktir; ivmeölçer (ax, ay) bunu verir. Dönüş: Compose'ta saat yönünde pozitif
 * derece (eksi: saat yönünün tersi); telefonu saat yönünde yatırınca ax < 0 olur, su sahnesi ters döner.
 * Telefon yüz yukarı yatıyorsa (düzlem bileşeni çok küçük) eğim belirsizdir: 0.
 */
fun tiltDegrees(ax: Float, ay: Float, maxDeg: Float = 60f): Float {
    if (sqrt(ax * ax + ay * ay) < 2f) return 0f
    return Math.toDegrees(atan2(ax.toDouble(), ay.toDouble())).toFloat().coerceIn(-maxDeg, maxDeg)
}

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
