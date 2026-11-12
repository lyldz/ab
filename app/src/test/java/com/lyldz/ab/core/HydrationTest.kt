package com.lyldz.ab.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HydrationTest {
    @Test fun tempTiers() {
        assertEquals(0, tempExtraMl(-5.0))
        assertEquals(0, tempExtraMl(20.4))
        assertEquals(250, tempExtraMl(21.0))
        assertEquals(500, tempExtraMl(30.0))
        assertEquals(750, tempExtraMl(35.0))
        assertEquals(1000, tempExtraMl(35.6))
    }

    @Test fun baseGoalHasFloor() {
        assertEquals(1500, baseGoalMl(50))
        assertEquals(1750, baseGoalMl(70))
        assertEquals(2125, baseGoalMl(85))
    }

    @Test fun dailyGoalExample() {
        // 70 kg, orta hareket, 31-35°C: 1750 + 250 + 750
        assertEquals(2750, dailyGoalMl(70, Activity.MEDIUM, 33.0))
    }

    @Test fun togglesAndMissingTemp() {
        assertEquals(1750, dailyGoalMl(70, Activity.HIGH, 40.0, useActivity = false, useWeather = false))
        assertEquals(2250, dailyGoalMl(70, Activity.HIGH, null))
    }

    @Test fun planExactMinutes() {
        // 06:00-18:00, hedef 2750, 08:00'de toplam 1000 ml içilmiş, bardak 200
        val p = nextPlan(480, 360, 1080, 2750, 1000, 200, 480)!!
        assertEquals(69, p.intervalMin) // 600 / 8.75 = 68.57
        assertEquals(549, p.nextAtMin)
        assertEquals(200, p.amountMl)
    }

    @Test fun noPlanWhenGoalReachedOrWindowOver() {
        assertNull(nextPlan(600, 360, 1080, 2000, 2000, 200, 590))
        assertNull(nextPlan(1080, 360, 1080, 2000, 500, 200, 900))
    }

    @Test fun noPlanWhenNextPastWindow() {
        // 17:50'de içildi, 1 bardak ve 10 dk kaldı: sıradaki 18:00'e denk gelir, bildirim yok
        assertNull(nextPlan(1070, 360, 1080, 2000, 1800, 200, 1070))
    }

    @Test fun overdueFiresNow() {
        // son içişten 1000'de, şimdi 1070: gecikmiş hatırlatma hemen
        assertEquals(1070, nextPlan(1070, 360, 1080, 2000, 1800, 200, 1000)!!.nextAtMin)
    }

    @Test fun beforeWindowStartsFromWindowStart() {
        val p = nextPlan(300, 360, 1080, 2400, 0, 200, null)!!
        assertEquals(60, p.intervalMin) // 720 / 12
        assertEquals(420, p.nextAtMin)
    }

    @Test fun lastPartialCupGetsReminder() {
        // 100 ml kaldı: blok 100 ml, aralık kalan sürenin tamamı (480 dk), son içişten sonra
        val p = nextPlan(600, 360, 1080, 2000, 1900, 200, 590)!!
        assertEquals(100, p.amountMl)
        assertEquals(480, p.intervalMin)
        assertEquals(1070, p.nextAtMin)
    }

    @Test fun paceIsLinearInsideWindow() {
        assertEquals(0, paceMl(2400, 360, 1080, 300)) // pencere öncesi
        assertEquals(0, paceMl(2400, 360, 1080, 360))
        assertEquals(1200, paceMl(2400, 360, 1080, 720)) // tam orta
        assertEquals(2400, paceMl(2400, 360, 1080, 1080))
        assertEquals(2400, paceMl(2400, 360, 1080, 1200)) // pencere sonrası
    }

    @Test fun paceWithBrokenWindowIsGoal() {
        assertEquals(2000, paceMl(2000, 600, 600, 700))
    }

    @Test fun roundsToTens() {
        assertEquals(450, roundTo10(447))
        assertEquals(450, roundTo10(450))
        assertEquals(460, roundTo10(455))
        assertEquals(0, roundTo10(4))
    }

    @Test fun ignoredReminderSpacesNextOne() {
        // 08:00'de hatırlatma gösterildi, içilmedi: sıradaki çıpa + aralık, hemen tekrar değil
        val p = nextPlan(480, 360, 1080, 2400, 0, 200, 480)!!
        assertEquals(480 + p.intervalMin, p.nextAtMin)
    }
}
