package com.lyldz.ab

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Ana ekranın ekran görüntüsünü app/build/screenshots'a yazar (CI yapıtı olarak yüklenir), böylece
 * telefon olmadan görünüm denetlenebilir. Hata verirse testi düşürmez: yalnızca bir yardımcıdır.
 */
@RunWith(AndroidJUnit4::class)
abstract class ScreenshotBase(private val prefix: String) {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun wait(text: String) = compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun tap(text: String) {
        wait(text)
        val n = compose.onNodeWithText(text)
        runCatching { n.performScrollTo() }
        n.performClick()
    }

    private fun shot(name: String) {
        val dir = File("build/screenshots").apply { mkdirs() }
        try {
            compose.waitForIdle()
            // captureToImage Robolectric'te çizim bekleyip zaman aşımına uğruyor; görünümü doğrudan çiz
            val view = compose.activity.window.decorView
            val bmp = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bmp))
            File(dir, "$prefix-$name.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        } catch (t: Throwable) {
            File(dir, "$prefix-$name.error.txt").writeText(t.stackTraceToString())
        }
    }

    @Test
    fun homeScreenshots() {
        wait("Start")
        shot("onb0-welcome")
        tap("Start")
        wait("Continue")
        shot("onb1-profile")
        tap("Continue")
        tap("Not now")
        wait("Use my location")
        shot("onb3-location")
        tap("Not now")
        wait("TODAY'S TIMELINE")
        shot("home-empty")
        repeat(3) { compose.onNodeWithContentDescription("+200 ml").performScrollTo().performClick() }
        shot("home-records")
        shot("home-actions")
        compose.onNodeWithContentDescription("Goal details").performClick()
        wait("How your goal is calculated")
        shot("goal-info")
        compose.onNodeWithText("OK").performClick()
        repeat(6) { compose.onNodeWithContentDescription("+200 ml").performScrollTo().performClick() }
        shot("home-top-filled")
    }
}

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-normal-xxhdpi")
class ScreenshotSmokeTest : ScreenshotBase("light")

@GraphicsMode(GraphicsMode.Mode.NATIVE)
// farklı SDK = ayrı Robolectric kum havuzu: DataStore durumu açık tema testiyle paylaşılmaz
@Config(sdk = [33], qualifiers = "w411dp-h891dp-normal-night-xxhdpi")
class ScreenshotNightSmokeTest : ScreenshotBase("dark")
