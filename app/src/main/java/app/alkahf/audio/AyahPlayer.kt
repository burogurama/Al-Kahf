package app.alkahf.audio

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import kotlinx.coroutines.delay

/** Outcome of a single play call. */
enum class PlayResult { ENDED, STOPPED }

/**
 * Shared per-āyah playback primitive over an ExoPlayer. Each call starts
 * playback honouring [isPaused] and polls until the source ends (ENDED) or
 * playback is stopped (IDLE → STOPPED), invoking [onTick] with the current
 * position and duration on every poll so callers can drive highlighting. Runs
 * on the caller's dispatcher, which must be the main thread (ExoPlayer).
 */
object AyahPlayer {

    /** Plays a downloaded everyayah file from start to finish. */
    suspend fun playFile(
        player: ExoPlayer,
        file: File,
        speed: Float,
        isPaused: () -> Boolean,
        pollMs: Long = 120,
        onTick: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    ): PlayResult {
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.prepare()
        player.setPlaybackSpeed(speed)
        player.playWhenReady = !isPaused()
        while (true) {
            when (player.playbackState) {
                Player.STATE_ENDED -> return PlayResult.ENDED
                Player.STATE_IDLE -> return PlayResult.STOPPED
                else -> onTick(player.currentPosition, player.duration.coerceAtLeast(0))
            }
            delay(pollMs)
        }
    }

    /**
     * Plays a single āyah's [segment] of an imported reciter's combined file.
     * Set [prepare] to false when the media item is already loaded (e.g. playing
     * successive segments of the same file). Returns ENDED once the play head
     * reaches the segment end, STOPPED if playback was cut.
     */
    suspend fun playSegment(
        player: ExoPlayer,
        fileUri: String,
        segment: LongRange,
        speed: Float,
        isPaused: () -> Boolean,
        prepare: Boolean = true,
        pollMs: Long = 120,
        onTick: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    ): PlayResult {
        if (prepare) {
            player.setMediaItem(MediaItem.fromUri(Uri.parse(fileUri)))
            player.prepare()
        }
        player.setPlaybackSpeed(speed)
        player.seekTo(segment.first)
        player.playWhenReady = !isPaused()
        val total = (segment.last - segment.first).coerceAtLeast(0)
        while (true) {
            when (player.playbackState) {
                Player.STATE_IDLE -> return PlayResult.STOPPED
                Player.STATE_ENDED -> return PlayResult.ENDED
                else -> {
                    if (player.currentPosition >= segment.last) return PlayResult.ENDED
                    val position = (player.currentPosition - segment.first).coerceIn(0, total)
                    onTick(position, total)
                }
            }
            delay(pollMs)
        }
    }
}
