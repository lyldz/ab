package com.lyldz.ab.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lyldz.ab.core.Activity
import com.lyldz.ab.core.dailyGoalMl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class Settings(
    val weightKg: Int = 70,
    val activity: Activity = Activity.NONE,
    val cupMl: Int = 200,
    val windowStartMin: Int = 6 * 60,
    val windowEndMin: Int = 18 * 60,
    val useActivity: Boolean = true,
    val useWeather: Boolean = true,
    val goalOverrideMl: Int? = null,
    val city: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val tempC: Double? = null,
    val tempDay: String? = null,
    val onboarded: Boolean = false,
    val lastNotifiedMs: Long? = null,
) {
    /** Sıcaklık yalnızca bugün alındıysa geçerli; eski gün değeri hedefi etkilemez. */
    fun goalMl(today: LocalDate): Int = goalOverrideMl ?: dailyGoalMl(
        weightKg, activity,
        tempC.takeIf { tempDay == today.toString() },
        useActivity, useWeather,
    )
}

private val Context.store by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    val flow: Flow<Settings> = context.store.data.map { it.toSettings() }

    suspend fun save(s: Settings) {
        context.store.edit { p ->
            p[WEIGHT] = s.weightKg
            p[ACTIVITY] = s.activity.name
            p[CUP] = s.cupMl
            p[START] = s.windowStartMin
            p[END] = s.windowEndMin
            p[USE_ACTIVITY] = s.useActivity
            p[USE_WEATHER] = s.useWeather
            p[ONBOARDED] = s.onboarded
            p.putOrRemove(GOAL, s.goalOverrideMl)
            p.putOrRemove(CITY, s.city)
            p.putOrRemove(LAT, s.lat)
            p.putOrRemove(LON, s.lon)
            p.putOrRemove(TEMP, s.tempC)
            p.putOrRemove(TEMP_DAY, s.tempDay)
        }
    }

    /** Yalnızca bu alanı yazar; ayar kaydıyla yarışmasın. */
    suspend fun setLastNotified(ms: Long) {
        context.store.edit { it[LAST_NOTIFIED] = ms }
    }

    private fun <T> androidx.datastore.preferences.core.MutablePreferences.putOrRemove(
        key: Preferences.Key<T>,
        v: T?,
    ) {
        if (v == null) remove(key) else this[key] = v
    }

    private fun Preferences.toSettings(): Settings {
        val d = Settings()
        return Settings(
            weightKg = this[WEIGHT] ?: d.weightKg,
            activity = this[ACTIVITY]?.let { runCatching { Activity.valueOf(it) }.getOrNull() } ?: d.activity,
            cupMl = this[CUP] ?: d.cupMl,
            windowStartMin = this[START] ?: d.windowStartMin,
            windowEndMin = this[END] ?: d.windowEndMin,
            useActivity = this[USE_ACTIVITY] ?: d.useActivity,
            useWeather = this[USE_WEATHER] ?: d.useWeather,
            goalOverrideMl = this[GOAL],
            city = this[CITY],
            lat = this[LAT],
            lon = this[LON],
            tempC = this[TEMP],
            tempDay = this[TEMP_DAY],
            onboarded = this[ONBOARDED] ?: d.onboarded,
            lastNotifiedMs = this[LAST_NOTIFIED],
        )
    }

    private companion object {
        val WEIGHT = intPreferencesKey("weight_kg")
        val ACTIVITY = stringPreferencesKey("activity")
        val CUP = intPreferencesKey("cup_ml")
        val START = intPreferencesKey("window_start_min")
        val END = intPreferencesKey("window_end_min")
        val USE_ACTIVITY = booleanPreferencesKey("use_activity")
        val USE_WEATHER = booleanPreferencesKey("use_weather")
        val GOAL = intPreferencesKey("goal_override_ml")
        val CITY = stringPreferencesKey("city")
        val LAT = doublePreferencesKey("lat")
        val LON = doublePreferencesKey("lon")
        val TEMP = doublePreferencesKey("temp_c")
        val TEMP_DAY = stringPreferencesKey("temp_day")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val LAST_NOTIFIED = longPreferencesKey("last_notified_ms")
    }
}
