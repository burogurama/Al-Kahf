package app.alkahf.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaStyleNotificationHelper
import app.alkahf.AlkahfApplication
import app.alkahf.MainActivity
import app.alkahf.R

/**
 * Foreground service that keeps Mushaf range-listening and drill playback alive
 * with the screen off. It is started (by [AlkahfApplication]'s player listener)
 * the moment playback actually begins — by then the app's ExoPlayer is playing
 * and its [androidx.media3.session.MediaSession] is active, so this can call
 * [startForeground] immediately (meeting the startForegroundService deadline and
 * the API-34 mediaPlayback FGS requirement) and post a MediaStyle notification.
 *
 * Keeping the process foreground stops the OS from killing it, so audio
 * continues and the in-memory navigation back stack survives a reopen. The
 * service stops once no sequencer is active (signalled by the controllers), so a
 * brief silence between āyāt or a recite-back gap does not dismiss it.
 *
 * The player and session are owned by [AlkahfApplication] and outlive the
 * service (it may be re-created), so neither is released here.
 */
class PlaybackService : Service() {

    private val app get() = application as AlkahfApplication

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Go foreground immediately so the startForegroundService deadline is met.
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 30) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away stops playback and the service.
        app.stopAllPlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val meta = app.playbackPlayer.mediaMetadata
        val title = meta.title?.toString()?.takeIf { it.isNotBlank() }
            ?: getString(R.string.playback_notification_default)
        val text = meta.artist?.toString().orEmpty()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(app.playbackSession))
            .build()
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.playback_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) },
        )
    }

    companion object {
        private const val CHANNEL_ID = "playback"
        private const val NOTIFICATION_ID = 4201
    }
}
