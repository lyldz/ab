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
import com.lyldz.ab.core.MAX_WEIGHT_KG
import com.lyldz.ab.core.MIN_WEIGHT_KG
import com.lyldz.ab.core.baseIntervalMin
import com.lyldz.ab.core.dailyGoalMl
import com.lyldz.ab.core.isValidIntakeMl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val DEFAULT_CUP_PRESETS = listOf(150, 200, 250, 330, 500, 750)

fun parseCupPresets(raw: String?): List<Int> {
    if (raw.isNullOrBlank()) return DEFAULT_CUP_PRESETS
    val parsed = raw.split(',').mapNotNull { it.trim().toIntOrNull()?.takeIf { ml -> ml in 50..2000 } }
        .distinct().sorted().take(8)
    return parsed.ifEmpty { DEFAULT_CUP_PRESETS }
}

fun encodeCupPresets(presets: List<Int>): String =
    presets.filter { it in 50..2000 }.distinct().sorted().take(8).ifEmpty { DEFAULT_CUP_PRESETS }.joinToString(",")

data class Settings(
    val weightKg: Int = 70,
    val activity: Activity = Activity.NONE,
    val cupMl: Int = 200,
    /** Kalkış saati: hatırlatmalar bu dakikadan başlar. */
    val windowStartMin: Int = 6 * 60 + 30,
    /** Su içmenin sonu = yatış − 2 saat (bkz. STOP_BEFORE_BED_MIN); yatış arayüzde gösterilir. */
    val windowEndMin: Int = 20 * 60,
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
    val name: String = "",
    val themeMode: Int = 0, // 0 sistem, 1 açık, 2 koyu
    /** Gösterge suyu telefonun eğimine/hareketine göre oynar (ivmeölçer, izin gerekmez). */
    val motionWater: Boolean = true,
    /** Uygulama içi hatırlatma aç/kapat anahtarı. */
    val remindersEnabled: Boolean = true,
    /** Uygulama dili: "" sistem, "tr" Türkçe, "en" İngilizce. */
    val language: String = "",
    /** Gösterim birimi: 0 = ml, 1 = fl oz (tüm kayıtlar dahili olarak ml saklanır). */
    val unit: Int = 0,
    /** Kullanıcının özel bardak/şişe ön ayarları (ml). */
    val cupPresets: List<Int> = DEFAULT_CUP_PRESETS,
    /** "Uzun bas" ipucunun kaç kez gösterildiği (3'te durur); ayar kaydı yazmaz, yalnızca [SettingsStore.setHintCount]. */
    val longPressHints: Int = 0,
) {
    /** Sıcaklık yalnızca bugün alındıysa ve sonlu bir sayıysa geçerli; eski gün değeri hedefi etkilemez. */
    fun tempToday(today: LocalDate): Double? = tempC?.takeIf { it.isFinite() && tempDay == today.toString() }

    /** Geçerli (pozitif) elle hedef değeri; 0 veya negatif değerler yok sayılır. */
    fun validGoalOverrideMl(): Int? = goalOverrideMl?.takeIf { it > 0 }

    /** Hatırlatma aralığı için hava eki olmayan hedef: yalnızca kilo, hareket ya da elle hedef değişince değişir. */
    fun planGoalMl(): Int = validGoalOverrideMl() ?: dailyGoalMl(weightKg, activity, null, useActivity, false)

    /** Günün sabit hatırlatma aralığı (dk). */
    fun reminderIntervalMin(): Int = baseIntervalMin(windowStartMin, windowEndMin, cupMl, planGoalMl())

    fun goalMl(today: LocalDate): Int = validGoalOverrideMl() ?: dailyGoalMl(
        weightKg, activity, tempToday(today), useActivity, useWeather,
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
            p[NAME] = s.name
            p[THEME] = s.themeMode
            p[MOTION] = s.motionWater
            p[REMINDERS] = s.remindersEnabled
            p[LANGUAGE] = s.language
            p[UNIT] = s.unit
            p[CUP_PRESETS] = encodeCupPresets(s.cupPresets)
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

    suspend fun setHintCount(n: Int) {
        context.store.edit { it[HINTS] = n }
    }

    suspend fun setTemperature(tempC: Double, day: String) {
        if (!tempC.isFinite()) return
        context.store.edit {
            it[TEMP] = tempC
            it[TEMP_DAY] = day
        }
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
            weightKg = this[WEIGHT]?.takeIf { it in MIN_WEIGHT_KG..MAX_WEIGHT_KG } ?: d.weightKg,
            activity = this[ACTIVITY]?.let { runCatching { Activity.valueOf(it) }.getOrNull() } ?: d.activity,
            cupMl = this[CUP]?.takeIf(::isValidIntakeMl) ?: d.cupMl,
            windowStartMin = this[START] ?: d.windowStartMin,
            windowEndMin = this[END] ?: d.windowEndMin,
            useActivity = this[USE_ACTIVITY] ?: d.useActivity,
            useWeather = this[USE_WEATHER] ?: d.useWeather,
            goalOverrideMl = this[GOAL]?.takeIf { it > 0 },
            city = this[CITY],
            lat = this[LAT],
            lon = this[LON],
            tempC = this[TEMP]?.takeIf { it.isFinite() },
            tempDay = this[TEMP_DAY],
            onboarded = this[ONBOARDED] ?: d.onboarded,
            lastNotifiedMs = this[LAST_NOTIFIED],
            name = this[NAME] ?: d.name,
            themeMode = this[THEME]?.takeIf { it in 0..2 } ?: d.themeMode,
            motionWater = this[MOTION] ?: d.motionWater,
            remindersEnabled = this[REMINDERS] ?: d.remindersEnabled,
            language = this[LANGUAGE]?.takeIf { it == "" || it == "tr" || it == "en" } ?: d.language,
            unit = this[UNIT]?.takeIf { it in 0..1 } ?: d.unit,
            cupPresets = parseCupPresets(this[CUP_PRESETS]),
            longPressHints = this[HINTS] ?: d.longPressHints,
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
        val NAME = stringPreferencesKey("name")
        val THEME = intPreferencesKey("theme_mode")
        val MOTION = booleanPreferencesKey("motion_water")
        val REMINDERS = booleanPreferencesKey("reminders_enabled")
        val LANGUAGE = stringPreferencesKey("language")
        val UNIT = intPreferencesKey("unit")
        val CUP_PRESETS = stringPreferencesKey("cup_presets")
        val HINTS = intPreferencesKey("long_press_hints")
        val LAST_NOTIFIED = longPreferencesKey("last_notified_ms")
    }
}
