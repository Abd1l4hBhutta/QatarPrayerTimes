package com.example.qatarprayertimes.azan

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFocusRequest
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.qatarprayertimes.MainActivity
import com.example.qatarprayertimes.R
import com.example.qatarprayertimes.data.AppLocale
import com.example.qatarprayertimes.data.PrayerId
import com.example.qatarprayertimes.data.PreferencesStore
import com.example.qatarprayertimes.data.SoundType
import com.example.qatarprayertimes.ui.Strings
import java.io.File

class AzanPlayerService : Service() {
    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmVolumeBeforePlayback: Int? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val prayerId = intent?.getStringExtra(AzanScheduler.EXTRA_PRAYER_ID)
            ?.let { runCatching { PrayerId.valueOf(it) }.getOrNull() }
        
        if (prayerId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val store = AzanAudioStore(this)
        val state = store.state(prayerId)
        
        if (state.soundId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        acquireWakeLock()
        val channelId = createChannel()
        val notification = notification(prayerId, channelId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        boostAlarmVolume()
        
        if (state.type == SoundType.CUSTOM) {
            val file = File(File(filesDir, "azan"), state.soundId)
            if (file.exists()) play(file.absolutePath) else stopSelf()
        } else {
            val resId = resources.getIdentifier(state.soundId, "raw", packageName)
            if (resId != 0) play(resId) else stopSelf()
        }
        
        return START_NOT_STICKY
    }

    private fun play(path: String) {
        player?.release()
        val attributes = playerAttributes()
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        runCatching {
            mediaPlayer.setAudioAttributes(attributes)
            mediaPlayer.setWakeMode(this@AzanPlayerService, PowerManager.PARTIAL_WAKE_LOCK)
            mediaPlayer.setDataSource(path)
            mediaPlayer.prepare()
            mediaPlayer.startPlayer()
        }.onFailure {
            stopSelf()
        }
    }

    private fun play(resId: Int) {
        player?.release()
        val attributes = playerAttributes()
        val mediaPlayer = MediaPlayer.create(this, resId, attributes, audioManager().generateAudioSessionId())
        if (mediaPlayer == null) {
            stopSelf()
            return
        }
        player = mediaPlayer
        mediaPlayer.startPlayer()
    }

    private fun MediaPlayer.startPlayer() {
        isLooping = false
        setVolume(1f, 1f)
        setOnCompletionListener { stopSelf() }
        setOnErrorListener { _, _, _ ->
            stopSelf()
            true
        }
        start()
    }

    private fun playerAttributes() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun audioManager() = getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun boostAlarmVolume() {
        val audio = audioManager()
        if (alarmVolumeBeforePlayback == null) {
            alarmVolumeBeforePlayback = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        }
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (max > 0) {
            runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, max, 0) }
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(playerAttributes())
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                audio.requestAudioFocus(requireNotNull(audioFocusRequest))
            } else {
                @Suppress("DEPRECATION")
                audio.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
                )
            }
        }
    }

    private fun restoreAudioState() {
        val audio = audioManager()
        val originalAlarmVolume = alarmVolumeBeforePlayback
        alarmVolumeBeforePlayback = null
        if (originalAlarmVolume != null &&
            audio.getStreamVolume(AudioManager.STREAM_ALARM) ==
            audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        ) {
            runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, originalAlarmVolume, 0) }
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let(audio::abandonAudioFocusRequest)
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audio.abandonAudioFocus(audioFocusChangeListener)
            }
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "qatarprayertimes:azan").apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L)
        }
    }

    private fun notification(prayerId: PrayerId, channelId: String): Notification {
        val locale = PreferencesStore(this).read().locale
        val name = Strings.prayerName(locale, prayerId)
        val title = if (locale == AppLocale.AR) "أذان $name" else "Azan · $name"
        val stop = if (locale == AppLocale.AR) "إيقاف" else "Stop"
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, AzanPlayerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(stop)
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, stop, stopIntent)
            .build()
    }

    private fun createChannel(): String {
        val channelId = if (DndAccess.hasAccess(this)) CHANNEL_ID_DND else CHANNEL_ID_STANDARD
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelId
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, "Prayer alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Prayer-time alerts and Adhaan playback"
            setSound(null, null)
            enableVibration(false)
            if (DndAccess.hasAccess(this@AzanPlayerService)) {
                setBypassDnd(true)
            }
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
        return channelId
    }

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        player?.release()
        player = null
        restoreAudioState()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.example.qatarprayertimes.STOP_AZAN"
        // A new ID avoids retaining the immutable settings of the old channel.
        private const val CHANNEL_ID_STANDARD = "azan_playback_v2"
        private const val CHANNEL_ID_DND = "azan_playback_dnd_v2"
        private const val NOTIFICATION_ID = 41
    }
}
