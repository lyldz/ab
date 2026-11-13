package com.lyldz.ab.core

import kotlin.math.max
import kotlin.math.roundToInt

/** Kullanıcının seçtiği hareket seviyesi ve hedefe eklediği miktar (docs/SPEC.md). */
enum class Activity(val extraMl: Int) { NONE(0), MEDIUM(250), HIGH(500) }

const val MIN_GOAL_ML = 1500
const val ML_PER_KG = 25
const val GOAL_WARN_ML = 5000

/** Uyku kalitesi için su içme, yatıştan bu kadar dakika önce biter: pencere = kalkış → yatış − 2 saat. */
const val STOP_BEFORE_BED_MIN = 120

/** Tempo farkı bu değerin altındaysa "tam tempodasın" denir (ana ekran ve bildirim ortak). */
const val PACE_TOLERANCE_ML = 50

/** Günün tahmini en yüksek sıcaklığına göre ek su. Soğukta düşürme yok. */
fun tempExtraMl(maxTempC: Double): Int = when (maxTempC.roundToInt()) {
    in Int.MIN_VALUE..20 -> 0
    in 21..25 -> 250
    in 26..30 -> 500
    in 31..35 -> 750
    else -> 1000
}

fun baseGoalMl(kg: Int): Int = max(MIN_GOAL_ML, ML_PER_KG * kg)

/** Hareket ve sıcaklık eki ayardan kapatılabilir; tempC null ise (veri yok) sıcaklık eki 0. */
fun dailyGoalMl(
    kg: Int,
    activity: Activity,
    tempC: Double?,
    useActivity: Boolean = true,
    useWeather: Boolean = true,
): Int = baseGoalMl(kg) +
    (if (useActivity) activity.extraMl else 0) +
    (if (useWeather && tempC != null) tempExtraMl(tempC) else 0)

/** Bir sonraki hatırlatma. Zamanlar gün içindeki dakikadır (0..1439). */
data class Plan(val nextAtMin: Int, val intervalMin: Int, val amountMl: Int)

/** Hatırlatma miktarı: kalan toplamı kalan hatırlatmalara böler, 50 ml'ye yuvarlar, bu sınırlarda tutar. */
const val MIN_REMINDER_ML = 100
const val MAX_REMINDER_ML = 500
const val REMINDER_STEP_ML = 50

/**
 * Günün sabit hatırlatma aralığı (dk) = pencere × bardak ÷ plan hedefi. Plan hedefi hava eki içermez
 * (bkz. Settings.planGoalMl), böylece aralık yalnızca kullanıcı kilo, hareket, rutin, bardak ya da elle
 * hedefi değiştirince değişir; gün içinde ve günler arasında sabittir. Geride kalınca sıklaşmaz.
 */
fun baseIntervalMin(windowStartMin: Int, windowEndMin: Int, cupMl: Int, planGoalMl: Int): Int {
    val window = windowEndMin - windowStartMin
    if (window <= 0 || cupMl <= 0 || planGoalMl <= 0) return 60
    return max(1, (window * cupMl.toDouble() / planGoalMl).roundToInt())
}

/**
 * Sıradaki hatırlatma: zaman = çıpa + sabit [intervalMin] (çıpa son içiş veya son gösterilen hatırlatmanın
 * en geç olanı, yoksa pencere başı; geçmişse hemen). Miktar = kalan toplam ml ÷ pencereye sığan kalan
 * hatırlatma sayısı (bardak boyutuna değil kalan toplama bakar), 50 ml'ye yuvarlanır, 100-500 ml arası,
 * kalandan fazla olmaz. Hedef doluysa veya sıradaki hatırlatma pencereyi aşıyorsa null: o gün bildirim yok.
 */
fun nextPlan(
    nowMin: Int,
    windowStartMin: Int,
    windowEndMin: Int,
    goalMl: Int,
    drankMl: Int,
    intervalMin: Int,
    anchorMin: Int?,
): Plan? {
    val remainingMl = goalMl - drankMl
    if (remainingMl <= 0 || windowEndMin - max(nowMin, windowStartMin) <= 0 || intervalMin <= 0) return null
    val anchor = max(windowStartMin, anchorMin ?: windowStartMin)
    val next = max(anchor + intervalMin, nowMin)
    if (next >= windowEndMin) return null
    val slots = (windowEndMin - 1 - next) / intervalMin + 1
    val rounded = (remainingMl.toDouble() / slots / REMINDER_STEP_ML).roundToInt() * REMINDER_STEP_ML
    val amount = rounded.coerceIn(MIN_REMINDER_ML, MAX_REMINDER_ML).coerceAtMost(remainingMl)
    return Plan(next, intervalMin, amount)
}

/**
 * Rutin penceresinde düz çizgi tempo: [atMin] anında toplamda içilmiş olması gereken miktar.
 * Pencere başında 0, sonunda hedefin tamamı; dışarıda uçlara sabitlenir.
 */
fun paceMl(goalMl: Int, startMin: Int, endMin: Int, atMin: Int): Int {
    if (endMin <= startMin) return goalMl
    val f = ((atMin - startMin).toDouble() / (endMin - startMin)).coerceIn(0.0, 1.0)
    return (goalMl * f).roundToInt()
}

/** Ekranda 10 ml'ye yuvarlanmış fark (küçük sapmalar rahatsız etmesin). */
fun roundTo10(ml: Int): Int = ((ml + 5) / 10) * 10
