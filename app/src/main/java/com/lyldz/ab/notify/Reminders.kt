package com.lyldz.ab.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.lyldz.ab.R
import com.lyldz.ab.core.Plan
import com.lyldz.ab.core.nextPlan
import com.lyldz.ab.core.paceMl
import com.lyldz.ab.core.roundTo10
import com.lyldz.ab.core.tempExtraMl
import androidx.glance.appwidget.updateAll
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.data.WeatherApi
import com.lyldz.ab.tile.AbTileService
import com.lyldz.ab.ui.fmtTime
import com.lyldz.ab.ui.fmtVol
import com.lyldz.ab.ui.fmtVolNum
import com.lyldz.ab.widget.AbWidget
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

/**
 * Tek bir alarm tutar: sıradaki hatırlatma ya da (plan yoksa) yarının rutin başı.
 * shortcut: inexact setAndAllowWhileIdle, Doze'da dakikalar kayabilir; "tam zamanında"
 * ayarı (SCHEDULE_EXACT_ALARM) istenirse eklenir.
 */
class Reminders(private val context: Context, private val repo: AbRepository) {
    private val alarm = context.getSystemService(AlarmManager::class.java)

    private fun pending(notify: Boolean): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_NOTIFY, notify),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun morningPending(): PendingIntent = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_MORNING),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /**
     * Android 12+'ta opsiyonel SCHEDULE_EXACT_ALARM izni verilmiş ve kullanıcı ayarı açıksa tam zamanında
     * alarm kurar; izin yoksa veya kapalıysa inexact setAndAllowWhileIdle'a düşer.
     */
    private fun scheduleAt(triggerMs: Long, pi: PendingIntent, preferExact: Boolean = true) {
        val safeMs = maxOf(triggerMs, System.currentTimeMillis() + 5_000L)
        val canExact = preferExact && (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                runCatching { alarm.canScheduleExactAlarms() }.getOrDefault(false)
            )
        if (canExact) {
            runCatching { alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeMs, pi) }
                .onFailure { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeMs, pi) }
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeMs, pi)
        }
    }

    /** Her gün rutin başında bir kez: sıcaklığı al. Sıradaki rutin başına kurulur. */
    private fun scheduleMorning(startMin: Int, now: ZonedDateTime, preferExact: Boolean) {
        val startToday = now.toLocalDate().atStartOfDay(now.zone).plusMinutes(startMin.toLong())
        val at = if (now.isBefore(startToday)) startToday else startToday.plusDays(1)
        scheduleAt(at.toInstant().toEpochMilli(), morningPending(), preferExact)
    }

    /** Çevrimdışı durumda hava durumu isteğini zaman aşımı beklemeden atlamak için aktif ağ kontrolü. */
    private fun isNetworkAvailable(): Boolean = runCatching {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(true)

    /**
     * Günde bir kez bugünün en yüksek sıcaklığını çeker (gün içinde tekrar çekmez). Çekildiyse
     * hava ekini (ml) döndürür; ayar kapalı, yer yok, bugün zaten alınmış ya da ağ yoksa null.
     * Başarısız olursa işaretlenmez; sonraki açılış ya da sabah alarmı yeniden dener.
     */
    suspend fun refreshWeather(zone: ZoneId = ZoneId.systemDefault()): Int? {
        val s = repo.settingsNow()
        val lat = s.lat
        val lon = s.lon
        val today = LocalDate.now(zone).toString()
        if (!s.useWeather || lat == null || lon == null || s.tempDay == today) return null
        if (!isNetworkAvailable()) return null
        val t = kotlinx.coroutines.withTimeoutOrNull(6_000) {
            runCatching { WeatherApi.fetchMaxTemp(lat, lon) }.getOrNull()
        } ?: return null
        repo.saveTemperature(t, today)
        return tempExtraMl(t)
    }

    suspend fun onMorning(zone: ZoneId = ZoneId.systemDefault()) {
        val extra = refreshWeather(zone)
        val s = repo.settingsNow()
        val temp = s.tempC
        if (extra != null && extra > 0 && s.onboarded && s.weatherNotificationEnabled && s.goalOverrideMl == null && temp != null) {
            Notifier.showWeather(context, temp.roundToInt(), repo.todayNow().goalMl, extra, s.unit)
        }
        reschedule(zone)
    }

    private suspend fun planNow(now: ZonedDateTime): Pair<AbRepository.Today, Plan?> {
        val t = repo.todayNow()
        val s = t.settings
        val plan = if (!s.onboarded || !s.remindersEnabled) null else nextPlan(
            now.hour * 60 + now.minute, s.windowStartMin, s.windowEndMin,
            t.goalMl, t.drankMl, s.reminderIntervalMin(), t.anchorMin,
        )
        return t to plan
    }

    /** Alarmı kurar ve widget ile Hızlı Ayarlar kutucuğunu tazeler; her veri değişiminden sonra bu çağrılır. */
    suspend fun reschedule(zone: ZoneId = ZoneId.systemDefault()) {
        scheduleAlarm(zone)
        runCatching { AbWidget().updateAll(context) }
        AbTileService.requestRefresh(context)
    }

    private suspend fun scheduleAlarm(zone: ZoneId) {
        val now = ZonedDateTime.now(zone)
        val (t, plan) = planNow(now)
        if (!t.settings.onboarded) {
            alarm.cancel(pending(true))
            alarm.cancel(morningPending())
            return
        }
        scheduleMorning(t.settings.windowStartMin, now, t.settings.preferExactAlarm)
        repo.recordGoal(now.toLocalDate(), t.goalMl)
        if (!t.settings.remindersEnabled) {
            alarm.cancel(pending(true))
            Notifier.cancel(context)
            return
        }
        if (!t.settings.statusNotificationEnabled) {
            Notifier.cancel(context)
        }
        val day = now.toLocalDate().atStartOfDay(zone)
        if (plan != null) {
            scheduleAt(day.plusMinutes(plan.nextAtMin.toLong()).toInstant().toEpochMilli(), pending(true), t.settings.preferExactAlarm)
        } else {
            // hedef doldu ya da pencere bitti: yarın rutin başında ACTION_MORNING yeniden hesaplar
            alarm.cancel(pending(true))
        }
    }

    /**
     * Bildirimden, uygulamadan ya da widget'tan kayıt sonrası: bildirim hâlâ duruyorsa yerinde
     * kalır, ilerlemesi ve sıradaki hatırlatma saati güncellenir (yeniden ses çalmaz).
     */
    suspend fun refreshNotification(zone: ZoneId = ZoneId.systemDefault(), force: Boolean = false) {
        if (!force && !Notifier.isShowing(context)) return
        val now = ZonedDateTime.now(zone)
        val (t, plan) = planNow(now)
        if (!t.settings.onboarded || !t.settings.remindersEnabled || !t.settings.statusNotificationEnabled) {
            Notifier.cancel(context)
            return
        }
        val nowMin = now.hour * 60 + now.minute
        val inWindow = nowMin in t.settings.windowStartMin..t.settings.windowEndMin
        val unit = t.settings.unit
        val progress = "${fmtVolNum(t.drankMl, unit)} / ${fmtVol(t.goalMl, unit)}"
        val done = t.drankMl >= t.goalMl
        val lctx = com.lyldz.ab.ui.AppLocale.wrapContext(context)
        val title = when {
            done -> lctx.getString(R.string.notif_title_done)
            !inWindow -> lctx.getString(R.string.notif_title_idle)
            else -> paceTitle(t, nowMin, lctx)
        }
        val text = when {
            done -> progress
            plan != null -> lctx.getString(R.string.notif_text_after_vol, progress, fmtTime(plan.nextAtMin))
            else -> progress
        }
        Notifier.show(
            context, title, text, t.drankMl, t.goalMl, plan?.amountMl ?: t.settings.cupMl,
            quiet = true, secondMl = t.settings.notifSecondActionMl, unit = unit,
        )
    }

    /** Bildirim başlığı: tempoya göre "X geridesin / ilerdesin / tam tempodasın" (ana ekranla aynı kural). */
    private fun paceTitle(t: AbRepository.Today, nowMin: Int, lctx: Context = com.lyldz.ab.ui.AppLocale.wrapContext(context)): String {
        val s = t.settings
        val diff = paceMl(t.goalMl, s.windowStartMin, s.windowEndMin, nowMin) - t.drankMl
        val tol = s.paceToleranceMl
        return when {
            diff >= tol && tol >= 0 && diff > 0 -> lctx.getString(R.string.pace_behind_vol, fmtVol(roundTo10(diff), s.unit))
            diff <= -tol && tol >= 0 && diff < 0 -> lctx.getString(R.string.pace_ahead_vol, fmtVol(roundTo10(-diff), s.unit))
            else -> lctx.getString(R.string.pace_on)
        }
    }

    suspend fun onFire(notify: Boolean, zone: ZoneId = ZoneId.systemDefault()) {
        if (notify) {
            val now = ZonedDateTime.now(zone)
            val (t, plan) = planNow(now)
            // alarm erken/geç gelebilir; yalnızca zamanı gelmişse göster
            // 1 dk tolerans: saat ayarı ya da alarm kayması yüzünden dakika erken denk gelirse bildirim yutulmasın
            if (plan != null && plan.nextAtMin <= now.hour * 60 + now.minute + 1) {
                val lctx = com.lyldz.ab.ui.AppLocale.wrapContext(context)
                val u = t.settings.unit
                val progress = "${fmtVolNum(t.drankMl, u)} / ${fmtVol(t.goalMl, u)}"
                val text = lctx.getString(R.string.notif_text_vol, fmtVol(plan.amountMl, u), progress)
                // önceki sessiz durum bildirimini kaldır ki yeni hatırlatma taze ses/titreşimle gelsin
                Notifier.cancel(context)
                Notifier.show(
                    context, paceTitle(t, now.hour * 60 + now.minute, lctx), text, t.drankMl, t.goalMl, plan.amountMl,
                    quiet = false, secondMl = t.settings.notifSecondActionMl, unit = u,
                )
                repo.markNotified(System.currentTimeMillis())
            }
        }
        reschedule(zone)
    }
}
