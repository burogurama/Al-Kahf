package app.alkahf

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import app.alkahf.audio.PlaybackService
import app.alkahf.data.QuranRepository
import app.alkahf.data.audio.AudioStore
import app.alkahf.notify.ReminderScheduler
import app.alkahf.ui.loop.LoopController
import app.alkahf.ui.mushaf.MushafAudioController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlkahfApplication : Application() {
    val repository: QuranRepository by lazy { QuranRepository(this) }

    private val audioStore: AudioStore by lazy { AudioStore(this) }

    /**
     * The one ExoPlayer for the whole app. Both playback controllers share it, so
     * starting one stops the other (only one playback at a time — correct). It is
     * app-owned and outlives any composition or screen, so backgrounding the app
     * (screen off) doesn't tear it down; [PlaybackService] keeps the hosting
     * process alive in the foreground while it plays. Built lazily on the main
     * thread (ExoPlayer requirement) the first time playback is wired up.
     */
    val playbackPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .build()
            .also { player ->
                // Bring the foreground service up only once playback has actually
                // started. By then the player is playing and its MediaSession is
                // active, so the service can post the media notification and call
                // startForeground immediately — meeting the startForegroundService
                // deadline and the API-34 mediaPlayback FGS requirement. (Starting
                // it earlier, before the āyah audio finished downloading, missed the
                // 5-second window and crashed.)
                player.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            ContextCompat.startForegroundService(
                                this@AlkahfApplication,
                                Intent(this@AlkahfApplication, PlaybackService::class.java),
                            )
                        }
                    }
                })
            }
    }

    /**
     * The session wrapping [playbackPlayer] for the media notification, lock
     * screen and Bluetooth controls. Owned here (not by the service) so the
     * service can be re-created without rebuilding the session.
     */
    val playbackSession: MediaSession by lazy {
        MediaSession.Builder(this, playbackPlayer).build()
    }

    /**
     * Long-lived scope driving the playback sequencers. Not tied to any
     * composition, so an in-flight loop/drill survives screen disposal and
     * backgrounding; cancelled only by an explicit stop or process death.
     */
    val playbackScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    /**
     * App-singleton Mushaf listening engine. Survives composition/backgrounding,
     * so its StateFlow persists and a reopened Mushaf reconnects to ongoing
     * playback. Screens configure it via the existing setters (reciter, speed,
     * mode, scope).
     */
    val mushafAudio: MushafAudioController by lazy {
        MushafAudioController(
            context = this,
            repository = repository,
            audioStore = audioStore,
            player = playbackPlayer,
            coroutineScope = playbackScope,
            reciterPath = AudioStore.DEFAULT_RECITER,
            reciterName = "Ḥuṣarī",
            onPlaybackIdle = ::stopPlaybackServiceIfIdle,
        )
    }

    /** App-singleton drill engine, sharing the one player and scope. */
    val loopAudio: LoopController by lazy {
        LoopController(
            repository = repository,
            audioStore = audioStore,
            player = playbackPlayer,
            scope = playbackScope,
            context = this,
            onPlaybackIdle = ::stopPlaybackServiceIfIdle,
        )
    }

    /** Stops every active sequencer (used when the app is swiped away). */
    fun stopAllPlayback() {
        mushafAudio.stop()
        loopAudio.stop()
    }

    /**
     * Tears down the foreground service once neither sequencer is running.
     * Called when a session ends (naturally or via stop), so a brief silence
     * between āyāt or a recite-back gap never dismisses the notification.
     */
    fun stopPlaybackServiceIfIdle() {
        if (!mushafAudio.isActive && !loopAudio.isActive) {
            stopService(Intent(this, PlaybackService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Warm the sūrah-name cache off the main thread so the first screen can
        // resolve names synchronously (no first-frame flash of bare numbers).
        CoroutineScope(SupervisorJob()).launch { repository.preloadSurahNames() }
        // Re-derive the khatam reminder scheduling mirror from its entity (the
        // source of truth) in case the two drifted, then re-arm alarms from the
        // reconciled state — covers a prefs reset or a mirror that was never written.
        CoroutineScope(SupervisorJob()).launch {
            repository.reconcileKhatamReminder()
            ReminderScheduler.reschedule(this@AlkahfApplication)
        }
        // Drop finished Exercises sessions older than a day (the auto-delete rule).
        CoroutineScope(SupervisorJob()).launch { repository.pruneExerciseSessions() }
    }
}
