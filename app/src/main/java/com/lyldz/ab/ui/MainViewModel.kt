package com.lyldz.ab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyldz.ab.AbApp
import com.lyldz.ab.core.DayStat
import com.lyldz.ab.core.streakDays
import com.lyldz.ab.core.withMinuteOfDay
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.data.Intake
import com.lyldz.ab.data.Settings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class MainViewModel(private val app: AbApp) : ViewModel() {
    /**
     * Bugünün tarihi (30 sn'de bir denetlenir). Depo akışları abone olunduğu andaki güne sabitlendiği için,
     * uygulama gece yarısını açık geçerse akışlar yeni güne yeniden kurulur; yoksa ekran dünü izlemeye devam eder.
     */
    private val date = flow {
        while (true) {
            emit(LocalDate.now())
            delay(30_000)
        }
    }.distinctUntilChanged()

    /** null: veri henüz okunmadı. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val today: StateFlow<AbRepository.Today?> =
        date.flatMapLatest { app.repo.today() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val period = MutableStateFlow(7)
    val periodDays: StateFlow<Int> = period

    @OptIn(ExperimentalCoroutinesApi::class)
    val stats: StateFlow<List<DayStat>> =
        combine(period, date) { p, _ -> p }.flatMapLatest { app.repo.stats(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val streak: StateFlow<Int> =
        date.flatMapLatest { app.repo.stats(60) }.map(::streakDays).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Geçmiş sekmesi: son 30 günün günlük toplamları. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<List<DayStat>> =
        date.flatMapLatest { app.repo.stats(30) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setHintCount(n: Int) {
        viewModelScope.launch { app.repo.setHintCount(n) }
    }

    fun setPeriod(days: Int) {
        period.value = days
    }

    fun finishOnboarding(s: Settings) {
        viewModelScope.launch {
            app.repo.saveSettings(s.copy(onboarded = true))
            app.reminders.refreshWeather()
            app.reminders.reschedule()
            app.reminders.refreshNotification(force = true)
        }
    }

    fun saveSettings(s: Settings) {
        viewModelScope.launch {
            app.repo.saveSettings(s)
            app.reminders.refreshWeather()
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
