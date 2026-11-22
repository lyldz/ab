package com.lyldz.ab.ui

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

const val UNIT_ML = 0
const val UNIT_OZ = 1
const val ML_PER_OZ = 29.5735

fun fmtMl(ml: Int): String = NumberFormat.getIntegerInstance().format(ml)

fun mlToOz(ml: Int): Double = ml / ML_PER_OZ

fun ozToMl(oz: Double): Int = (oz * ML_PER_OZ).roundToInt()

fun unitLabel(unit: Int): String = if (unit == UNIT_OZ) "oz" else "ml"

/** Seçilen birime göre yalnızca sayısal hacim (ml: tam sayı, oz: gerekirse 1 ondalık). */
fun fmtVolNum(ml: Int, unit: Int = UNIT_ML): String {
    if (unit != UNIT_OZ) return fmtMl(ml)
    val tenths = (mlToOz(ml) * 10.0).roundToInt()
    val fmt = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = if (tenths % 10 == 0) 0 else 1
        maximumFractionDigits = 1
    }
    return fmt.format(tenths / 10.0)
}

/** Seçilen birime göre hacim + birim ("250 ml" veya "8.5 oz"). */
fun fmtVol(ml: Int, unit: Int = UNIT_ML): String = "${fmtVolNum(ml, unit)} ${unitLabel(unit)}"

fun fmtTime(min: Int): String {
    val safe = min.coerceIn(0, 1439)
    return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60)
}
