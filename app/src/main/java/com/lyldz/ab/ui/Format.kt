package com.lyldz.ab.ui

import java.text.NumberFormat

fun fmtMl(ml: Int): String = NumberFormat.getIntegerInstance().format(ml)

fun fmtTime(min: Int): String = "%02d:%02d".format(min / 60, min % 60)
