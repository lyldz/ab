package com.lyldz.ab.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.lyldz.ab.AbApp
import com.lyldz.ab.data.SOURCE_NOTIFICATION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private fun BroadcastReceiver.async(context: Context, block: suspend (AbApp) -> Unit) {
    val app = context.applicationContext as? AbApp ?: return
    val result = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
        try {
            block(app)
        } catch (e: Exception) {
            Log.e("ab", "receiver failed", e) // arka planda çökme yerine günlüğe yaz
        } finally {
            result.finish()
        }
    }
}

/** Alarm ve bildirim düğmeleri; dışarıdan erişilemez. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FIRE -> {
                val notify = intent.getBooleanExtra(EXTRA_NOTIFY, true)
                async(context) { it.reminders.onFire(notify) }
            }
            ACTION_MORNING -> async(context) { it.reminders.onMorning() }
            ACTION_ADD -> {
                val ml = intent.getIntExtra(EXTRA_ML, 0).takeIf { it > 0 }
                    ?: RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_TEXT)
                        ?.filter(Char::isDigit)?.toString()?.toIntOrNull()
                async(context) { app ->
                    if (ml != null && ml in 1..5000) {
                        app.addWater(ml, SOURCE_NOTIFICATION)
                    } else {
                        app.reminders.reschedule()
                        app.reminders.refreshNotification()
                    }
                }
            }
        }
    }
}

/** Yeniden başlatma, gece yarısı gün değişimi, saat/saat dilimi değişimi ve uygulama güncellemesinde alarmı ve widget'ı çevrimdışı yeniden kurar. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action in SUPPORTED_SYSTEM_ACTIONS) {
            async(context) { app ->
                app.reminders.reschedule()
                app.reminders.refreshNotification()
            }
        }
    }

    companion object {
        val SUPPORTED_SYSTEM_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
