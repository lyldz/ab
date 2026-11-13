package com.lyldz.ab.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SloshTest {
    @Test fun uprightIsZeroAndClockwiseTiltIsNegative() {
        assertEquals(0f, tiltDegrees(0f, 9.8f), 0.01f)
        assertEquals(-30f, tiltDegrees(-4.9f, 8.487f), 0.2f) // saat yönünde yatık: ax < 0
        assertEquals(30f, tiltDegrees(4.9f, 8.487f), 0.2f)
    }

    @Test fun flatPhoneAndLargeTiltAreHandled() {
        assertEquals(0f, tiltDegrees(0.5f, 0.5f), 0f) // yüz yukarı: belirsiz
        assertEquals(60f, tiltDegrees(9.8f, 0.1f), 0f) // üst sınır
        assertEquals(-60f, tiltDegrees(-9.8f, 0.1f), 0f)
    }

    @Test fun springSettlesAfterImpulseAndStaysBounded() {
        val s = SloshSpring()
        s.step(200f, 0.016f) // yaklaşık 5 m/s² yan ivme farkı × kazanç 40
        var peak = 0f
        repeat(60) { s.step(0f, 0.016f); peak = maxOf(peak, kotlin.math.abs(s.pos)) }
        assertTrue("itme yüzeyi gözle görülür oynatmalı: $peak", peak > 0.2f)
        repeat(400) { s.step(0f, 0.016f) } // ~6 sn
        assertTrue("su sakinleşmeli", kotlin.math.abs(s.pos) < 0.02f && kotlin.math.abs(s.vel) < 0.05f)
        repeat(50) { s.step(500f, 0.016f) }
        assertTrue(kotlin.math.abs(s.pos) <= 1f)
    }
}
