package com.lyldz.ab.notify

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lyldz.ab.data.AbDb
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.data.Settings
import com.lyldz.ab.data.SettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/**
 * AndroidManifest DATE_CHANGED receiver kaydı, Android 13+ POST_NOTIFICATIONS izni
 * ve çevrimdışı (ağ yokken) alarm/hava durumu güvenilirliği testleri.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class NotifyAndOfflineTest {
    private lateinit var ctx: Application
    private lateinit var db: AbDb
    private lateinit var repo: AbRepository

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, AbDb::class.java).allowMainThreadQueries().build()
        repo = AbRepository(db.intake(), SettingsStore(ctx), ZoneId.of("UTC"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun manifestRegistersSystemEventsReceiverForDateChanged() {
        val receivers = ctx.packageManager.queryBroadcastReceivers(Intent(Intent.ACTION_DATE_CHANGED), 0)
        assertTrue(
            "SystemEventsReceiver must handle ACTION_DATE_CHANGED in AndroidManifest.xml",
            receivers.any { it.activityInfo.name == SystemEventsReceiver::class.java.name },
        )
        assertTrue(Intent.ACTION_DATE_CHANGED in SystemEventsReceiver.SUPPORTED_SYSTEM_ACTIONS)
        assertTrue(Intent.ACTION_BOOT_COMPLETED in SystemEventsReceiver.SUPPORTED_SYSTEM_ACTIONS)
        assertTrue(Intent.ACTION_TIME_CHANGED in SystemEventsReceiver.SUPPORTED_SYSTEM_ACTIONS)
        assertTrue(Intent.ACTION_TIMEZONE_CHANGED in SystemEventsReceiver.SUPPORTED_SYSTEM_ACTIONS)
        assertTrue(Intent.ACTION_MY_PACKAGE_REPLACED in SystemEventsReceiver.SUPPORTED_SYSTEM_ACTIONS)
    }

    @Test
    fun notifierChecksPostNotificationsPermissionOnApi33Plus() {
        val appShadow = shadowOf(ctx)
        appShadow.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(Notifier.hasPermission(ctx))

        appShadow.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(Notifier.hasPermission(ctx))
    }

    @Test
    fun refreshWeatherSkipsCleanlyWhenOfflineAndDoesNotMarkTempDay() = runBlocking {
        repo.saveSettings(
            Settings(
                onboarded = true,
                useWeather = true,
                lat = 41.0,
                lon = 29.0,
                tempC = null,
                tempDay = null,
            ),
        )
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        shadowOf(cm).setDefaultNetworkActive(false)
        shadowOf(cm).setActiveNetworkInfo(null)
        shadowOf(cm).clearAllNetworks()

        val reminders = Reminders(ctx, repo)
        val extra = reminders.refreshWeather(ZoneId.of("UTC"))
        assertNull(extra)
        assertNull(repo.settingsNow().tempDay)
    }

    @Test
    fun onMorningAndRescheduleWorkOfflineAndRecordDailyGoal() = runBlocking {
        val zone = ZoneId.of("UTC")
        repo.saveSettings(
            Settings(
                weightKg = 70,
                onboarded = true,
                useWeather = true,
                lat = 41.0,
                lon = 29.0,
            ),
        )
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        shadowOf(cm).clearAllNetworks()

        val reminders = Reminders(ctx, repo)
        reminders.onMorning(zone)

        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        assertNotNull(shadowOf(am).nextScheduledAlarm)

        val todayStr = LocalDate.now(zone).toString()
        val goals = db.intake().allGoals()
        assertTrue(goals.any { it.day == todayStr && it.goalMl == repo.todayNow().goalMl })
        assertEquals(null, repo.settingsNow().tempDay)
    }
}
