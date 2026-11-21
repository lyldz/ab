package com.lyldz.ab.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Günün [başlangıç, bitiş) sınırları (ms). Yaz saati günlerinde 24 saat değildir. */
fun dayBoundsMs(day: LocalDate, zone: ZoneId): Pair<Long, Long> =
    day.atStartOfDay(zone).toInstant().toEpochMilli() to
        day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

/** Kaydın tarihini koruyup saatini (günün dakikası, 0..1439) değiştirir. */
fun withMinuteOfDay(atMs: Long, minute: Int, zone: ZoneId): Long {
    val clamped = minute.coerceIn(0, 24 * 60 - 1)
    return Instant.ofEpochMilli(atMs).atZone(zone).toLocalDate().atStartOfDay(zone)
        .plusMinutes(clamped.toLong()).toInstant().toEpochMilli()
}

/** Bir sonraki gece yarısına (ertesi günün başlangıcına) kalan milisaniye (en az 1 ms). */
fun msUntilNextMidnight(now: ZonedDateTime): Long {
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return (nextMidnight.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()).coerceAtLeast(1L)
}
