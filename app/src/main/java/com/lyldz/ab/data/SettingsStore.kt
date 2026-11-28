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
import com.lyldz.ab.core.GOAL_WARN_ML
import com.lyldz.ab.core.MAX_CUP_ML
import com.lyldz.ab.core.MAX_MANUAL_GOAL_ML
import com.lyldz.ab.core.MAX_WEIGHT_KG
import com.lyldz.ab.core.MIN_CUP_ML
import com.lyldz.ab.core.MIN_GOAL_ML
import com.lyldz.ab.core.MIN_MANUAL_GOAL_ML
import com.lyldz.ab.core.MAX_REMINDER_ML
import com.lyldz.ab.core.MIN_REMINDER_ML
import com.lyldz.ab.core.MIN_WEIGHT_KG
import com.lyldz.ab.core.ML_PER_KG
import com.lyldz.ab.core.PACE_TOLERANCE_ML
import com.lyldz.ab.core.STOP_BEFORE_BED_MIN
import com.lyldz.ab.core.baseIntervalMin
import com.lyldz.ab.core.dailyGoalMl
import com.lyldz.ab.core.isValidCupMl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val DEFAULT_CUP_PRESETS = listOf(150, 200, 250, 330, 500, 750)
val DEFAULT_WIDGET_AMOUNTS = listOf(500, 1000)

fun parseCupPresets(raw: String?): List<Int> {
    if (raw.isNullOrBlank()) return DEFAULT_CUP_PRESETS
    val parsed = raw.split(',').mapNotNull { it.trim().toIntOrNull()?.takeIf(::isValidCupMl) }
        .distinct().sorted().take(8)
    return parsed.ifEmpty { DEFAULT_CUP_PRESETS }
}

fun encodeCupPresets(presets: List<Int>): String =
    presets.filter(::isValidCupMl).distinct().sorted().take(8).ifEmpty { DEFAULT_CUP_PRESETS }.joinToString(",")

fun parseWidgetAmounts(raw: String?): List<Int> {
    if (raw.isNullOrBlank()) return DEFAULT_WIDGET_AMOUNTS
    val parsed = raw.split(',').mapNotNull { it.trim().toIntOrNull()?.takeIf(::isValidCupMl) }
        .distinct().sorted().take(3)
    return parsed.ifEmpty { DEFAULT_WIDGET_AMOUNTS }
}

fun encodeWidgetAmounts(amounts: List<Int>): String =
    amounts.filter(::isValidCupMl).distinct().sorted().take(3).ifEmpty { DEFAULT_WIDGET_AMOUNTS }.joinToString(",")

