package com.lyldz.ab.ui

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

private const val PREF_NAME = "ab_locale"
private const val KEY_LANG = "lang"

/**
 * Ek bağımlılık (AppCompat) olmadan uygulama dili desteği:
 * - Android 13+ (API 33+): yerel LocaleManager.applicationLocales
 * - Android 8–12 (API 26–32): attachBaseContext ile Configuration sarmalama + Activity.recreate()
 */
object AppLocale {
    fun savedTag(context: Context): String = runCatching {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_LANG, "") ?: ""
    }.getOrDefault("")

    fun wrapContext(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = savedTag(base)
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /** Dili kaydeder ve uygular. Android 8–12'de Activity.recreate() gerekirse true döner. */
    fun apply(context: Context, tag: String): Boolean {
        val clean = if (tag == "tr" || tag == "en") tag else ""
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString(KEY_LANG, clean).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val lm = context.getSystemService(LocaleManager::class.java)
            lm?.applicationLocales = if (clean.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(clean)
            return false
        }
        return true
    }
}
