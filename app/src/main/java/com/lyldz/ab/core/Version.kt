package com.lyldz.ab.core

/**
 * Sürüm serisinin su temalı adı: yalnızca son rakam değişen sürümler aynı adı taşır, yeni özellikte (orta rakam)
 * yeni bir ad alınır. 0.3 = Testi (su kabı), 0.4 = Pınar. Yeni seri açılırken buraya satır eklenir.
 */
fun seriesName(version: String): String = when (version.removePrefix("v").split('.').take(2).joinToString(".")) {
    "0.2" -> "Damla"
    "0.3" -> "Testi"
    "0.4" -> "Pınar"
    else -> ""
}