data class Settings(
    val weightKg: Int = 70,
    val activity: Activity = Activity.NONE,
    val cupMl: Int = 200,
    /** Kalkış saati: hatırlatmalar bu dakikadan başlar. */
    val windowStartMin: Int = 6 * 60 + 30,
    /** Su içmenin sonu = yatış − stopBeforeBedMin; yatış arayüzde gösterilir. */
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
    /** Su eklendiğinde dolum dalgalanma efekti. */
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
    /** Sessiz/kalıcı durum bildirimi gösterilsin mi. */
    val statusNotificationEnabled: Boolean = true,
    /** Sabah sıcaklık hedefi yükselttiğinde ayrı bildirim gönderilsin mi. */
    val weatherNotificationEnabled: Boolean = true,
    /** İzin verilmişse tam zamanında (exact) alarm kullanılsın mı. */
    val preferExactAlarm: Boolean = true,
    /** Özel sabit hatırlatma aralığı (dk); null ise pencere ve hedefe göre otomatik hesaplanır. */
    val customReminderIntervalMin: Int? = null,
    /** Yatış saatinden kaç dakika önce su içme penceresi bitsin (varsayılan 120 dk). */
    val stopBeforeBedMin: Int = STOP_BEFORE_BED_MIN,
    /** Bildirimdeki 2. hızlı ekleme düğmesinin miktarı (ml). */
    val notifSecondActionMl: Int = 500,
    /** Widget'ta bardak boyutunun yanında gösterilen hızlı ekleme miktarları (ml). */
    val widgetQuickAmounts: List<Int> = DEFAULT_WIDGET_AMOUNTS,
    /** Hızlı Ayarlar (Quick Settings Tile) dokunuşunda Toast mesajı gösterilsin mi. */
    val tileToastEnabled: Boolean = true,
    /** Otomatik hedef formülü: kilo başına ml katsayısı (15..50). */
    val mlPerKg: Int = ML_PER_KG,
    /** Otomatik hedef alt tabanı (500..3000 ml). */
    val minGoalMl: Int = MIN_GOAL_ML,
    /** Yüksek hedef uyarı eşiği (2000..8000 ml). */
    val goalWarnMl: Int = GOAL_WARN_ML,
    /** Tempo karşılaştırmasında "tam tempodasın" toleransı (0..500 ml). */
    val paceToleranceMl: Int = PACE_TOLERANCE_ML,
    /** Su küresindeki sürekli dalga animasyonu açık mı. */
    val waterWaveAnimation: Boolean = true,
    /** Hedef aşıldığında taşma halkası ve damlacık efekti gösterilsin mi. */
    val showOverflowEffect: Boolean = true,
    /** Ana ekrandaki uzun-basma keşif ipuçları gösterilsin mi. */
    val showHints: Boolean = true,
    /** Geri-al (Undo) çubuğunun ekranda kalma süresi (1000..10000 ms). */
    val undoBannerMs: Int = 1800,
    /** İstatistik ekranının varsayılan dönemi (7 veya 30 gün). */
    val defaultStatsDays: Int = 7,
    /** Geçmiş sekmesinde gösterilen gün sayısı (7..365). */
    val historyDays: Int = 30,
    /** Seri (streak) hesaplama penceresi (30..365 gün). */
    val streakWindowDays: Int = 60,
    /** Hatırlatmada önerilen en az miktar (50..500 ml). */
    val minReminderMl: Int = MIN_REMINDER_ML,
    /** Hatırlatmada önerilen en çok miktar (100..2000 ml). */
    val maxReminderMl: Int = MAX_REMINDER_ML,
    /** Ana ekrandaki uzun-basma ipucunun kaç açılışta gösterileceği (1..10). */
    val maxHints: Int = 3,
    /** Hava durumu ve şehir sorgusunda bağlantı/okuma zaman aşımı (3..30 sn). */
    val networkTimeoutSec: Int = 10,
    /** Hatırlatma sırasında arka planda hava çekme üst süresi (3..30 sn). */
    val backgroundWeatherTimeoutSec: Int = 6,
) {
    /** Sıcaklık yalnızca bugün alındıysa ve sonlu bir sayıysa geçerli; eski gün değeri hedefi etkilemez. */
    fun tempToday(today: LocalDate): Double? = tempC?.takeIf { it.isFinite() && tempDay == today.toString() }

    /** Geçerli (pozitif) elle hedef değeri; aralık dışı değerler yok sayılır. */
    fun validGoalOverrideMl(): Int? = goalOverrideMl?.takeIf { it in MIN_MANUAL_GOAL_ML..MAX_MANUAL_GOAL_ML }

    /** Hatırlatma aralığı için hava eki olmayan hedef: yalnızca kilo, hareket ya da elle hedef değişince değişir. */
    fun planGoalMl(): Int = validGoalOverrideMl() ?: dailyGoalMl(
        weightKg, activity, null, useActivity, false, mlPerKg, minGoalMl,
    )

    /** Günün sabit hatırlatma aralığı (dk): özel aralık tanımlıysa onu, yoksa otomatik hesabı kullanır. */
    fun reminderIntervalMin(): Int =
        customReminderIntervalMin?.takeIf { it in 15..240 }
            ?: baseIntervalMin(windowStartMin, windowEndMin, cupMl, planGoalMl())

    fun goalMl(today: LocalDate): Int = validGoalOverrideMl() ?: dailyGoalMl(
        weightKg, activity, tempToday(today), useActivity, useWeather, mlPerKg, minGoalMl,
    )

    /** Tüm alanları geçerli sınırlara sabitleyen güvenli kopya. */
    fun sanitized(): Settings {
        val def = Settings()
        val start = windowStartMin.coerceIn(0, 23 * 60)
        val end = if (windowEndMin in (start + 60)..(23 * 60 + 30)) windowEndMin else maxOf(start + 60, def.windowEndMin).coerceAtMost(23 * 60 + 30)
        return copy(
            weightKg = weightKg.takeIf { it in MIN_WEIGHT_KG..MAX_WEIGHT_KG } ?: def.weightKg,
            cupMl = cupMl.takeIf(::isValidCupMl) ?: def.cupMl,
            windowStartMin = start,
            windowEndMin = end,
            goalOverrideMl = goalOverrideMl?.takeIf { it in MIN_MANUAL_GOAL_ML..MAX_MANUAL_GOAL_ML },
            city = city?.takeIf { it.isNotBlank() }?.take(100),
            lat = lat?.takeIf { it.isFinite() && it in -90.0..90.0 },
            lon = lon?.takeIf { it.isFinite() && it in -180.0..180.0 },
            tempC = tempC?.takeIf { it.isFinite() },
            name = name.take(30),
            themeMode = themeMode.takeIf { it in 0..2 } ?: def.themeMode,
            language = language.takeIf { it == "" || it == "tr" || it == "en" } ?: def.language,
            unit = unit.takeIf { it in 0..1 } ?: def.unit,
            cupPresets = cupPresets.filter(::isValidCupMl).distinct().sorted().take(8).ifEmpty { DEFAULT_CUP_PRESETS },
            longPressHints = longPressHints.coerceIn(0, 10),
            customReminderIntervalMin = customReminderIntervalMin?.takeIf { it in 15..240 },
            stopBeforeBedMin = stopBeforeBedMin.coerceIn(0, 240),
            notifSecondActionMl = notifSecondActionMl.takeIf(::isValidCupMl) ?: def.notifSecondActionMl,
            widgetQuickAmounts = widgetQuickAmounts.filter(::isValidCupMl).distinct().sorted().take(3).ifEmpty { DEFAULT_WIDGET_AMOUNTS },
            mlPerKg = mlPerKg.coerceIn(15, 50),
            minGoalMl = minGoalMl.coerceIn(500, 3000),
            goalWarnMl = goalWarnMl.coerceIn(2000, 8000),
            paceToleranceMl = paceToleranceMl.coerceIn(0, 500),
            undoBannerMs = undoBannerMs.coerceIn(1000, 10000),
            defaultStatsDays = if (defaultStatsDays == 30) 30 else 7,
            historyDays = historyDays.coerceIn(7, 365),
            streakWindowDays = streakWindowDays.coerceIn(30, 365),
            minReminderMl = minReminderMl.coerceIn(50, 500),
            maxReminderMl = maxReminderMl.coerceIn(100, 2000).coerceAtLeast(minReminderMl.coerceIn(50, 500)),
            maxHints = maxHints.coerceIn(1, 10),
            networkTimeoutSec = networkTimeoutSec.coerceIn(3, 30),
            backgroundWeatherTimeoutSec = backgroundWeatherTimeoutSec.coerceIn(3, 30),
        )
    }
}

