package com.lyldz.ab.core

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionTest {
    @Test fun seriesFollowsMinorVersion() {
        assertEquals("Damla", seriesName("0.2.0"))
        assertEquals("Testi", seriesName("0.3.0"))
        assertEquals("Testi", seriesName("0.3.1"))
        assertEquals("Testi", seriesName("v0.3.12"))
        assertEquals("Pınar", seriesName("0.4.0"))
        assertEquals("", seriesName("9.9.9"))
        assertEquals("", seriesName("bozuk"))
    }
}
