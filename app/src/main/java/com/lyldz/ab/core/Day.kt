package com.lyldz.ab.core

import java.time.LocalDate
import java.time.ZoneId

/** Günün [başlangıç, bitiş) sınırları (ms). Yaz saati günlerinde 24 saat değildir. */
fun dayBoundsMs(day: LocalDate, zone: ZoneId): Pair<Long, Long> =
    day.atStartOfDay(zone).toInstant().toEpochMilli() to
        day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
