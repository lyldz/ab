package com.lyldz.ab.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.lyldz.ab.MainActivity
import com.lyldz.ab.R
import com.lyldz.ab.ui.UNIT_ML
import com.lyldz.ab.ui.fmtVol

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

    /** Android 13+ POST_NOTIFICATIONS izni ve sistem bildirim anahtarı birlikte açık mı? */
    fun hasPermission(ctx: Context): Boolean {
        val runtimeOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return runtimeOk && NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    /**
     * Android bir bildirimde en fazla 3 düğme gösterir: önerilen miktar (plan yoksa bardak), 500 (ilki 500 ise 1000) ve
     * uygulamayı açmadan miktar yazılan "Özel" satır içi giriş.
     */
    @SuppressLint("MissingPermission") // hasPermission() Android 13+ izni ve bildirim anahtarını kapsar
    fun show(
        ctx: Context,
        title: String,
        text: String,
        drankMl: Int,
        goalMl: Int,
        firstMl: Int,
        quiet: Boolean = false,
        secondMl: Int = 500,
        unit: Int = UNIT_ML,
    ) {
        if (!hasPermission(ctx)) return
        val lctx = com.lyldz.ab.ui.AppLocale.wrapContext(ctx)
        val nm = NotificationManagerCompat.from(ctx)

        val open = openMainPendingIntent(ctx)
        val second = if (firstMl == secondMl) (if (secondMl == 1000) 500 else 1000) else secondMl
        val custom = RemoteInput.Builder(KEY_TEXT).setLabel(lctx.getString(R.string.notif_custom_hint)).build()
        val customPi = PendingIntent.getBroadcast(
            ctx, 3, addIntent(ctx), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE, // RemoteInput mutable ister
        )
        val safeGoal = goalMl.coerceAtLeast(1)

        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_ab)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(quiet) // güncellemede ses/titreşim tekrar etmez
            .setProgress(safeGoal, drankMl.coerceIn(0, safeGoal), false)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(addAction(ctx, 1, firstMl, unit))
            .addAction(addAction(ctx, 2, second, unit))
            .addAction(
                NotificationCompat.Action.Builder(0, lctx.getString(R.string.notif_custom), customPi)
                    .addRemoteInput(custom)
                    .build(),
            )
            .build()
        nm.notify(NOTIF_ID, n)
    }

    /** Sabah sıcaklığı hedefi yükselttiyse bir kez haber verir. */
    @SuppressLint("MissingPermission") // hasPermission() Android 13+ izni ve bildirim anahtarını kapsar
    fun showWeather(ctx: Context, tempC: Int, goalMl: Int, extraMl: Int, unit: Int = UNIT_ML) {
        if (!hasPermission(ctx)) return
        val lctx = com.lyldz.ab.ui.AppLocale.wrapContext(ctx)
        val nm = NotificationManagerCompat.from(ctx)
        val open = openMainPendingIntent(ctx)
        nm.notify(
            GOAL_NOTIF_ID,
            NotificationCompat.Builder(ctx, GOAL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_ab)
                .setContentTitle(lctx.getString(R.string.weather_notif_title, tempC.toString()))
                .setContentText(lctx.getString(R.string.weather_notif_text_vol, fmtVol(goalMl, unit), fmtVol(extraMl, unit)))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    /** Aktif hatırlatma bildirimini kaldırır (ör. hatırlatmalar kapatıldığında). */
    fun cancel(ctx: Context) {
        runCatching { NotificationManagerCompat.from(ctx).cancel(NOTIF_ID) }
    }

    /** Ayarlar'dan tek dokunuşla taze, sesli test bildirimi gönderir; izin yoksa false döner. */
    fun showTest(ctx: Context): Boolean {
        if (!hasPermission(ctx)) return false
        val lctx = com.lyldz.ab.ui.AppLocale.wrapContext(ctx)
        cancel(ctx)
        show(
            ctx = ctx,
            title = lctx.getString(R.string.notif_test_title),
            text = lctx.getString(R.string.notif_test_text),
            drankMl = 0,
            goalMl = 2000,
            firstMl = 200,
            quiet = false,
        )
        return true
    }

    /** Bildirim şu an gösteriliyor mu (kaydırılıp kapatılmadıysa). */
    fun isShowing(ctx: Context): Boolean = runCatching {
        ctx.getSystemService(NotificationManager::class.java)?.activeNotifications?.any { it.id == NOTIF_ID } == true
    }.getOrDefault(false)

    private fun openMainPendingIntent(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx,
        0,
        Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun addIntent(ctx: Context) =
        Intent(ctx, ReminderReceiver::class.java).setAction(ACTION_ADD)

    private fun addAction(ctx: Context, code: Int, ml: Int, unit: Int = UNIT_ML): NotificationCompat.Action {
        val pi = PendingIntent.getBroadcast(
            ctx, code, addIntent(ctx).putExtra(EXTRA_ML, ml),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(0, "+${fmtVol(ml, unit)}", pi).build()
    }
}
