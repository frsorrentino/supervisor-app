package it.pixelbox.cmwatch.mobile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * La lettura vocale del tasto ▶ nel player di Android (Franz 03/11 16:07, app-player-lettura-03-11): una sessione multimediale
 * con titolo e fonte, in tendina e sulla schermata di blocco; play, pausa e stop arrivano anche da cuffie e orologio. Il
 * servizio in primo piano tiene viva la lettura a schermo spento e si chiude da solo quando la lettura finisce. Con la
 * modalità live accesa non parte: la voce e i tasti delle cuffie sono suoi.
 */
class ReadingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var media: MediaSession
    private lateinit var speech: Speech
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        speech = (application as PhoneApp).speech
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.reading_channel), NotificationManager.IMPORTANCE_LOW))
        media = MediaSession(this, "reading").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { speech.resume() }
                override fun onPause() { speech.pause() }
                override fun onStop() { speech.stop() }
            })
            isActive = true
        }
        startForeground(NOTE_ID, note(speech.speaking.value, speech.source.value, speech.paused.value), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        scope.launch {
            combine(speech.speaking, speech.source, speech.paused) { t, s, p -> Triple(t, s, p) }.collect { (text, source, paused) ->
                if (text == null) { stopSelf(); return@collect }
                show(text, source, paused)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> speech.pause()
            ACTION_PLAY -> speech.resume()
            ACTION_STOP -> speech.stop()
        }
        return START_NOT_STICKY
    }

    private fun show(text: String, source: String?, paused: Boolean) {
        media.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, it.pixelbox.cmwatch.rules.ReadingTitle.of(text))
                .putString(MediaMetadata.METADATA_KEY_ARTIST, source ?: getString(R.string.app_name))
                .build(),
        )
        media.setPlaybackState(
            PlaybackState.Builder().setState(if (paused) PlaybackState.STATE_PAUSED else PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .setActions(PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_STOP)
                .build(),
        )
        getSystemService(NotificationManager::class.java).notify(NOTE_ID, note(text, source, paused))
    }

    private fun note(text: String?, source: String?, paused: Boolean): Notification {
        fun action(act: String, icon: Int, label: Int) = Notification.Action.Builder(
            Icon.createWithResource(this, icon), getString(label),
            PendingIntent.getService(this, act.hashCode(), Intent(this, ReadingService::class.java).setAction(act), PendingIntent.FLAG_IMMUTABLE),
        ).build()
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(text?.let { t -> it.pixelbox.cmwatch.rules.ReadingTitle.of(t) } ?: getString(R.string.reading_now))
            .setContentText(source ?: getString(R.string.app_name))
            .setContentIntent(open).setOngoing(!paused).setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(if (paused) action(ACTION_PLAY, android.R.drawable.ic_media_play, R.string.reading_resume) else action(ACTION_PAUSE, android.R.drawable.ic_media_pause, R.string.reading_pause))
            .addAction(action(ACTION_STOP, android.R.drawable.ic_menu_close_clear_cancel, R.string.stop_reading))
            .setStyle(Notification.MediaStyle().setMediaSession(media.sessionToken).setShowActionsInCompactView(0, 1))
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        media.isActive = false
        media.release()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "reading"
        private const val NOTE_ID = 4207
        private const val ACTION_PAUSE = "it.pixelbox.cmwatch.reading.PAUSE"
        private const val ACTION_PLAY = "it.pixelbox.cmwatch.reading.PLAY"
        private const val ACTION_STOP = "it.pixelbox.cmwatch.reading.STOP"

        /** Parte quando comincia una lettura, se la modalità live non è accesa. */
        fun start(ctx: Context) {
            if (it.pixelbox.cmwatch.mobile.live.LiveService.running.value) return
            runCatching { ctx.startForegroundService(Intent(ctx, ReadingService::class.java)) }
        }
    }
}
