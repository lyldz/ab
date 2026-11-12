package com.lyldz.ab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyldz.ab.AbApp
import com.lyldz.ab.core.withMinuteOfDay
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.data.Intake
import com.lyldz.ab.data.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

class MainViewModel(private val app: AbApp) : ViewModel() {
    /** null: veri henüz okunmadı. */
    val today: StateFlow<AbRepository.Today?> =
        app.repo.today().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun finishOnboarding(s: Settings) {
        viewModelScope.launch {
            app.repo.saveSettings(s.copy(onboarded = true))
            app.reminders.reschedule()
        }
    }

    fun saveSettings(s: Settings) {
        viewModelScope.launch {
            app.repo.saveSettings(s)
            app.reminders.reschedule()
        }
    }

    fun add(ml: Int) {
        viewModelScope.launch { app.addWater(ml) }
    }

    fun delete(e: Intake) {
        viewModelScope.launch { app.deleteEntry(e) }
    }

    fun restore(e: Intake) {
        viewModelScope.launch { app.restoreEntry(e) }
    }

    fun edit(e: Intake, ml: Int, minute: Int) {
        viewModelScope.launch { app.editEntry(e, ml, withMinuteOfDay(e.atMs, minute, ZoneId.systemDefault())) }
    }

    fun undo() {
        viewModelScope.launch { app.undoWater() }
    }
}
