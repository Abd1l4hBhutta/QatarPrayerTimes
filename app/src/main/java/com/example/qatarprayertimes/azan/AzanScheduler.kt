package com.example.qatarprayertimes.azan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.qatarprayertimes.MainActivity
import com.example.qatarprayertimes.data.PrayerId
import com.example.qatarprayertimes.data.PrayerTime
import com.example.qatarprayertimes.data.TimeUtils
import java.util.Calendar

object AzanScheduler {
    const val EXTRA_PRAYER_ID = "prayer_id"
    const val ACTION_REFRESH = "com.example.qatarprayertimes.REFRESH_TIMES"

    fun reschedule(context: Context, prayers: List<PrayerTime>) {
        val store = AzanAudioStore(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Schedule Prayer Alarms
        PrayerId.entries.forEach { id ->
            cancel(context, alarmManager, id)
            val prayer = prayers.find { it.id == id } ?: return@forEach
            if (store.shouldPlay(id)) {
                scheduleNext(context, alarmManager, id, prayer.azan)
            }
        }

        // Schedule Midnight Refresh
        scheduleMidnightRefresh(context, alarmManager)
    }

    private fun scheduleMidnightRefresh(context: Context, alarmManager: AlarmManager) {
        val intent = Intent(context, AzanAlarmReceiver::class.java).setAction(ACTION_REFRESH)
        val pending = PendingIntent.getBroadcast(
            context, 999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val cal = Calendar.getInstance(TimeUtils.QATAR)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 5) // 5 seconds past midnight
        cal.set(Calendar.MILLISECOND, 0)
        
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        scheduleWakeupAlarm(context, alarmManager, cal.timeInMillis, pending)
    }

    fun scheduleNext(context: Context, prayerId: PrayerId, clock: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduleNext(context, alarmManager, prayerId, clock)
    }

    private fun scheduleNext(
        context: Context,
        alarmManager: AlarmManager,
        prayerId: PrayerId,
        clock: String,
    ) {
        val show = PendingIntent.getActivity(
            context,
            200 + prayerId.ordinal,
            Intent(context, MainActivity::class.java),
            pendingFlags(),
        )
        val triggerAtMillis = nextTriggerMillis(clock)
        try {
            if (ExactAlarmAccess.hasAccess(context)) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, show),
                    alarmIntent(context, prayerId),
                )
            } else {
                // The user has not yet allowed exact alarms. Keep a best-effort alarm
                // scheduled rather than silently dropping this prayer notification.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    alarmIntent(context, prayerId),
                )
            }
        } catch (e: SecurityException) {
            android.util.Log.e("AzanScheduler", "SecurityException while scheduling exact alarm", e)
            // Best effort if permission was revoked between check and call
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                alarmIntent(context, prayerId),
            )
        }
    }

    private fun scheduleWakeupAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
    ) {
        try {
            if (ExactAlarmAccess.hasAccess(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        } catch (e: SecurityException) {
            android.util.Log.e("AzanScheduler", "SecurityException while scheduling wakeup alarm", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    private fun cancel(context: Context, alarmManager: AlarmManager, prayerId: PrayerId) {
        alarmManager.cancel(alarmIntent(context, prayerId))
    }

    private fun alarmIntent(context: Context, prayerId: PrayerId): PendingIntent {
        val intent = Intent(context, AzanAlarmReceiver::class.java)
            .putExtra(EXTRA_PRAYER_ID, prayerId.name)
        return PendingIntent.getBroadcast(context, 100 + prayerId.ordinal, intent, pendingFlags())
    }

    private fun pendingFlags(): Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun nextTriggerMillis(clock: String): Long {
        val minutes = TimeUtils.clockToMinutes(clock)
        val cal = Calendar.getInstance(TimeUtils.QATAR)
        cal.set(Calendar.HOUR_OF_DAY, minutes / 60)
        cal.set(Calendar.MINUTE, minutes % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis() + 100) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
