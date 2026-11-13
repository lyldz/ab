package com.lyldz.ab.data

import com.lyldz.ab.core.DayStat
import com.lyldz.ab.core.buildDayStats
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
        /** Planın çıpası: son içiş veya bugün gösterilen son hatırlatma, hangisi geçse. */
        val anchorMin: Int?,
        val settings: Settings,
        /** Bugünün kayıtları, en yeniden eskiye. */
        val entries: List<Intake>,
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
                anchorMin = listOfNotNull(
                    list.firstOrNull()?.let { minuteOfDay(it.atMs) },
                    s.lastNotifiedMs?.takeIf { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == day }
                        ?.let { minuteOfDay(it) },
                ).maxOrNull(),
                settings = s,
                entries = list,
            )
        }
    }

    /** Widget ve bildirim aksiyonları için tek seferlik güncel durum. */
    suspend fun todayNow(): Today = today().first()

    suspend fun addIntake(ml: Int, source: Int = SOURCE_APP, atMs: Long = System.currentTimeMillis()): Long =
        dao.insert(Intake(atMs = atMs, ml = ml, source = source))

    suspend fun deleteIntake(id: Long) = dao.delete(id)

    /** Silinen kaydı aynı kimlikle geri koyar (geri al). */
    suspend fun restoreIntake(e: Intake) = dao.insert(e)

    suspend fun updateIntake(id: Long, ml: Int, atMs: Long) = dao.update(id, ml, atMs)

    suspend fun recordGoal(day: LocalDate, goalMl: Int) = dao.upsertGoal(DayGoal(day.toString(), goalMl))

    /** Son [days] günün günlük toplamları, eskiden yeniye. Gün sınırı abone olunduğu andaki güne sabittir. */
    fun stats(days: Int): Flow<List<DayStat>> {
        val end = LocalDate.now(zone)
        val start = end.minusDays(days - 1L)
        val from = dayBoundsMs(start, zone).first
        val to = dayBoundsMs(end, zone).second
        return combine(store.flow, dao.observeRange(from, to), dao.observeGoals(start.toString())) { s, list, goals ->
            buildDayStats(list.map { it.atMs to it.ml }, goals.associate { it.day to it.goalMl }, start, days, zone) { s.goalMl(it) }
        }
    }

    suspend fun exportData(): BackupData = BackupData(dao.all(), dao.allGoals(), store.flow.first())

    /**
     * Yedeği ekler: aynı an ve miktardaki kayıt yeniden eklenmez (iki kez geri yüklemek çoğaltmaz), günlük hedef
     * anlık görüntüleri yedektekiyle güncellenir, ayarlar yedektekilerle değişir. Eklenen kayıt sayısını döndürür.
     */
    suspend fun importData(d: BackupData): Int {
        val seen = dao.all().mapTo(HashSet()) { it.atMs to it.ml }
        var added = 0
        for (i in d.intakes) {
            if (seen.add(i.atMs to i.ml)) {
                dao.insert(i.copy(id = 0))
                added++
            }
        }
        d.goals.forEach { dao.upsertGoal(it) }
        val cur = store.flow.first()
        val samePlace = d.settings.lat == cur.lat && d.settings.lon == cur.lon
        store.save(
            d.settings.copy(
                onboarded = true,
                tempC = cur.tempC.takeIf { samePlace }, tempDay = cur.tempDay.takeIf { samePlace },
                lastNotifiedMs = cur.lastNotifiedMs,
            ),
        )
        return added
    }

    suspend fun markNotified(ms: Long) = store.setLastNotified(ms)

    suspend fun setHintCount(n: Int) = store.setHintCount(n)

    suspend fun settingsNow(): Settings = store.flow.first()

    suspend fun saveTemperature(tempC: Double, day: String) = store.setTemperature(tempC, day)

    private fun minuteOfDay(ms: Long): Int =
        Instant.ofEpochMilli(ms).atZone(zone).let { it.hour * 60 + it.minute }

    suspend fun undoLast() {
        dao.lastId()?.let { dao.delete(it) }
    }
}
