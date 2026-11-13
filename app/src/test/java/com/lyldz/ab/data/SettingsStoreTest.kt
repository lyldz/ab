package com.lyldz.ab.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Ayarların DataStore'a yazılıp geri okunduğunu doğrular (su hareketi anahtarı ve tema dahil). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsStoreTest {
    @Test fun motionAndThemeArePersisted() = runBlocking {
        val store = SettingsStore(ApplicationProvider.getApplicationContext<Context>())
        store.save(Settings(motionWater = false, themeMode = 2, onboarded = true))
        val saved = store.flow.first()
        assertFalse(saved.motionWater)
        assertEquals(2, saved.themeMode)
        store.save(saved.copy(motionWater = true))
        assertTrue(store.flow.first().motionWater)
    }
}
