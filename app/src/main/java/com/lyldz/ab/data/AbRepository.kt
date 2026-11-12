package com.lyldz.ab.data

import com.lyldz.ab.core.dayBoundsMs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Uygulama, widget ve bildirim aksiyonlarının ortak tek veri kapısı. */
class AbRepository(
    private val dao: IntakeDao,
    private val store: SettingsStore,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    data class Today(
        val goalMl: Int,
        val drankMl: Int,
        val lastIntakeMin: Int?,
        val settings: Settings,
    )

    val settings: Flow<Settings> = store.flow

    suspend fun saveSettings(s: Settings) = store.save(s)

    /** Gün sınırı abone olunduğu andaki güne sabittir; gün dönünce arayüz yeniden abone olmalı. */
    fun today(): Flow<Today> {
        val day = LocalDate.now(zone)
        val (from, to) = dayBoundsMs(day, zone)
        return combine(store.flow, dao.observeRange(from, to)) { s, list ->
            Today(
                goalMl = s.goalMl(day),
                drankMl = list.sumOf { it.ml },
                lastIntakeMin = list.firstOrNull()?.let {
                    val t = Instant.ofEpochMilli(it.atMs).atZone(zone)
                    t.hour * 60 + t.minute
                },
                settings = s,
            )
        }
    }

    /** Widget ve bildirim aksiyonları için tek seferlik güncel durum. */
    suspend fun todayNow(): Today = today().first()

    suspend fun addIntake(ml: Int, atMs: Long = System.currentTimeMillis()): Long =
        dao.insert(Intake(atMs = atMs, ml = ml))

    suspend fun undoLast() {
        dao.lastId()?.let { dao.delete(it) }
    }
}
