package com.lyldz.ab.data

import com.lyldz.ab.core.MAX_REMINDER_ML
import com.lyldz.ab.core.MIN_REMINDER_ML
import com.lyldz.ab.core.nextPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Saf JVM: her Settings alanı için varsayılan, uç değer, yedek gidiş-dönüşü ve eski davranış korunumu. */
class SettingsAllKeysTest {
    private val def = Settings()

    @Test fun everyFieldIsCoveredByThisSuite() {
        // Yeni bir alan eklenirse bu sayı ve aşağıdaki testler birlikte güncellenmeli.
        val fields = Settings::class.java.declaredFields.filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }
        assertEquals(47, fields.size)
    }

    @Test fun newSettingsDefaultToTheOldHardcodedValues() {
        assertEquals(MIN_REMINDER_ML, def.minReminderMl)
        assertEquals(MAX_REMINDER_ML, def.maxReminderMl)
        assertEquals(100, def.minReminderMl)
        assertEquals(500, def.maxReminderMl)
        assertEquals(3, def.maxHints)
        assertEquals(10, def.networkTimeoutSec)
        assertEquals(6, def.backgroundWeatherTimeoutSec)
    }

    @Test fun everyNumericSettingClampsAtBothEdges() {
        val lo = Settings(
            weightKg = -1, cupMl = -1, windowStartMin = -5, windowEndMin = -5, stopBeforeBedMin = -1, notifSecondActionMl = -1,
            mlPerKg = -1, minGoalMl = -1, goalWarnMl = -1, paceToleranceMl = -1, undoBannerMs = -1, historyDays = -1,
            streakWindowDays = -1, longPressHints = -1, minReminderMl = -1, maxReminderMl = -1, maxHints = -1,
            networkTimeoutSec = -1, backgroundWeatherTimeoutSec = -1, customReminderIntervalMin = -1, goalOverrideMl = -1,
        ).sanitized()
        val hi = Settings(
            weightKg = Int.MAX_VALUE, cupMl = Int.MAX_VALUE, windowStartMin = Int.MAX_VALUE, windowEndMin = Int.MAX_VALUE,
            stopBeforeBedMin = Int.MAX_VALUE, notifSecondActionMl = Int.MAX_VALUE, mlPerKg = Int.MAX_VALUE, minGoalMl = Int.MAX_VALUE,
            goalWarnMl = Int.MAX_VALUE, paceToleranceMl = Int.MAX_VALUE, undoBannerMs = Int.MAX_VALUE, historyDays = Int.MAX_VALUE,
            streakWindowDays = Int.MAX_VALUE, longPressHints = Int.MAX_VALUE, minReminderMl = Int.MAX_VALUE, maxReminderMl = Int.MAX_VALUE,
            maxHints = Int.MAX_VALUE, networkTimeoutSec = Int.MAX_VALUE, backgroundWeatherTimeoutSec = Int.MAX_VALUE,
            customReminderIntervalMin = Int.MAX_VALUE, goalOverrideMl = Int.MAX_VALUE,
        ).sanitized()
        assertEquals(50, lo.minReminderMl); assertEquals(500, hi.minReminderMl)
        assertEquals(100.coerceAtLeast(lo.minReminderMl), lo.maxReminderMl); assertEquals(2000, hi.maxReminderMl)
        assertEquals(1, lo.maxHints); assertEquals(10, hi.maxHints)
        assertEquals(3, lo.networkTimeoutSec); assertEquals(30, hi.networkTimeoutSec)
        assertEquals(3, lo.backgroundWeatherTimeoutSec); assertEquals(30, hi.backgroundWeatherTimeoutSec)
        assertEquals(15, lo.mlPerKg); assertEquals(50, hi.mlPerKg)
        assertEquals(500, lo.minGoalMl); assertEquals(3000, hi.minGoalMl)
        assertEquals(2000, lo.goalWarnMl); assertEquals(8000, hi.goalWarnMl)
        assertEquals(0, lo.paceToleranceMl); assertEquals(500, hi.paceToleranceMl)
        assertEquals(1000, lo.undoBannerMs); assertEquals(10000, hi.undoBannerMs)
        assertEquals(7, lo.historyDays); assertEquals(365, hi.historyDays)
        assertEquals(30, lo.streakWindowDays); assertEquals(365, hi.streakWindowDays)
        assertEquals(0, lo.stopBeforeBedMin); assertEquals(240, hi.stopBeforeBedMin)
        assertEquals(0, lo.longPressHints); assertEquals(10, hi.longPressHints)
        assertEquals(null, lo.customReminderIntervalMin); assertEquals(null, hi.customReminderIntervalMin)
        assertEquals(null, lo.goalOverrideMl); assertEquals(null, hi.goalOverrideMl)
        // sınırlara çekilen ayarlarla hedef/aralık hesabı çökmez
        for (s in listOf(lo, hi)) { s.goalMl(java.time.LocalDate.of(2026, 11, 26)); s.reminderIntervalMin() }
    }

    @Test fun maxReminderNeverFallsBelowMin() {
        val s = Settings(minReminderMl = 400, maxReminderMl = 150).sanitized()
        assertTrue(s.maxReminderMl >= s.minReminderMl)
        assertEquals(400, s.minReminderMl)
        assertEquals(400, s.maxReminderMl)
    }

    private val booleans: List<Pair<String, (Settings, Boolean) -> Settings>> = listOf(
        "useActivity" to { s, v -> s.copy(useActivity = v) },
        "useWeather" to { s, v -> s.copy(useWeather = v) },
        "onboarded" to { s, v -> s.copy(onboarded = v) },
        "motionWater" to { s, v -> s.copy(motionWater = v) },
        "remindersEnabled" to { s, v -> s.copy(remindersEnabled = v) },
        "statusNotificationEnabled" to { s, v -> s.copy(statusNotificationEnabled = v) },
        "weatherNotificationEnabled" to { s, v -> s.copy(weatherNotificationEnabled = v) },
        "preferExactAlarm" to { s, v -> s.copy(preferExactAlarm = v) },
        "tileToastEnabled" to { s, v -> s.copy(tileToastEnabled = v) },
        "waterWaveAnimation" to { s, v -> s.copy(waterWaveAnimation = v) },
        "showOverflowEffect" to { s, v -> s.copy(showOverflowEffect = v) },
        "showHints" to { s, v -> s.copy(showHints = v) },
    )

    private fun read(s: Settings, name: String): Boolean = when (name) {
        "useActivity" -> s.useActivity; "useWeather" -> s.useWeather; "onboarded" -> s.onboarded
        "motionWater" -> s.motionWater; "remindersEnabled" -> s.remindersEnabled
        "statusNotificationEnabled" -> s.statusNotificationEnabled; "weatherNotificationEnabled" -> s.weatherNotificationEnabled
        "preferExactAlarm" -> s.preferExactAlarm; "tileToastEnabled" -> s.tileToastEnabled
        "waterWaveAnimation" -> s.waterWaveAnimation; "showOverflowEffect" -> s.showOverflowEffect; "showHints" -> s.showHints
        else -> error(name)
    }

    @Test fun everyBooleanToggleSurvivesSanitizeAndBackupBothWays() {
        assertEquals(12, booleans.size)
        for ((name, set) in booleans) for (v in listOf(true, false)) {
            val s = set(def.copy(onboarded = true), v)
            assertEquals(name, v, read(s.sanitized(), name))
            val back = Backup.parse(Backup.toJson(BackupData(emptyList(), emptyList(), s), "t", 1L))!!.settings
            if (name != "onboarded") assertEquals("$name backup", v, read(back, name)) else assertTrue(back.onboarded)
        }
    }

    @Test fun backupRoundTripKeepsNewSettingsAndOldBackupsFallBackToDefaults() {
        val s = def.copy(minReminderMl = 250, maxReminderMl = 900, maxHints = 7, networkTimeoutSec = 20, backgroundWeatherTimeoutSec = 12)
        val back = Backup.parse(Backup.toJson(BackupData(emptyList(), emptyList(), s), "t", 1L))!!.settings
        assertEquals(listOf(250, 900, 7, 20, 12), listOf(back.minReminderMl, back.maxReminderMl, back.maxHints, back.networkTimeoutSec, back.backgroundWeatherTimeoutSec))
        val old = """{"format":"ab-backup","version":1,"app":"0.3.11","exportedAt":1,"settings":{"weightKg":80},"intakes":[],"goals":[]}"""
        val o = Backup.parse(old)!!.settings
        assertEquals(80, o.weightKg)
        assertEquals(listOf(100, 500, 3, 10, 6), listOf(o.minReminderMl, o.maxReminderMl, o.maxHints, o.networkTimeoutSec, o.backgroundWeatherTimeoutSec))
    }

    @Test fun badBackupValuesFallBackToDefaultsWithoutCrashing() {
        val bad = """{"format":"ab-backup","version":1,"settings":{"minReminderMl":0,"maxReminderMl":-5,"maxHints":999,"networkTimeoutSec":"x","backgroundWeatherTimeoutSec":1e12},"intakes":[],"goals":[]}"""
        val o = Backup.parse(bad)
        assertNotNull(o)
        val s = o!!.settings
        assertEquals(listOf(100, 500, 3, 10, 6), listOf(s.minReminderMl, s.maxReminderMl, s.maxHints, s.networkTimeoutSec, s.backgroundWeatherTimeoutSec))
    }

    @Test fun nextPlanKeepsOldAmountsByDefaultAndHonoursCustomLimits() {
        val old = nextPlan(1000, 360, 1080, 6000, 0, 60, 1000)!!.amountMl
        assertEquals(500, old)
        assertEquals(500, nextPlan(1000, 360, 1080, 6000, 0, 60, 1000, 100, 500)!!.amountMl)
        assertEquals(800, nextPlan(1000, 360, 1080, 6000, 0, 60, 1000, 100, 800)!!.amountMl)
        assertEquals(100, nextPlan(480, 360, 1080, 600, 0, 30, 480)!!.amountMl) // varsayılan alt sınır 100
        assertEquals(250, nextPlan(480, 360, 1080, 600, 0, 30, 480, 250, 800)!!.amountMl) // özel alt sınır
        assertEquals(30, nextPlan(600, 360, 1080, 2000, 1970, 60, 590, 250, 800)!!.amountMl) // kalandan fazla değil
        assertFalse(nextPlan(600, 360, 1080, 2000, 2500, 60, 500, 0, 0) != null)
    }
}
