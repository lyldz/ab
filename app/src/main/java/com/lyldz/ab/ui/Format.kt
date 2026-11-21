package com.lyldz.ab.ui

import java.text.NumberFormat
import java.util.Locale

fun fmtMl(ml: Int): String = NumberFormat.getIntegerInstance().format(ml)

fun fmtTime(min: Int): String {
    val safe = min.coerceIn(0, 1439)
    return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60)
}
