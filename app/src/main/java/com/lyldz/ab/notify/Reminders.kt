package com.lyldz.ab.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.lyldz.ab.core.Plan
import com.lyldz.ab.core.nextPlan
import com.lyldz.ab.data.AbRepository
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

    suspend fun reschedule(zone: ZoneId = ZoneId.systemDefault()) {
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

    suspend fun onFire(notify: Boolean, zone: ZoneId = ZoneId.systemDefault()) {
        if (notify) {
            val now = ZonedDateTime.now(zone)
            val (t, plan) = planNow(now)
            // alarm erken/geç gelebilir; yalnızca zamanı gelmişse göster
            if (plan != null && plan.nextAtMin <= now.hour * 60 + now.minute) {
                Notifier.show(context, plan.amountMl, t.drankMl, t.goalMl, t.settings.cupMl)
                repo.markNotified(System.currentTimeMillis())
            }
        }
        reschedule(zone)
    }
}
