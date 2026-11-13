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

    @Test fun fixedIntervalFromWindowCupAndPlanGoal() {
        assertEquals(51, baseIntervalMin(360, 1080, 200, 2850)) // 720*200/2850 = 50,5
        assertEquals(60, baseIntervalMin(360, 1080, 200, 2400))
        assertEquals(60, baseIntervalMin(600, 600, 200, 2400)) // bozuk pencere: varsayılan
    }

    @Test fun planUsesFixedIntervalAndAnchor() {
        // 06:00-18:00, hedef 2750, 08:00'de 1000 ml içilmiş, sabit aralık 60
        val p = nextPlan(480, 360, 1080, 2750, 1000, 60, 480)!!
        assertEquals(60, p.intervalMin)
        assertEquals(540, p.nextAtMin)
        // kalan 1750 ml, 09:00'dan sonra pencereye 9 hatırlatma sığar: 194 -> 200 ml
        assertEquals(200, p.amountMl)
    }

    @Test fun behindNeverTightensIntervalButRaisesAmount() {
        // 13:00, hiç içilmemiş, hedef 3125: aralık hâlâ 46 dk, miktar kalan toplama göre büyür
        val p = nextPlan(780, 360, 1080, 3125, 0, 46, 734)!!
        assertEquals(46, p.intervalMin)
        assertEquals(780, p.nextAtMin)
        assertEquals(450, p.amountMl) // 3125 / 7 hatırlatma = 446 -> 450
    }

    @Test fun amountFollowsRemainingTotalNotCup() {
        // 12:00'de 3000 hedefin 1100'ü içilmiş, 1900 kaldı, aralık 50
        val p = nextPlan(720, 360, 1080, 3000, 1100, 50, 720)!!
        assertEquals(770, p.nextAtMin)
        assertEquals(250, p.amountMl) // 1900 / 7 = 271 -> 250
    }

    @Test fun amountIsCappedAndNeverExceedsRemaining() {
        assertEquals(500, nextPlan(1000, 360, 1080, 6000, 0, 60, 1000)!!.amountMl) // üst sınır
        assertEquals(30, nextPlan(600, 360, 1080, 2000, 1970, 60, 590)!!.amountMl) // kalandan fazla değil
    }

    @Test fun noPlanWhenGoalReachedOrWindowOver() {
        assertNull(nextPlan(600, 360, 1080, 2000, 2000, 60, 590))
        assertNull(nextPlan(1080, 360, 1080, 2000, 500, 60, 900))
    }

    @Test fun noPlanWhenNextPastWindow() {
        // 17:50'de içildi: sıradaki 18:50, pencere dışı, bildirim yok
        assertNull(nextPlan(1070, 360, 1080, 2000, 1800, 60, 1070))
    }

    @Test fun overdueFiresNow() {
        // son içiş 1000'de, aralık 60, şimdi 1070: gecikmiş hatırlatma hemen
        assertEquals(1070, nextPlan(1070, 360, 1080, 2000, 1800, 60, 1000)!!.nextAtMin)
    }

    @Test fun beforeWindowStartsFromWindowStart() {
        val p = nextPlan(300, 360, 1080, 2400, 0, 60, null)!!
        assertEquals(420, p.nextAtMin) // pencere başı + aralık
        assertEquals(200, p.amountMl) // 2400 / 11 = 218 -> 200
    }

    @Test fun ignoredReminderSpacesNextOne() {
        // 08:00'de hatırlatma gösterildi, içilmedi: sıradaki çıpa + sabit aralık, hemen tekrar değil
        val p = nextPlan(480, 360, 1080, 2400, 0, 60, 480)!!
        assertEquals(540, p.nextAtMin)
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

}
