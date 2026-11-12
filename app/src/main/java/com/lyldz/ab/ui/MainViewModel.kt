package com.lyldz.ab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.notify.Reminders
import com.lyldz.ab.data.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repo: AbRepository, private val reminders: Reminders) : ViewModel() {
    /** null: veri henüz okunmadı. */
    val today: StateFlow<AbRepository.Today?> =
        repo.today().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun finishOnboarding(s: Settings) {
        viewModelScope.launch { repo.saveSettings(s.copy(onboarded = true)); reminders.reschedule() }
    }

    fun add(ml: Int) {
        viewModelScope.launch { repo.addIntake(ml); reminders.reschedule() }
    }

    fun undo() {
        viewModelScope.launch { repo.undoLast(); reminders.reschedule() }
    }
}
