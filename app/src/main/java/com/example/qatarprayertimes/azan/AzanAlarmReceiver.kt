package com.example.qatarprayertimes.azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.qatarprayertimes.data.PrayerId
import com.example.qatarprayertimes.data.PrayerTimeCache
import com.example.qatarprayertimes.data.PrayerTimesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AzanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AzanScheduler.ACTION_REFRESH) {
            refreshTimes(context)
            return
        }

        val prayerId = intent.getStringExtra(AzanScheduler.EXTRA_PRAYER_ID)
            ?.let { runCatching { PrayerId.valueOf(it) }.getOrNull() } ?: return
        if (!AzanAudioStore(context).shouldPlay(prayerId)) return

        val play = Intent(context, AzanPlayerService::class.java)
            .putExtra(AzanScheduler.EXTRA_PRAYER_ID, prayerId.name)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(play)
            } else {
                context.startService(play)
            }
        }.onFailure { error ->
            Log.e(TAG, "Unable to start Azan playback for $prayerId", error)
        }

        PrayerTimeCache(context).readLatest()?.prayers?.find { it.id == prayerId }?.azan?.let { clock ->
            AzanScheduler.scheduleNext(context, prayerId, clock)
        }
    }

    private fun refreshTimes(context: Context) {
        val repo = PrayerTimesRepository(context)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { repo.load(forceRefresh = true) }.onSuccess { payload ->
                AzanScheduler.reschedule(context, payload.prayers)
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) return
        if (intent.action == android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED &&
            !ExactAlarmAccess.hasAccess(context)
        ) return
        val payload = PrayerTimeCache(context).readLatest() ?: return
        AzanScheduler.reschedule(context, payload.prayers)
    }
}

private const val TAG = "AzanAlarmReceiver"
