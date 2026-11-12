package com.lyldz.ab.core

import kotlin.math.max
import kotlin.math.roundToInt

/** Kullanıcının seçtiği hareket seviyesi ve hedefe eklediği miktar (docs/SPEC.md). */
enum class Activity(val extraMl: Int) { NONE(0), MEDIUM(250), HIGH(500) }

const val MIN_GOAL_ML = 1500
const val ML_PER_KG = 25
const val GOAL_WARN_ML = 5000

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

/**
 * Aralık = kalan süre ÷ (kalan ml ÷ blok), blok = min(bardak, kalan ml); tam dakikaya yuvarlanır, alt sınır yok.
 * Sayım çıpadan yapılır: son içiş veya son gösterilen hatırlatmanın en geç olanı (yoksa pencere başı). Hedef doluysa veya sıradaki
 * hatırlatma pencereyi aşıyorsa null: o gün bildirim yok.
 */
fun nextPlan(
    nowMin: Int,
    windowStartMin: Int,
    windowEndMin: Int,
    goalMl: Int,
    drankMl: Int,
    cupMl: Int,
    anchorMin: Int?,
): Plan? {
    val remainingMl = goalMl - drankMl
    val remainingMin = windowEndMin - max(nowMin, windowStartMin)
    if (remainingMl <= 0 || remainingMin <= 0 || cupMl <= 0) return null
    val amount = minOf(cupMl, remainingMl)
    val interval = max(1, (remainingMin * amount.toDouble() / remainingMl).roundToInt())
    val anchor = max(windowStartMin, anchorMin ?: windowStartMin)
    val next = max(anchor + interval, nowMin)
    if (next >= windowEndMin) return null
    return Plan(next, interval, amount)
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
