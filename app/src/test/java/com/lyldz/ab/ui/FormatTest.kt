package com.lyldz.ab.ui

import com.lyldz.ab.data.DEFAULT_CUP_PRESETS
import com.lyldz.ab.data.encodeCupPresets
import com.lyldz.ab.data.parseCupPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {
    @Test
    fun mlAndOzConversionsRoundTripReasonably() {
        val oz = mlToOz(250)
        assertTrue(oz in 8.4..8.5)
        assertEquals(250, ozToMl(oz))
        assertEquals("ml", unitLabel(UNIT_ML))
        assertEquals("oz", unitLabel(UNIT_OZ))
    }

    @Test
    fun fmtVolRespectsSelectedUnit() {
        assertTrue(fmtVol(500, UNIT_ML).endsWith(" ml"))
        assertTrue(fmtVol(500, UNIT_OZ).endsWith(" oz"))
    }

    @Test
    fun cupPresetsSanitizeAndDeduplicate() {
        assertEquals(DEFAULT_CUP_PRESETS, parseCupPresets(null))
        assertEquals(DEFAULT_CUP_PRESETS, parseCupPresets("bad,0,-5,9999"))
        assertEquals(listOf(150, 330, 750), parseCupPresets("750, 150, 330, 150, 9999"))
        assertEquals("200,500,750", encodeCupPresets(listOf(750, 200, 500, 200)))
    }
}
