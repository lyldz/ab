package com.lyldz.ab.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatsTest {
    private val utc = ZoneId.of("UTC")
    private fun ms(d: LocalDate, h: Int) = d.atTime(h, 0).atZone(utc).toInstant().toEpochMilli()

    @Test fun groupsByLocalDayAndFillsEmptyDays() {
        val d1 = LocalDate.of(2026, 6, 1)
        val entries = listOf(ms(d1, 9) to 200, ms(d1, 20) to 300, ms(d1.plusDays(2), 8) to 500)
        val s = buildDayStats(entries, emptyMap(), d1, 3, utc) { 2000 }
        assertEquals(listOf(500, 0, 500), s.map { it.ml })
        assertEquals(d1.plusDays(1), s[1].day)
    }

    @Test fun snapshotGoalBeatsFallback() {
        val d1 = LocalDate.of(2026, 6, 1)
        val s = buildDayStats(emptyList(), mapOf("2026-06-02" to 2750), d1, 2, utc) { 2000 }
        assertEquals(listOf(2000, 2750), s.map { it.goalMl })
    }

    @Test fun midnightBelongsToNextDayInZone() {
        // 23:30 UTC, Istanbul (UTC+3) saatiyle ertesi gün
        val ist = ZoneId.of("Europe/Istanbul")
        val at = LocalDate.of(2026, 6, 1).atTime(23, 30).atZone(utc).toInstant().toEpochMilli()
        val s = buildDayStats(listOf(at to 250), emptyMap(), LocalDate.of(2026, 6, 1), 2, ist) { 2000 }
        assertEquals(listOf(0, 250), s.map { it.ml })
    }

    @Test fun summaryUsesRecordedDaysOnly() {
        val d = LocalDate.of(2026, 6, 1)
        val stats = listOf(DayStat(d, 2000, 2000), DayStat(d.plusDays(1), 0, 2000), DayStat(d.plusDays(2), 1000, 2000))
        val sum = summarize(stats)
        assertEquals(1500, sum.avgMl)
        assertEquals(1, sum.reachedDays)
        assertEquals(2, sum.recordedDays)
        assertEquals(2000, sum.best!!.ml)
    }

    @Test fun emptySummary() {
        val sum = summarize(listOf(DayStat(LocalDate.of(2026, 6, 1), 0, 2000)))
        assertEquals(0, sum.avgMl)
        assertEquals(0, sum.recordedDays)
        assertNull(sum.best)
    }
}
