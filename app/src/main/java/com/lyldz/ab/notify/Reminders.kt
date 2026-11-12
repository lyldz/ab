package com.lyldz.ab.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.lyldz.ab.R
import com.lyldz.ab.core.Plan
import com.lyldz.ab.core.nextPlan
import androidx.glance.appwidget.updateAll
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.ui.fmtMl
import com.lyldz.ab.ui.fmtTime
import com.lyldz.ab.widget.AbWidget
import java.time.ZoneId
import java.time.ZonedDateTime

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
            return
        }
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
