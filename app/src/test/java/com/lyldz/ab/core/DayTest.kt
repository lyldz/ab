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

    @Test fun dstFallBackDayIs25h() {
        val (a, b) = dayBoundsMs(LocalDate.of(2026, 11, 1), ZoneId.of("America/New_York"))
        assertEquals(25 * 3600 * 1000L, b - a)
    }

    @Test fun consecutiveDayBoundsAreContiguous() {
        val zone = ZoneId.of("Europe/Istanbul")
        val d1 = LocalDate.of(2026, 12, 31)
        val d2 = LocalDate.of(2027, 1, 1)
        assertEquals(dayBoundsMs(d1, zone).second, dayBoundsMs(d2, zone).first)
    }

    @Test fun leapYearBoundaryIs24h() {
        val zone = ZoneId.of("UTC")
        val (a, b) = dayBoundsMs(LocalDate.of(2028, 2, 29), zone)
        assertEquals(24 * 3600 * 1000L, b - a)
        assertEquals(b, dayBoundsMs(LocalDate.of(2028, 3, 1), zone).first)
    }

    @Test fun withMinuteOfDayHandlesMidnightAndEndOfDay() {
        val zone = ZoneId.of("Europe/Istanbul")
        val d = LocalDate.of(2026, 6, 15)
        val noon = d.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(d.atStartOfDay(zone).toInstant().toEpochMilli(), withMinuteOfDay(noon, 0, zone))
        assertEquals(d.atTime(23, 59).atZone(zone).toInstant().toEpochMilli(), withMinuteOfDay(noon, 23 * 60 + 59, zone))
    }

    @Test fun msUntilNextMidnightComputesRemainingMs() {
        val zone = ZoneId.of("UTC")
        val justBeforeMidnight = LocalDate.of(2026, 6, 1).atTime(23, 59, 30).atZone(zone)
        assertEquals(30_000L, msUntilNextMidnight(justBeforeMidnight))
        val atMidnight = LocalDate.of(2026, 6, 1).atStartOfDay(zone)
        assertEquals(24 * 3600 * 1000L, msUntilNextMidnight(atMidnight))
    }
}
