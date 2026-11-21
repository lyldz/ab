package com.lyldz.ab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyldz.ab.AbApp
import com.lyldz.ab.core.DayStat
import com.lyldz.ab.core.isValidIntakeMl
import com.lyldz.ab.core.msUntilNextMidnight
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
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class MainViewModel(private val app: AbApp) : ViewModel() {
    private val manualDate = MutableStateFlow(LocalDate.now())

    /**
     * Bugünün tarihi: bir sonraki gece yarısına kalan süreyi hesaplayarak en fazla 30 sn bekler,
     * böylece gece yarısı geçişinde beklemeden yeni güne döner. [refreshDate] ile de anında tetiklenebilir.
     */
    private val tickerDate = flow {
        while (true) {
            val now = ZonedDateTime.now()
            emit(now.toLocalDate())
            val waitMs = minOf(30_000L, msUntilNextMidnight(now) + 50L)
            delay(waitMs)
        }
    }

    private val date = merge(manualDate, tickerDate).distinctUntilChanged()

    /** Günü anında yeniden denetler (ör. ekran öne gelince veya gece yarısı geçişinde). */
    fun refreshDate() {
        manualDate.value = LocalDate.now()
    }

    /** null: veri henüz okunmadı. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val today: StateFlow<AbRepository.Today?> =
        date.flatMapLatest { d ->
            viewModelScope.launch { app.repo.ensureGoalRecorded(d) }
            app.repo.today(d)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val period = MutableStateFlow(7)
    val periodDays: StateFlow<Int> = period

    @OptIn(ExperimentalCoroutinesApi::class)
    val stats: StateFlow<List<DayStat>> =
        combine(period, date) { p, d -> p to d }.flatMapLatest { (p, d) -> app.repo.stats(p, d) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val streak: StateFlow<Int> =
        date.flatMapLatest { d -> app.repo.stats(60, d) }.map(::streakDays)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Geçmiş sekmesi: son 30 günün günlük toplamları. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<List<DayStat>> =
        date.flatMapLatest { d -> app.repo.stats(30, d) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        if (!isValidIntakeMl(ml)) return
        viewModelScope.launch { app.addWater(ml) }
    }

    fun addForDay(day: LocalDate, ml: Int, minuteOfDay: Int = 12 * 60) {
        if (!isValidIntakeMl(ml)) return
        viewModelScope.launch { app.addWaterForDay(day, ml, minuteOfDay) }
    }

    fun delete(e: Intake) {
        viewModelScope.launch { app.deleteEntry(e) }
    }

    fun restore(e: Intake) {
        viewModelScope.launch { app.restoreEntry(e) }
    }

    fun edit(e: Intake, ml: Int, minute: Int) {
        if (!isValidIntakeMl(ml)) return
        viewModelScope.launch { app.editEntry(e, ml, withMinuteOfDay(e.atMs, minute, ZoneId.systemDefault())) }
    }

    fun undo() {
        viewModelScope.launch { app.undoWater() }
    }
}
