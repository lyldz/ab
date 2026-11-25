package com.lyldz.ab.data

import com.lyldz.ab.core.Activity
import com.lyldz.ab.core.MAX_MANUAL_GOAL_ML
import com.lyldz.ab.core.MAX_WEIGHT_KG
import com.lyldz.ab.core.MIN_MANUAL_GOAL_ML
import com.lyldz.ab.core.MIN_WEIGHT_KG
import com.lyldz.ab.core.isValidCupMl
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class BackupData(val intakes: List<Intake>, val goals: List<DayGoal>, val settings: Settings)

/**
 * Yedek dosyası (JSON, sürüm 1): içme kayıtları, günlük hedef anlık görüntüleri ve tüm ayarlar. Geçici
 * alanlar (bugünün sıcaklığı, son bildirim, kurulum bayrağı) yazılmaz. Okurken her alan doğrulanır:
 * bozuk satır atlanır, aralık dışı ayar varsayılana döner; biçim tanınmazsa null.
 */
object Backup {
    const val FORMAT = "ab-backup"
    const val VERSION = 1

    fun toJson(d: BackupData, appVersion: String, exportedAtMs: Long): String {
        val s = d.settings.sanitized()
        val settings = JSONObject()
            .put("weightKg", s.weightKg)
            .put("activity", s.activity.name)
            .put("cupMl", s.cupMl)
            .put("windowStartMin", s.windowStartMin)
            .put("windowEndMin", s.windowEndMin)
            .put("useActivity", s.useActivity)
            .put("useWeather", s.useWeather)
            .put("name", s.name)
            .put("themeMode", s.themeMode)
            .put("motionWater", s.motionWater)
            .put("remindersEnabled", s.remindersEnabled)
            .put("language", s.language)
            .put("unit", s.unit)
            .put("cupPresets", encodeCupPresets(s.cupPresets))
            .put("statusNotificationEnabled", s.statusNotificationEnabled)
            .put("weatherNotificationEnabled", s.weatherNotificationEnabled)
            .put("preferExactAlarm", s.preferExactAlarm)
            .put("stopBeforeBedMin", s.stopBeforeBedMin)
            .put("notifSecondActionMl", s.notifSecondActionMl)
            .put("widgetQuickAmounts", encodeWidgetAmounts(s.widgetQuickAmounts))
            .put("tileToastEnabled", s.tileToastEnabled)
            .put("mlPerKg", s.mlPerKg)
            .put("minGoalMl", s.minGoalMl)
            .put("goalWarnMl", s.goalWarnMl)
            .put("paceToleranceMl", s.paceToleranceMl)
            .put("waterWaveAnimation", s.waterWaveAnimation)
            .put("showOverflowEffect", s.showOverflowEffect)
            .put("showHints", s.showHints)
            .put("undoBannerMs", s.undoBannerMs)
            .put("defaultStatsDays", s.defaultStatsDays)
            .put("historyDays", s.historyDays)
            .put("streakWindowDays", s.streakWindowDays)
        s.customReminderIntervalMin?.let { settings.put("customReminderIntervalMin", it) }
        s.goalOverrideMl?.let { settings.put("goalOverrideMl", it) }
        s.city?.let { settings.put("city", it) }
        s.lat?.let { settings.put("lat", it) }
        s.lon?.let { settings.put("lon", it) }
        val intakes = JSONArray()
        d.intakes.sortedBy { it.atMs }.forEach {
            intakes.put(JSONObject().put("at", it.atMs).put("ml", it.ml).put("src", it.source))
        }
        val goals = JSONArray()
        d.goals.sortedBy { it.day }.forEach { goals.put(JSONObject().put("day", it.day).put("goal", it.goalMl)) }
        return JSONObject()
            .put("format", FORMAT).put("version", VERSION).put("app", appVersion).put("exportedAt", exportedAtMs)
            .put("settings", settings).put("intakes", intakes).put("goals", goals)
            .toString(2)
    }

