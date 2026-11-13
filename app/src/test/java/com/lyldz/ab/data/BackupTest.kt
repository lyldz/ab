package com.lyldz.ab.data

import com.lyldz.ab.core.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    private val settings = Settings(
        weightKg = 82, activity = Activity.HIGH, cupMl = 250, windowStartMin = 420, windowEndMin = 1380,
        useActivity = false, useWeather = true, goalOverrideMl = 3000, city = "Ankara, Türkiye", lat = 39.9, lon = 32.9,
        name = "Deniz", themeMode = 2, onboarded = true, tempC = 31.0, tempDay = "2026-11-13", lastNotifiedMs = 5L,
    )
    private val data = BackupData(
        intakes = listOf(Intake(7, 2_000, 250, SOURCE_NOTIFICATION), Intake(3, 1_000, 200, SOURCE_APP)),
        goals = listOf(DayGoal("2026-11-12", 3125), DayGoal("2026-11-11", 2750)),
        settings = settings,
    )

    @Test fun roundTripKeepsEverythingButTransientFields() {
        val back = Backup.parse(Backup.toJson(data, "0.3.0", 123L))!!
        assertEquals(listOf(Triple(1_000L, 200, SOURCE_APP), Triple(2_000L, 250, SOURCE_NOTIFICATION)), back.intakes.map { Triple(it.atMs, it.ml, it.source) })
        assertEquals(listOf(DayGoal("2026-11-11", 2750), DayGoal("2026-11-12", 3125)), back.goals)
        val s = back.settings
        assertEquals(settings.copy(tempC = null, tempDay = null, lastNotifiedMs = null), s)
    }

    @Test fun unknownFormatOrVersionIsRejected() {
        assertNull(Backup.parse("not json"))
        assertNull(Backup.parse("""{"format":"other","version":1}"""))
        assertNull(Backup.parse("""{"format":"ab-backup","version":2}"""))
        assertNull(Backup.parse("""{"format":"ab-backup"}"""))
    }

    @Test fun badRowsAreSkippedAndBadSettingsFallBackToDefaults() {
        val json = """{"format":"ab-backup","version":1,
            "settings":{"weightKg":999,"cupMl":5,"windowStartMin":900,"windowEndMin":910,"goalOverrideMl":12,"themeMode":9,"activity":"X","lat":200.0,"lon":10.0,"city":"Z"},
            "intakes":[{"at":0,"ml":200},{"at":5,"ml":0},{"at":6,"ml":99999},{"at":7,"ml":300,"src":9},{"at":8,"ml":100}],
            "goals":[{"day":"bad","goal":3000},{"day":"2026-01-01","goal":5},{"day":"2026-01-02","goal":2500}]}"""
        val d = Backup.parse(json)!!
        val def = Settings()
        assertEquals(def.weightKg, d.settings.weightKg)
        assertEquals(def.cupMl, d.settings.cupMl)
        assertEquals(def.windowStartMin, d.settings.windowStartMin) // pencere < 60 dk: varsayılan
        assertEquals(def.windowEndMin, d.settings.windowEndMin)
        assertNull(d.settings.goalOverrideMl)
        assertEquals(def.themeMode, d.settings.themeMode)
        assertEquals(def.activity, d.settings.activity)
        assertNull(d.settings.lat) // geçersiz enlem: yer tümüyle yok sayılır
        assertNull(d.settings.city)
        assertTrue(d.settings.onboarded)
        assertEquals(listOf(7L to 300, 8L to 100), d.intakes.map { it.atMs to it.ml })
        assertEquals(SOURCE_APP, d.intakes.first().source) // geçersiz kaynak: uygulama
        assertEquals(listOf(DayGoal("2026-01-02", 2500)), d.goals)
    }

    @Test fun emptyBackupIsValid() {
        val d = Backup.parse("""{"format":"ab-backup","version":1}""")
        assertNotNull(d)
        assertTrue(d!!.intakes.isEmpty() && d.goals.isEmpty())
    }
}
