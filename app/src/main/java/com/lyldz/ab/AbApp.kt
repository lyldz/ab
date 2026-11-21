package com.lyldz.ab

import android.app.Application
import androidx.room.Room
import com.lyldz.ab.core.isValidIntakeMl
import com.lyldz.ab.data.AbDb
import com.lyldz.ab.data.BackupData
import com.lyldz.ab.data.Intake
import com.lyldz.ab.data.MIGRATION_1_2
import com.lyldz.ab.data.MIGRATION_2_3
import com.lyldz.ab.data.SOURCE_APP
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.data.SettingsStore
import com.lyldz.ab.notify.Notifier
import com.lyldz.ab.notify.Reminders
import java.time.LocalDate

class AbApp : Application() {
    val repo: AbRepository by lazy {
        val db = Room.databaseBuilder(this, AbDb::class.java, "ab.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        AbRepository(db.intake(), SettingsStore(this))
    }
    val reminders: Reminders by lazy { Reminders(this, repo) }

    /** Uygulama, bildirim ve widget aynı yolu kullanır: değişiklikten sonra alarmı ve widget'ı tazele, bildirim duruyorsa güncelle. */
    suspend fun addWater(ml: Int, source: Int = SOURCE_APP) {
        if (!isValidIntakeMl(ml)) return
        repo.addIntake(ml, source)
        afterChange()
    }

    /** Geçmiş (veya bugünkü) bir güne kayıt ekler; yalnızca bugünse bildirim/alarmı tazeler. */
    suspend fun addWaterForDay(day: LocalDate, ml: Int, minuteOfDay: Int = 12 * 60, source: Int = SOURCE_APP) {
        if (!isValidIntakeMl(ml)) return
        val id = repo.addIntakeForDay(day, ml, minuteOfDay, source)
        if (id > 0L && day == LocalDate.now()) {
            afterChange()
        }
    }

    suspend fun undoWater(): Intake? {
        val removed = repo.undoLast()
        if (removed != null) afterChange()
        return removed
    }

    suspend fun deleteEntry(e: Intake) {
        repo.deleteIntake(e.id)
        afterChange()
    }

    suspend fun restoreEntry(e: Intake) {
        repo.restoreIntake(e)
        afterChange()
    }

    suspend fun editEntry(e: Intake, ml: Int, atMs: Long) {
        if (!isValidIntakeMl(ml)) return
        repo.updateIntake(e.id, ml, atMs)
        afterChange()
    }

    suspend fun importBackup(d: BackupData): Int {
        val n = repo.importData(d)
        afterChange()
        return n
    }

    private suspend fun afterChange() {
        reminders.reschedule()
        reminders.refreshNotification()
    }

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this) // izinden önce oluşturulur
    }
}
