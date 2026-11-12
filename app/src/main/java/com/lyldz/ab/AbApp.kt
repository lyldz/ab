package com.lyldz.ab

import android.app.Application
import androidx.room.Room
import com.lyldz.ab.data.AbDb
import com.lyldz.ab.data.Intake
import com.lyldz.ab.data.MIGRATION_1_2
import com.lyldz.ab.data.SOURCE_APP
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.data.SettingsStore
import com.lyldz.ab.notify.Notifier
import com.lyldz.ab.notify.Reminders

class AbApp : Application() {
    val repo: AbRepository by lazy {
        val db = Room.databaseBuilder(this, AbDb::class.java, "ab.db").addMigrations(MIGRATION_1_2).build()
        AbRepository(db.intake(), SettingsStore(this))
    }
    val reminders: Reminders by lazy { Reminders(this, repo) }

    /** Uygulama, bildirim ve widget aynı yolu kullanır: değişiklikten sonra alarmı ve widget'ı tazele, bildirim duruyorsa güncelle. */
    suspend fun addWater(ml: Int, source: Int = SOURCE_APP) {
        repo.addIntake(ml, source)
        afterChange()
    }

    suspend fun undoWater() {
        repo.undoLast()
        afterChange()
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
        repo.updateIntake(e.id, ml, atMs)
        afterChange()
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
