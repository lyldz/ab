package com.lyldz.ab.data

import com.lyldz.ab.core.DayStat
import com.lyldz.ab.core.buildDayStats
import com.lyldz.ab.core.dayBoundsMs
import com.lyldz.ab.core.isValidIntakeMl
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

    suspend fun saveSettings(s: Settings) {
        store.save(s)
        if (s.onboarded) {
            val today = LocalDate.now(zone)
            recordGoal(today, s.goalMl(today))
        }
    }

    /** Gün sınırı verilen güne (varsayılan: abone olunduğu an) sabittir; gün dönünce arayüz yeniden abone olmalı. */
    fun today(day: LocalDate = LocalDate.now(zone)): Flow<Today> {
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

    /** Geçersiz (1..5000 ml dışı) miktarlar reddedilir (-1L döner); geçerli kayıtta günün hedef anlık görüntüsü de güncellenir. */
    suspend fun addIntake(ml: Int, source: Int = SOURCE_APP, atMs: Long = System.currentTimeMillis()): Long {
        if (!isValidIntakeMl(ml)) return -1L
        val id = dao.insert(Intake(atMs = atMs, ml = ml, source = source))
        val entryDay = Instant.ofEpochMilli(atMs).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        val goal = store.flow.first().goalMl(entryDay)
        if (entryDay == today) {
            recordGoal(entryDay, goal)
        } else if (goal > 0) {
            dao.insertGoalIfAbsent(DayGoal(entryDay.toString(), goal))
        }
        return id
    }

    /** Geçmiş (veya bugünkü) bir güne su kaydı ekler; gelecek günler ve geçersiz miktarlar reddedilir (-1L). */
    suspend fun addIntakeForDay(
        day: LocalDate,
        ml: Int,
        minuteOfDay: Int = 12 * 60,
        source: Int = SOURCE_APP,
        nowMs: Long = System.currentTimeMillis(),
    ): Long {
        if (!isValidIntakeMl(ml)) return -1L
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        if (day.isAfter(today)) return -1L
        val rawMs = day.atStartOfDay(zone).plusMinutes(minuteOfDay.coerceIn(0, 1439).toLong()).toInstant().toEpochMilli()
        val atMs = if (day == today) minOf(rawMs, nowMs) else rawMs
        return addIntake(ml, source, atMs)
    }

    suspend fun deleteIntake(id: Long) = dao.delete(id)

    /** Silinen kaydı aynı kimlikle geri koyar (geri al). */
    suspend fun restoreIntake(e: Intake) = dao.insert(e)

    suspend fun updateIntake(id: Long, ml: Int, atMs: Long) {
        if (!isValidIntakeMl(ml)) return
        dao.update(id, ml, atMs)
    }

    suspend fun recordGoal(day: LocalDate, goalMl: Int) {
        if (goalMl <= 0) return
        dao.upsertGoal(DayGoal(day.toString(), goalMl))
    }

    /** Gece yarısı geçişi ve açılışta günün hedef anlık görüntüsünün geçmiş/istatistik için kaydedilmesini sağlar. */
    suspend fun ensureGoalRecorded(day: LocalDate = LocalDate.now(zone)) {
        val s = store.flow.first()
        if (s.onboarded) recordGoal(day, s.goalMl(day))
    }

    /** Son [days] günün günlük toplamları, eskiden yeniye. Gün sınırı [end]'e (varsayılan bugün) sabittir. */
    fun stats(days: Int, end: LocalDate = LocalDate.now(zone)): Flow<List<DayStat>> {
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
            if (isValidIntakeMl(i.ml) && seen.add(i.atMs to i.ml)) {
                dao.insert(i.copy(id = 0))
                added++
            }
        }
        d.goals.filter { it.goalMl > 0 }.forEach { dao.upsertGoal(it) }
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

    suspend fun saveTemperature(tempC: Double, day: String) {
        if (!tempC.isFinite()) return
        store.setTemperature(tempC, day)
        val parsedDay = runCatching { LocalDate.parse(day) }.getOrNull() ?: return
        val s = store.flow.first()
        if (s.onboarded) recordGoal(parsedDay, s.goalMl(parsedDay))
    }

    private fun minuteOfDay(ms: Long): Int =
        Instant.ofEpochMilli(ms).atZone(zone).let { it.hour * 60 + it.minute }

    /**
     * Yalnızca [day] (varsayılan bugün) sınırları içindeki en son kaydı geri alır.
     * Gece yarısından sonra dünün kaydını silmez; geri alınan kaydı (yoksa null) döndürür.
     */
    suspend fun undoLast(day: LocalDate = LocalDate.now(zone)): Intake? {
        val (from, to) = dayBoundsMs(day, zone)
        val last = dao.lastInRange(from, to) ?: return null
        dao.delete(last.id)
        return last
    }
}
