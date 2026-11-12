package com.lyldz.ab.core

import com.lyldz.ab.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DayTest {
    @Test fun normalDayIs24h() {
        val (a, b) = dayBoundsMs(LocalDate.of(2026, 6, 1), ZoneId.of("UTC"))
        assertEquals(24 * 3600 * 1000L, b - a)
    }

    @Test fun dstDayIs23h() {
        val (a, b) = dayBoundsMs(LocalDate.of(2026, 3, 8), ZoneId.of("America/New_York"))
        assertEquals(23 * 3600 * 1000L, b - a)
    }

    @Test fun staleTemperatureIsIgnored() {
        val today = LocalDate.of(2026, 7, 1)
        val s = Settings(weightKg = 70, tempC = 33.0, tempDay = "2026-06-30")
        assertEquals(1750, s.goalMl(today))
        assertEquals(2500, s.copy(tempDay = today.toString()).goalMl(today))
    }

    @Test fun withMinuteOfDayKeepsDate() {
        val zone = ZoneId.of("UTC")
        val at = LocalDate.of(2026, 6, 1).atTime(9, 10).atZone(zone).toInstant().toEpochMilli()
        val want = LocalDate.of(2026, 6, 1).atTime(14, 5).atZone(zone).toInstant().toEpochMilli()
        assertEquals(want, withMinuteOfDay(at, 14 * 60 + 5, zone))
    }

    @Test fun overrideWins() {
        assertEquals(3000, Settings(goalOverrideMl = 3000).goalMl(LocalDate.of(2026, 7, 1)))
    }
}
