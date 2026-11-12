package com.lyldz.ab.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.lyldz.ab.R
import com.lyldz.ab.core.Plan
import com.lyldz.ab.core.nextPlan
import com.lyldz.ab.core.tempExtraMl
import androidx.glance.appwidget.updateAll
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.data.WeatherApi
import com.lyldz.ab.ui.fmtMl
import com.lyldz.ab.ui.fmtTime
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

    /** Her gün rutin başında bir kez: sıcaklığı al. Sıradaki rutin başına kurulur. */
    private fun scheduleMorning(startMin: Int, now: ZonedDateTime) {
        val startToday = now.toLocalDate().atStartOfDay(now.zone).plusMinutes(startMin.toLong())
        val at = if (now.isBefore(startToday)) startToday else startToday.plusDays(1)
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), morningPending())
    }

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
        val t = WeatherApi.fetchMaxTemp(lat, lon) ?: return null
        repo.saveTemperature(t, today)
        return tempExtraMl(t)
    }

    suspend fun onMorning(zone: ZoneId = ZoneId.systemDefault()) {
        val extra = refreshWeather(zone)
        val s = repo.settingsNow()
        val temp = s.tempC
        if (extra != null && extra > 0 && s.onboarded && s.goalOverrideMl == null && temp != null) {
            Notifier.showWeather(context, temp.roundToInt(), repo.todayNow().goalMl, extra)
        }
        reschedule(zone)
    }

    private suspend fun planNow(now: ZonedDateTime): Pair<AbRepository.Today, Plan?> {
        val t = repo.todayNow()
        val s = t.settings
        val plan = if (!s.onboarded) null else nextPlan(
            now.hour * 60 + now.minute, s.windowStartMin, s.windowEndMin,
            t.goalMl, t.drankMl, s.cupMl, t.anchorMin,
        )
        return t to plan
    }

    /** Alarmı kurar ve widget'ı tazeler; her veri değişiminden sonra bu çağrılır. */
    suspend fun reschedule(zone: ZoneId = ZoneId.systemDefault()) {
        scheduleAlarm(zone)
        AbWidget().updateAll(context)
    }

    private suspend fun scheduleAlarm(zone: ZoneId) {
        val now = ZonedDateTime.now(zone)
        val (t, plan) = planNow(now)
        if (!t.settings.onboarded) {
            alarm.cancel(pending(true))
            alarm.cancel(morningPending())
            return
        }
        scheduleMorning(t.settings.windowStartMin, now)
        val day = now.toLocalDate().atStartOfDay(zone)
        val (at, notify) = if (plan != null) {
            day.plusMinutes(plan.nextAtMin.toLong()) to true
        } else {
            // hedef doldu ya da pencere bitti: yarın rutin başında yeniden hesapla, bildirim yok
            day.plusDays(1).plusMinutes(t.settings.windowStartMin.toLong()) to false
        }
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), pending(notify))
    }

    /**
     * Bildirimden, uygulamadan ya da widget'tan kayıt sonrası: bildirim hâlâ duruyorsa yerinde
     * kalır, ilerlemesi ve sıradaki hatırlatma saati güncellenir (yeniden ses çalmaz).
     */
    suspend fun refreshNotification(zone: ZoneId = ZoneId.systemDefault()) {
        if (!Notifier.isShowing(context)) return
        val (t, plan) = planNow(ZonedDateTime.now(zone))
        val drank = fmtMl(t.drankMl)
        val goal = fmtMl(t.goalMl)
        val text = when {
            t.drankMl >= t.goalMl -> context.getString(R.string.notif_text_done, drank, goal)
            plan != null -> context.getString(R.string.notif_text_after, drank, goal, fmtTime(plan.nextAtMin))
            else -> context.getString(R.string.home_progress, drank, goal)
        }
        Notifier.show(context, text, t.drankMl, t.goalMl, t.settings.cupMl, quiet = true)
    }

    suspend fun onFire(notify: Boolean, zone: ZoneId = ZoneId.systemDefault()) {
        if (notify) {
            val now = ZonedDateTime.now(zone)
            val (t, plan) = planNow(now)
            // alarm erken/geç gelebilir; yalnızca zamanı gelmişse göster
            if (plan != null && plan.nextAtMin <= now.hour * 60 + now.minute) {
                val text = context.getString(R.string.notif_text, fmtMl(plan.amountMl), fmtMl(t.drankMl), fmtMl(t.goalMl))
                Notifier.show(context, text, t.drankMl, t.goalMl, t.settings.cupMl)
                repo.markNotified(System.currentTimeMillis())
            }
        }
        reschedule(zone)
    }
}
