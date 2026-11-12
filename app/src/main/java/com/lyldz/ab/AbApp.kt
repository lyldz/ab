package com.lyldz.ab

import android.app.Application
import androidx.room.Room
import com.lyldz.ab.data.AbDb
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.data.SettingsStore

class AbApp : Application() {
    val repo: AbRepository by lazy {
        val db = Room.databaseBuilder(this, AbDb::class.java, "ab.db").build()
        AbRepository(db.intake(), SettingsStore(this))
    }
}
