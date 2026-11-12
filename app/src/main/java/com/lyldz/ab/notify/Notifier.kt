package com.lyldz.ab.notify

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.lyldz.ab.MainActivity
import com.lyldz.ab.R
import com.lyldz.ab.ui.fmtMl

const val CHANNEL_ID = "reminders"
const val NOTIF_ID = 1
const val ACTION_ADD = "com.lyldz.ab.ADD"
const val ACTION_FIRE = "com.lyldz.ab.FIRE"
const val ACTION_MORNING = "com.lyldz.ab.MORNING"
const val GOAL_CHANNEL_ID = "goal_updates"
const val GOAL_NOTIF_ID = 2
const val EXTRA_ML = "ml"
const val EXTRA_NOTIFY = "notify"
const val KEY_TEXT = "ml_text"

object Notifier {
    fun createChannel(ctx: Context) {
        NotificationManagerCompat.from(ctx).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(ctx.getString(R.string.channel_name))
                .build(),
        )
        NotificationManagerCompat.from(ctx).createNotificationChannel(
            NotificationChannelCompat.Builder(GOAL_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(ctx.getString(R.string.goal_channel_name))
                .build(),
        )
    }

    /**
     * Android bir bildirimde en fazla 3 düğme gösterir: bardak, 500 (bardak 500 ise 1000) ve
     * uygulamayı açmadan miktar yazılan "Özel" satır içi giriş.
     */
    @SuppressLint("MissingPermission") // areNotificationsEnabled() izni kapsar
    fun show(ctx: Context, text: String, drankMl: Int, goalMl: Int, cupMl: Int, quiet: Boolean = false) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return

        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val second = if (cupMl == 500) 1000 else 500
        val custom = RemoteInput.Builder(KEY_TEXT).setLabel(ctx.getString(R.string.notif_custom_hint)).build()
        val customPi = PendingIntent.getBroadcast(
            ctx, 3, addIntent(ctx), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE, // RemoteInput mutable ister
        )

        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_ab)
            .setContentTitle(ctx.getString(R.string.notif_title))
            .setContentText(text)
            .setOnlyAlertOnce(quiet) // güncellemede ses/titreşim tekrar etmez
            .setProgress(goalMl, drankMl.coerceAtMost(goalMl), false)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(addAction(ctx, 1, cupMl))
            .addAction(addAction(ctx, 2, second))
            .addAction(
                NotificationCompat.Action.Builder(0, ctx.getString(R.string.notif_custom), customPi)
                    .addRemoteInput(custom)
                    .build(),
            )
            .build()
        nm.notify(NOTIF_ID, n)
    }

    /** Sabah sıcaklığı hedefi yükselttiyse bir kez haber verir. */
    @SuppressLint("MissingPermission") // areNotificationsEnabled() izni kapsar
    fun showWeather(ctx: Context, tempC: Int, goalMl: Int, extraMl: Int) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(
            GOAL_NOTIF_ID,
            NotificationCompat.Builder(ctx, GOAL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_ab)
                .setContentTitle(ctx.getString(R.string.weather_notif_title, tempC.toString()))
                .setContentText(ctx.getString(R.string.weather_notif_text, fmtMl(goalMl), fmtMl(extraMl)))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    /** Bildirim şu an gösteriliyor mu (kaydırılıp kapatılmadıysa). */
    fun isShowing(ctx: Context): Boolean =
        ctx.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == NOTIF_ID }

    private fun addIntent(ctx: Context) =
        Intent(ctx, ReminderReceiver::class.java).setAction(ACTION_ADD)

    private fun addAction(ctx: Context, code: Int, ml: Int): NotificationCompat.Action {
        val pi = PendingIntent.getBroadcast(
            ctx, code, addIntent(ctx).putExtra(EXTRA_ML, ml),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(0, ctx.getString(R.string.add_ml, fmtMl(ml)), pi).build()
    }
}