    fun parse(json: String): BackupData? = runCatching {
        val root = JSONObject(json)
        if (root.optString("format") != FORMAT || root.optInt("version", 0) !in 1..VERSION) return null
        val def = Settings()
        val so = root.optJSONObject("settings") ?: JSONObject()
        val start = so.optInt("windowStartMin", def.windowStartMin)
        val end = so.optInt("windowEndMin", def.windowEndMin)
        val windowOk = start in 0..1439 && end in 1..1440 && end - start >= 60
        val lat = if (so.has("lat")) so.optDouble("lat", Double.NaN).takeIf { it in -90.0..90.0 } else null
        val lon = if (so.has("lon")) so.optDouble("lon", Double.NaN).takeIf { it in -180.0..180.0 } else null
        val place = lat != null && lon != null
        val settings = def.copy(
            weightKg = so.optInt("weightKg", def.weightKg).takeIf { it in MIN_WEIGHT_KG..MAX_WEIGHT_KG } ?: def.weightKg,
            activity = runCatching { Activity.valueOf(so.optString("activity")) }.getOrDefault(def.activity),
            cupMl = so.optInt("cupMl", def.cupMl).takeIf(::isValidCupMl) ?: def.cupMl,
            windowStartMin = if (windowOk) start else def.windowStartMin,
            windowEndMin = if (windowOk) end else def.windowEndMin,
            useActivity = so.optBoolean("useActivity", def.useActivity),
            useWeather = so.optBoolean("useWeather", def.useWeather),
            goalOverrideMl = if (so.has("goalOverrideMl")) so.optInt("goalOverrideMl").takeIf { it in MIN_MANUAL_GOAL_ML..MAX_MANUAL_GOAL_ML } else null,
            city = if (place) so.optString("city").takeIf { it.isNotBlank() }?.take(100) else null,
            lat = if (place) lat else null,
            lon = if (place) lon else null,
            name = so.optString("name", "").take(30),
            themeMode = so.optInt("themeMode", def.themeMode).takeIf { it in 0..2 } ?: def.themeMode,
            motionWater = so.optBoolean("motionWater", def.motionWater),
            remindersEnabled = so.optBoolean("remindersEnabled", def.remindersEnabled),
            language = so.optString("language", def.language).takeIf { it == "" || it == "tr" || it == "en" } ?: def.language,
            unit = so.optInt("unit", def.unit).takeIf { it in 0..1 } ?: def.unit,
            cupPresets = if (so.has("cupPresets")) parseCupPresets(so.optString("cupPresets")) else def.cupPresets,
            statusNotificationEnabled = so.optBoolean("statusNotificationEnabled", def.statusNotificationEnabled),
            weatherNotificationEnabled = so.optBoolean("weatherNotificationEnabled", def.weatherNotificationEnabled),
            preferExactAlarm = so.optBoolean("preferExactAlarm", def.preferExactAlarm),
            customReminderIntervalMin = if (so.has("customReminderIntervalMin")) so.optInt("customReminderIntervalMin").takeIf { it in 15..240 } else null,
            stopBeforeBedMin = so.optInt("stopBeforeBedMin", def.stopBeforeBedMin).takeIf { it in 0..240 } ?: def.stopBeforeBedMin,
            notifSecondActionMl = so.optInt("notifSecondActionMl", def.notifSecondActionMl).takeIf(::isValidCupMl) ?: def.notifSecondActionMl,
            widgetQuickAmounts = if (so.has("widgetQuickAmounts")) parseWidgetAmounts(so.optString("widgetQuickAmounts")) else def.widgetQuickAmounts,
            tileToastEnabled = so.optBoolean("tileToastEnabled", def.tileToastEnabled),
            mlPerKg = so.optInt("mlPerKg", def.mlPerKg).takeIf { it in 15..50 } ?: def.mlPerKg,
            minGoalMl = so.optInt("minGoalMl", def.minGoalMl).takeIf { it in 500..3000 } ?: def.minGoalMl,
            goalWarnMl = so.optInt("goalWarnMl", def.goalWarnMl).takeIf { it in 2000..8000 } ?: def.goalWarnMl,
            paceToleranceMl = so.optInt("paceToleranceMl", def.paceToleranceMl).takeIf { it in 0..500 } ?: def.paceToleranceMl,
            waterWaveAnimation = so.optBoolean("waterWaveAnimation", def.waterWaveAnimation),
            showOverflowEffect = so.optBoolean("showOverflowEffect", def.showOverflowEffect),
            showHints = so.optBoolean("showHints", def.showHints),
            undoBannerMs = so.optInt("undoBannerMs", def.undoBannerMs).takeIf { it in 1000..10000 } ?: def.undoBannerMs,
            defaultStatsDays = so.optInt("defaultStatsDays", def.defaultStatsDays).takeIf { it == 7 || it == 30 } ?: def.defaultStatsDays,
            historyDays = so.optInt("historyDays", def.historyDays).takeIf { it in 7..365 } ?: def.historyDays,
            streakWindowDays = so.optInt("streakWindowDays", def.streakWindowDays).takeIf { it in 30..365 } ?: def.streakWindowDays,
            onboarded = true,
        )
        val intakes = mutableListOf<Intake>()
        root.optJSONArray("intakes")?.let { a ->
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                val at = o.optLong("at", 0L)
                val ml = o.optInt("ml", 0)
                if (at <= 0L || ml !in 1..5000) continue
                intakes += Intake(atMs = at, ml = ml, source = o.optInt("src", SOURCE_APP).takeIf { it in 0..3 } ?: SOURCE_APP)
            }
        }
        val goals = mutableListOf<DayGoal>()
        root.optJSONArray("goals")?.let { a ->
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                val day = runCatching { LocalDate.parse(o.optString("day")) }.getOrNull() ?: continue
                val g = o.optInt("goal", 0)
                if (g in 100..20000) goals += DayGoal(day.toString(), g)
            }
        }
        BackupData(intakes, goals, settings)
    }.getOrNull()
}
