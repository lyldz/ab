package com.lyldz.ab.data

import com.lyldz.ab.core.Activity
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class BackupData(val intakes: List<Intake>, val goals: List<DayGoal>, val settings: Settings)

/**
 * Yedek dosyası (JSON, sürüm 1): içme kayıtları, günlük hedef anlık görüntüleri ve ayarlar. Geçici
 * alanlar (bugünün sıcaklığı, son bildirim, kurulum bayrağı) yazılmaz. Okurken her alan doğrulanır:
 * bozuk satır atlanır, aralık dışı ayar varsayılana döner; biçim tanınmazsa null.
 */
object Backup {
    const val FORMAT = "ab-backup"
    const val VERSION = 1

    fun toJson(d: BackupData, appVersion: String, exportedAtMs: Long): String {
        val s = d.settings
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
            weightKg = so.optInt("weightKg", def.weightKg).takeIf { it in 30..250 } ?: def.weightKg,
            activity = runCatching { Activity.valueOf(so.optString("activity")) }.getOrDefault(def.activity),
            cupMl = so.optInt("cupMl", def.cupMl).takeIf { it in 100..1000 } ?: def.cupMl,
            windowStartMin = if (windowOk) start else def.windowStartMin,
            windowEndMin = if (windowOk) end else def.windowEndMin,
            useActivity = so.optBoolean("useActivity", def.useActivity),
            useWeather = so.optBoolean("useWeather", def.useWeather),
            goalOverrideMl = if (so.has("goalOverrideMl")) so.optInt("goalOverrideMl").takeIf { it in 500..8000 } else null,
            city = if (place) so.optString("city").takeIf { it.isNotBlank() }?.take(100) else null,
            lat = if (place) lat else null,
            lon = if (place) lon else null,
            name = so.optString("name", "").take(30),
            themeMode = so.optInt("themeMode", def.themeMode).takeIf { it in 0..2 } ?: def.themeMode,
            onboarded = true,
        )
        val intakes = mutableListOf<Intake>()
        root.optJSONArray("intakes")?.let { a ->
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                val at = o.optLong("at", 0L)
                val ml = o.optInt("ml", 0)
                if (at <= 0L || ml !in 1..5000) continue
                intakes += Intake(atMs = at, ml = ml, source = o.optInt("src", SOURCE_APP).takeIf { it in 0..2 } ?: SOURCE_APP)
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
