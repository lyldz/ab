package com.lyldz.ab.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.lyldz.ab.AbApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private fun BroadcastReceiver.async(context: Context, block: suspend (AbApp) -> Unit) {
    val result = goAsync()
    val app = context.applicationContext as AbApp
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
            ACTION_ADD -> {
                val ml = intent.getIntExtra(EXTRA_ML, 0).takeIf { it > 0 }
                    ?: RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_TEXT)
                        ?.filter(Char::isDigit)?.toString()?.toIntOrNull()
                async(context) { app ->
                    if (ml != null && ml in 1..5000) {
                        app.addWater(ml)
                    } else {
                        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
                        app.reminders.reschedule()
                    }
                }
            }
        }
    }
}

/** Yeniden başlatma, saat/saat dilimi değişimi ve uygulama güncellemesinde alarmı yeniden kurar. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        async(context) { it.reminders.reschedule() }
    }
}
