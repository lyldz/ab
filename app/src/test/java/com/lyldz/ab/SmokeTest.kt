package com.lyldz.ab

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Gerçek MainActivity'yi açıp ekranlar arasında gezer. Amaç: derleme ve birim testlerinin
 * yakalayamadığı çalışma anı Compose çökmelerini (örn. sürüm uyumsuzluğundan LocalLifecycleOwner
 * hatası) CI'da yakalamak. Metinler İngilizce varsayılan dilden okunur.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** Bulunamazsa ekranın anlamsal ağacını hata mesajına koyar, CI günlüğünden teşhis edilebilsin. */
    private fun waitFor(text: String) {
        try {
            compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("'$text' bulunamadı. Ekran:\n" + compose.onRoot().printToString(), e)
        }
    }

    private fun tap(text: String) {
        waitFor(text)
        compose.onNodeWithText(text).performClick()
    }

    @Test
    fun onboardingThenAllTabsOpenWithoutCrashing() {
        // karşılama → bilgiler → bildirim izni → konum izni → ana ekran
        tap("Start")
        tap("Continue")
        tap("Not now")
        tap("Not now")

        waitFor("Glass: 200 ml") // ana ekran açıldı
        waitFor("Today's records")
        tap("+200 ml") // kayıt ekle
        waitFor("Added 200 ml")

        tap("Statistics") // alt gezinme: İstatistik
        waitFor("7 days")
        tap("Settings") // alt gezinme: Ayarlar
        waitFor("Weight (kg)")
        tap("Home")
        waitFor("Glass: 200 ml")
    }
}
