package com.lyldz.ab.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lyldz.ab.core.dayBoundsMs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/**
 * AbRepository ekleme, geri alma (bugüne kısıtlı undo), düzenleme/silme,
 * hedef anlık görüntüsü (history/stats) ve yedekleme entegrasyon testleri.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AbRepositoryTest {
    private val zone = ZoneId.of("UTC")
    private lateinit var db: AbDb
    private lateinit var repo: AbRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AbDb::class.java).allowMainThreadQueries().build()
        repo = AbRepository(db.intake(), SettingsStore(ctx), zone)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addIntakeAccumulatesAndOrdersNewestFirst() = runBlocking {
        repo.saveSettings(Settings(weightKg = 70, onboarded = true))
        val (start, _) = dayBoundsMs(LocalDate.now(zone), zone)

        repo.addIntake(200, SOURCE_APP, atMs = start + 8 * 3600_000L)
        repo.addIntake(330, SOURCE_WIDGET, atMs = start + 10 * 3600_000L)
        repo.addIntake(500, SOURCE_NOTIFICATION, atMs = start + 12 * 3600_000L)

        val today = repo.todayNow()
        assertEquals(1030, today.drankMl)
        assertEquals(listOf(500, 330, 200), today.entries.map { it.ml })
        assertEquals(12 * 60, today.anchorMin)
    }

    @Test
    fun undoLastRemovesOnlyTodaysEntryAndNeverDeletesYesterday() = runBlocking {
        repo.saveSettings(Settings(weightKg = 70, onboarded = true))
        val today = LocalDate.now(zone)
        val (todayStart, _) = dayBoundsMs(today, zone)
        val yesterdayMs = todayStart - 3600_000L // dün 23:00 UTC

        // Dün eklenen bir kayıt
        repo.addIntake(250, SOURCE_APP, atMs = yesterdayMs)
        assertEquals(0, repo.todayNow().drankMl)

        // Bugün henüz kayıt yokken undo yapılırsa dünün kaydı korunmalı ve null dönmeli
        val none = repo.undoLast()
        assertEquals(null, none)
        assertEquals(1, db.intake().all().size)
        assertEquals(250, db.intake().all().first().ml)

        // Bugün iki kayıt ekle, sonra undo yap: yalnızca bugünün son kaydı silinmeli
        repo.addIntake(200, SOURCE_APP, atMs = todayStart + 9 * 3600_000L)
        repo.addIntake(330, SOURCE_APP, atMs = todayStart + 10 * 3600_000L)
        assertEquals(530, repo.todayNow().drankMl)

        val undone1 = repo.undoLast()
        assertEquals(330, undone1?.ml)
        assertEquals(200, repo.todayNow().drankMl)
        assertEquals(2, db.intake().all().size)

        val undone2 = repo.undoLast()
        assertEquals(200, undone2?.ml)
        assertEquals(0, repo.todayNow().drankMl)
        // Dünün kaydı hâlâ sağlam
        assertEquals(1, db.intake().all().size)
        assertEquals(yesterdayMs, db.intake().all().first().atMs)
    }

    @Test
    fun addIntakeRejectsOutOfRangeAmounts() = runBlocking {
        repo.saveSettings(Settings(weightKg = 70, onboarded = true))
        val (todayStart, _) = dayBoundsMs(LocalDate.now(zone), zone)
        assertEquals(-1L, repo.addIntake(0, SOURCE_APP, atMs = todayStart + 3600_000L))
        assertEquals(-1L, repo.addIntake(-50, SOURCE_APP, atMs = todayStart + 3600_000L))
        assertEquals(-1L, repo.addIntake(5001, SOURCE_APP, atMs = todayStart + 3600_000L))
        assertTrue(repo.todayNow().entries.isEmpty())
    }

    @Test
    fun deleteRestoreAndUpdateIntakeWork() = runBlocking {
        repo.saveSettings(Settings(weightKg = 70, onboarded = true))
        val (todayStart, _) = dayBoundsMs(LocalDate.now(zone), zone)
        val id = repo.addIntake(200, SOURCE_APP, atMs = todayStart + 9 * 3600_000L)

        repo.updateIntake(id, 350, todayStart + 11 * 3600_000L)
        val updated = repo.todayNow().entries.single()
        assertEquals(350, updated.ml)
        assertEquals(todayStart + 11 * 3600_000L, updated.atMs)

        repo.deleteIntake(id)
        assertTrue(repo.todayNow().entries.isEmpty())

        repo.restoreIntake(updated)
        assertEquals(350, repo.todayNow().drankMl)
    }

    @Test
    fun statsUsesHistoricalGoalSnapshotAndGroupsByDay() = runBlocking {
        repo.saveSettings(Settings(weightKg = 70, goalOverrideMl = 2000, onboarded = true))
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)
        val (yStart, _) = dayBoundsMs(yesterday, zone)
        val (tStart, _) = dayBoundsMs(today, zone)

        // Dünün hedefi 2750 ml olarak kaydedilmiş olsun, bugün ayar 2000 ml
        repo.recordGoal(yesterday, 2750)
        repo.recordGoal(today, 2000)
        repo.addIntake(2800, SOURCE_APP, atMs = yStart + 12 * 3600_000L)
        repo.addIntake(1000, SOURCE_APP, atMs = tStart + 9 * 3600_000L)

        val stats = repo.stats(2).first()
        assertEquals(2, stats.size)
        assertEquals(yesterday, stats[0].day)
        assertEquals(2800, stats[0].ml)
        assertEquals(2750, stats[0].goalMl)
        assertTrue(stats[0].reached)

        assertEquals(today, stats[1].day)
        assertEquals(1000, stats[1].ml)
        assertEquals(2000, stats[1].goalMl)
    }

    @Test
    fun exportAndImportBackupIsIdempotent() = runBlocking {
        repo.saveSettings(Settings(weightKg = 80, cupMl = 250, onboarded = true))
        val (tStart, _) = dayBoundsMs(LocalDate.now(zone), zone)
        repo.addIntake(250, SOURCE_APP, atMs = tStart + 8 * 3600_000L)
        repo.recordGoal(LocalDate.now(zone), 2500)

        val backup = repo.exportData()
        // Aynı yedeği iki kez içeri aktarmak kayıtları çoğaltmamalı
        assertEquals(0, repo.importData(backup))
        assertEquals(1, db.intake().all().size)
    }

    @Test
    fun addIntakeForDayRecordsPastDayAndRejectsFuture() = runBlocking {
        repo.saveSettings(Settings(weightKg = 80, onboarded = true))
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)
        val nowMs = dayBoundsMs(today, zone).first + 14 * 3600_000L

        assertEquals(-1L, repo.addIntakeForDay(tomorrow, 300, 10 * 60, nowMs = nowMs))
        val id = repo.addIntakeForDay(yesterday, 400, 15 * 60, nowMs = nowMs)
        assertTrue(id > 0L)

        val stats = repo.stats(2, today).first()
        assertEquals(400, stats[0].ml)
        assertEquals(0, stats[1].ml)
    }
}
