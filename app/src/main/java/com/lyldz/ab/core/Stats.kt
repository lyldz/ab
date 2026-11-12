package com.lyldz.ab.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayStat(val day: LocalDate, val ml: Int, val goalMl: Int) {
    val reached: Boolean get() = ml >= goalMl
}

data class StatSummary(val avgMl: Int, val reachedDays: Int, val recordedDays: Int, val best: DayStat?)

/**
 * [start]'tan [days] gün için günlük toplamlar. Günün hedefi kayıtlı anlık değerden (goals),
 * yoksa [fallbackGoal]'dan gelir: sıcaklık gibi o güne özgü ekler sonradan bilinemez.
 */
fun buildDayStats(
    entries: List<Pair<Long, Int>>,
    goals: Map<String, Int>,
    start: LocalDate,
    days: Int,
    zone: ZoneId,
    fallbackGoal: (LocalDate) -> Int,
): List<DayStat> {
    val byDay = entries
        .groupBy({ Instant.ofEpochMilli(it.first).atZone(zone).toLocalDate() }, { it.second })
        .mapValues { it.value.sum() }
    return (0 until days).map { i ->
        val d = start.plusDays(i.toLong())
        DayStat(d, byDay[d] ?: 0, goals[d.toString()] ?: fallbackGoal(d))
    }
}

/** Ortalama ve "hedefe ulaşılan gün" yalnızca kaydı olan günler üzerinden hesaplanır. */
fun summarize(stats: List<DayStat>): StatSummary {
    val recorded = stats.filter { it.ml > 0 }
    return StatSummary(
        avgMl = if (recorded.isEmpty()) 0 else recorded.sumOf { it.ml } / recorded.size,
        reachedDays = recorded.count { it.reached },
        recordedDays = recorded.size,
        best = recorded.maxByOrNull { it.ml },
    )
}
