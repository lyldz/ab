package com.lyldz.ab.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lyldz.ab.core.Activity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Tüm ayarların DataStore'a yazılıp geri okunduğunu, toggle, reset ve uç değer güvenliğini doğrular. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsStoreTest {
    @Test fun allSettingsPersistAndReload() = runBlocking {
        val store = SettingsStore(ApplicationProvider.getApplicationContext<Context>())
        store.resetToDefaults(keepOnboarded = false)
        val custom = Settings(
            weightKg = 84,
            activity = Activity.HIGH,
            cupMl = 350,
            windowStartMin = 7 * 60,
            windowEndMin = 21 * 60,
            useActivity = false,
            useWeather = false,
            goalOverrideMl = 3200,
            city = "İzmir",
            lat = 38.4,
            lon = 27.1,
            onboarded = true,
            name = "Ali",
            themeMode = 2,
            motionWater = false,
            remindersEnabled = false,
            language = "en",
            unit = 1,
            cupPresets = listOf(100, 300, 600),
            statusNotificationEnabled = false,
            weatherNotificationEnabled = false,
            preferExactAlarm = false,
            customReminderIntervalMin = 45,
            stopBeforeBedMin = 90,
            notifSecondActionMl = 750,
            widgetQuickAmounts = listOf(300, 750),
            tileToastEnabled = false,
            mlPerKg = 35,
            minGoalMl = 1800,
            goalWarnMl = 6000,
            paceToleranceMl = 120,
            waterWaveAnimation = false,
            showOverflowEffect = false,
            showHints = false,
            undoBannerMs = 4000,
            defaultStatsDays = 30,
            historyDays = 90,
            streakWindowDays = 180,
            minReminderMl = 150,
            maxReminderMl = 700,
            maxHints = 5,
            networkTimeoutSec = 15,
            backgroundWeatherTimeoutSec = 9,
        )
        store.save(custom)
        val loaded = store.flow.first()
        assertEquals(custom, loaded.copy(longPressHints = custom.longPressHints))
        assertEquals(45, loaded.reminderIntervalMin())
    }

    @Test fun allBooleanTogglesFlipTrueAndFalse() = runBlocking {
        val store = SettingsStore(ApplicationProvider.getApplicationContext<Context>())
        val allOff = Settings(
            onboarded = true,
            useActivity = false,
            useWeather = false,
            motionWater = false,
            remindersEnabled = false,
            statusNotificationEnabled = false,
            weatherNotificationEnabled = false,
            preferExactAlarm = false,
            tileToastEnabled = false,
            waterWaveAnimation = false,
            showOverflowEffect = false,
            showHints = false,
        )
        store.save(allOff)
        val readOff = store.flow.first()
        assertFalse(readOff.useActivity)
        assertFalse(readOff.useWeather)
        assertFalse(readOff.motionWater)
        assertFalse(readOff.remindersEnabled)
        assertFalse(readOff.statusNotificationEnabled)
        assertFalse(readOff.weatherNotificationEnabled)
        assertFalse(readOff.preferExactAlarm)
        assertFalse(readOff.tileToastEnabled)
        assertFalse(readOff.waterWaveAnimation)
        assertFalse(readOff.showOverflowEffect)
        assertFalse(readOff.showHints)

        val allOn = readOff.copy(
            useActivity = true,
            useWeather = true,
            motionWater = true,
            remindersEnabled = true,
            statusNotificationEnabled = true,
            weatherNotificationEnabled = true,
            preferExactAlarm = true,
            tileToastEnabled = true,
            waterWaveAnimation = true,
            showOverflowEffect = true,
            showHints = true,
        )
        store.save(allOn)
        val readOn = store.flow.first()
        assertTrue(readOn.useActivity)
        assertTrue(readOn.useWeather)
        assertTrue(readOn.motionWater)
        assertTrue(readOn.remindersEnabled)
        assertTrue(readOn.statusNotificationEnabled)
        assertTrue(readOn.weatherNotificationEnabled)
        assertTrue(readOn.preferExactAlarm)
        assertTrue(readOn.tileToastEnabled)
        assertTrue(readOn.waterWaveAnimation)
        assertTrue(readOn.showOverflowEffect)
        assertTrue(readOn.showHints)
    }

    @Test fun resetToDefaultsRestoresFactoryValuesAndKeepsOnboarded() = runBlocking {
        val store = SettingsStore(ApplicationProvider.getApplicationContext<Context>())
        store.save(
            Settings(
                weightKg = 110,
                cupMl = 600,
                themeMode = 2,
                unit = 1,
                motionWater = false,
                waterWaveAnimation = false,
                customReminderIntervalMin = 90,
                mlPerKg = 40,
                historyDays = 120,
                onboarded = true,
            ),
        )
        store.resetToDefaults(keepOnboarded = true)
        val reset = store.flow.first()
        assertEquals(Settings(onboarded = true), reset)
        assertTrue(reset.onboarded)
    }

    @Test fun extremeAndInvalidValuesDoNotCrashAndFallbackSafely() = runBlocking {
        val store = SettingsStore(ApplicationProvider.getApplicationContext<Context>())
        val extreme = Settings(
            weightKg = 0,
            cupMl = -50,
            windowStartMin = -100,
            windowEndMin = 0,
            goalOverrideMl = 0,
            city = "   ",
            lat = Double.NaN,
            lon = Double.POSITIVE_INFINITY,
            tempC = Double.NaN,
            name = "A".repeat(100),
            themeMode = 99,
            language = "xx",
            unit = 9,
            cupPresets = emptyList(),
            customReminderIntervalMin = 0,
            stopBeforeBedMin = -10,
            notifSecondActionMl = 99999,
            widgetQuickAmounts = listOf(0, -1, 99999),
            mlPerKg = 0,
            minGoalMl = 0,
            goalWarnMl = 0,
            paceToleranceMl = -5,
            undoBannerMs = 0,
            defaultStatsDays = 999,
            historyDays = 0,
            streakWindowDays = 9999,
            onboarded = true,
        )
        store.save(extreme)
        val safe = store.flow.first()
        val def = Settings()
        assertEquals(def.weightKg, safe.weightKg)
        assertEquals(def.cupMl, safe.cupMl)
        assertTrue(safe.windowEndMin - safe.windowStartMin >= 60)
        assertNull(safe.goalOverrideMl)
        assertNull(safe.city)
        assertNull(safe.lat)
        assertNull(safe.lon)
        assertEquals(30, safe.name.length)
        assertEquals(def.themeMode, safe.themeMode)
        assertEquals(def.language, safe.language)
        assertEquals(def.unit, safe.unit)
        assertEquals(DEFAULT_CUP_PRESETS, safe.cupPresets)
        assertNull(safe.customReminderIntervalMin)
        assertEquals(0, safe.stopBeforeBedMin)
        assertEquals(def.notifSecondActionMl, safe.notifSecondActionMl)
        assertEquals(DEFAULT_WIDGET_AMOUNTS, safe.widgetQuickAmounts)
        assertEquals(15, safe.mlPerKg)
        assertEquals(500, safe.minGoalMl)
        assertEquals(2000, safe.goalWarnMl)
        assertEquals(0, safe.paceToleranceMl)
        assertEquals(1000, safe.undoBannerMs)
        assertEquals(7, safe.defaultStatsDays)
        assertEquals(7, safe.historyDays)
        assertEquals(365, safe.streakWindowDays)
        assertTrue(safe.goalMl(LocalDate.now()) >= 500)
        assertTrue(safe.reminderIntervalMin() >= 1)
    }
}