private val Context.store by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    val flow: Flow<Settings> = context.store.data.map { it.toSettings() }

    suspend fun save(raw: Settings) {
        val s = raw.sanitized()
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
            p[STATUS_NOTIF] = s.statusNotificationEnabled
            p[WEATHER_NOTIF] = s.weatherNotificationEnabled
            p[EXACT_ALARM] = s.preferExactAlarm
            p.putOrRemove(CUSTOM_INTERVAL, s.customReminderIntervalMin)
            p[STOP_BEFORE_BED] = s.stopBeforeBedMin
            p[NOTIF_SECOND_ML] = s.notifSecondActionMl
            p[WIDGET_AMOUNTS] = encodeWidgetAmounts(s.widgetQuickAmounts)
            p[TILE_TOAST] = s.tileToastEnabled
            p[ML_PER_KG_KEY] = s.mlPerKg
            p[MIN_GOAL_KEY] = s.minGoalMl
            p[GOAL_WARN_KEY] = s.goalWarnMl
            p[PACE_TOLERANCE] = s.paceToleranceMl
            p[WATER_WAVE] = s.waterWaveAnimation
            p[OVERFLOW_EFFECT] = s.showOverflowEffect
            p[SHOW_HINTS] = s.showHints
            p[UNDO_BANNER_MS] = s.undoBannerMs
            p[DEFAULT_STATS_DAYS] = s.defaultStatsDays
            p[HISTORY_DAYS] = s.historyDays
            p[STREAK_WINDOW_DAYS] = s.streakWindowDays
            p[MIN_REMINDER] = s.minReminderMl
            p[MAX_REMINDER] = s.maxReminderMl
            p[MAX_HINTS_KEY] = s.maxHints
            p[NETWORK_TIMEOUT] = s.networkTimeoutSec
            p[BG_WEATHER_TIMEOUT] = s.backgroundWeatherTimeoutSec
            p.putOrRemove(GOAL, s.goalOverrideMl)
            p.putOrRemove(CITY, s.city)
            p.putOrRemove(LAT, s.lat)
            p.putOrRemove(LON, s.lon)
            p.putOrRemove(TEMP, s.tempC)
            p.putOrRemove(TEMP_DAY, s.tempDay)
        }
    }

    /** Tüm kullanıcı ayarlarını varsayılan değerlere döndürür (kurulum durumu korunur). */
    suspend fun resetToDefaults(keepOnboarded: Boolean = true) {
        val defaults = Settings(onboarded = keepOnboarded)
        context.store.edit { p ->
            p.clear()
            p[ONBOARDED] = defaults.onboarded
        }
        save(defaults)
    }

    /** Yalnızca bu alanı yazar; ayar kaydıyla yarışmasın. */
    suspend fun setLastNotified(ms: Long) {
        context.store.edit { it[LAST_NOTIFIED] = ms }
    }

    suspend fun setHintCount(n: Int) {
        context.store.edit { it[HINTS] = n.coerceIn(0, 10) }
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
            cupMl = this[CUP]?.takeIf(::isValidCupMl) ?: d.cupMl,
            windowStartMin = this[START] ?: d.windowStartMin,
            windowEndMin = this[END] ?: d.windowEndMin,
            useActivity = this[USE_ACTIVITY] ?: d.useActivity,
            useWeather = this[USE_WEATHER] ?: d.useWeather,
            goalOverrideMl = this[GOAL]?.takeIf { it in MIN_MANUAL_GOAL_ML..MAX_MANUAL_GOAL_ML },
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
            statusNotificationEnabled = this[STATUS_NOTIF] ?: d.statusNotificationEnabled,
            weatherNotificationEnabled = this[WEATHER_NOTIF] ?: d.weatherNotificationEnabled,
            preferExactAlarm = this[EXACT_ALARM] ?: d.preferExactAlarm,
            customReminderIntervalMin = this[CUSTOM_INTERVAL]?.takeIf { it in 15..240 },
            stopBeforeBedMin = this[STOP_BEFORE_BED]?.takeIf { it in 0..240 } ?: d.stopBeforeBedMin,
            notifSecondActionMl = this[NOTIF_SECOND_ML]?.takeIf(::isValidCupMl) ?: d.notifSecondActionMl,
            widgetQuickAmounts = parseWidgetAmounts(this[WIDGET_AMOUNTS]),
            tileToastEnabled = this[TILE_TOAST] ?: d.tileToastEnabled,
            mlPerKg = this[ML_PER_KG_KEY]?.takeIf { it in 15..50 } ?: d.mlPerKg,
            minGoalMl = this[MIN_GOAL_KEY]?.takeIf { it in 500..3000 } ?: d.minGoalMl,
            goalWarnMl = this[GOAL_WARN_KEY]?.takeIf { it in 2000..8000 } ?: d.goalWarnMl,
            paceToleranceMl = this[PACE_TOLERANCE]?.takeIf { it in 0..500 } ?: d.paceToleranceMl,
            waterWaveAnimation = this[WATER_WAVE] ?: d.waterWaveAnimation,
            showOverflowEffect = this[OVERFLOW_EFFECT] ?: d.showOverflowEffect,
            showHints = this[SHOW_HINTS] ?: d.showHints,
            undoBannerMs = this[UNDO_BANNER_MS]?.takeIf { it in 1000..10000 } ?: d.undoBannerMs,
            defaultStatsDays = this[DEFAULT_STATS_DAYS]?.takeIf { it == 7 || it == 30 } ?: d.defaultStatsDays,
            historyDays = this[HISTORY_DAYS]?.takeIf { it in 7..365 } ?: d.historyDays,
            streakWindowDays = this[STREAK_WINDOW_DAYS]?.takeIf { it in 30..365 } ?: d.streakWindowDays,
            minReminderMl = this[MIN_REMINDER]?.takeIf { it in 50..500 } ?: d.minReminderMl,
            maxReminderMl = this[MAX_REMINDER]?.takeIf { it in 100..2000 } ?: d.maxReminderMl,
            maxHints = this[MAX_HINTS_KEY]?.takeIf { it in 1..10 } ?: d.maxHints,
            networkTimeoutSec = this[NETWORK_TIMEOUT]?.takeIf { it in 3..30 } ?: d.networkTimeoutSec,
            backgroundWeatherTimeoutSec = this[BG_WEATHER_TIMEOUT]?.takeIf { it in 3..30 } ?: d.backgroundWeatherTimeoutSec,
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
        val STATUS_NOTIF = booleanPreferencesKey("status_notification_enabled")
        val WEATHER_NOTIF = booleanPreferencesKey("weather_notification_enabled")
        val EXACT_ALARM = booleanPreferencesKey("prefer_exact_alarm")
        val CUSTOM_INTERVAL = intPreferencesKey("custom_reminder_interval_min")
        val STOP_BEFORE_BED = intPreferencesKey("stop_before_bed_min")
        val NOTIF_SECOND_ML = intPreferencesKey("notif_second_action_ml")
        val WIDGET_AMOUNTS = stringPreferencesKey("widget_quick_amounts")
        val TILE_TOAST = booleanPreferencesKey("tile_toast_enabled")
        val ML_PER_KG_KEY = intPreferencesKey("ml_per_kg")
        val MIN_GOAL_KEY = intPreferencesKey("min_goal_ml")
        val GOAL_WARN_KEY = intPreferencesKey("goal_warn_ml")
        val PACE_TOLERANCE = intPreferencesKey("pace_tolerance_ml")
        val WATER_WAVE = booleanPreferencesKey("water_wave_animation")
        val OVERFLOW_EFFECT = booleanPreferencesKey("show_overflow_effect")
        val SHOW_HINTS = booleanPreferencesKey("show_hints")
        val UNDO_BANNER_MS = intPreferencesKey("undo_banner_ms")
        val DEFAULT_STATS_DAYS = intPreferencesKey("default_stats_days")
        val HISTORY_DAYS = intPreferencesKey("history_days")
        val STREAK_WINDOW_DAYS = intPreferencesKey("streak_window_days")
        val MIN_REMINDER = intPreferencesKey("min_reminder_ml")
        val MAX_REMINDER = intPreferencesKey("max_reminder_ml")
        val MAX_HINTS_KEY = intPreferencesKey("max_hints")
        val NETWORK_TIMEOUT = intPreferencesKey("network_timeout_sec")
        val BG_WEATHER_TIMEOUT = intPreferencesKey("bg_weather_timeout_sec")
    }
}
