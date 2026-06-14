package app.alkahf.data.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.alkahf.AlkahfApplication
import app.alkahf.R
import java.io.IOException

/**
 * Downloads a reciter's sūrah audio in the background as a data-sync foreground
 * service, so it survives leaving the screen, backgrounding the app, and process
 * death (WorkManager re-runs unfinished work; already-cached āyāt are skipped).
 * Progress is reported through [setProgressAsync] for the UI and a notification.
 */
class AudioDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        foregroundInfo(
            inputData.getString(KEY_RECITER_NAME).orEmpty(),
            done = 0,
            total = inputData.getIntArray(KEY_SURAHS)?.size ?: 0,
        )

    override suspend fun doWork(): Result {
        val reciterKey = inputData.getString(KEY_RECITER_KEY) ?: return Result.failure()
        val reciterName = inputData.getString(KEY_RECITER_NAME).orEmpty()
        val surahs = inputData.getIntArray(KEY_SURAHS) ?: return Result.success()
        if (surahs.isEmpty()) return Result.success()
        val repository = (applicationContext as AlkahfApplication).repository

        runCatching { setForeground(foregroundInfo(reciterName, 0, surahs.size)) }
        return try {
            surahs.forEachIndexed { index, surah ->
                repository.downloadSurah(reciterKey, surah) { fraction ->
                    setProgressAsync(
                        workDataOf(
                            KEY_PROGRESS_SURAH to surah,
                            KEY_PROGRESS_FRACTION to fraction,
                            KEY_PROGRESS_DONE to index,
                            KEY_PROGRESS_TOTAL to surahs.size,
                        ),
                    )
                }
                runCatching { setForeground(foregroundInfo(reciterName, index + 1, surahs.size)) }
            }
            Result.success()
        } catch (e: IOException) {
            // Flaky network: retry with backoff a few times (cached āyāt are kept,
            // so a retry resumes rather than restarts), then give up.
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private fun foregroundInfo(reciterName: String, done: Int, total: Int): ForegroundInfo {
        ensureChannel(applicationContext)
        val reciterKey = inputData.getString(KEY_RECITER_KEY).orEmpty()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.download_notification_title, reciterName))
            .setContentText(applicationContext.getString(R.string.download_notification_progress, done, total))
            .setProgress(total.coerceAtLeast(1), done, total == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        val id = NOTIFICATION_ID_BASE + (reciterKey.hashCode() and 0xFFF)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.download_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.download_channel_desc) },
        )
    }

    companion object {
        const val KEY_RECITER_KEY = "reciter_key"
        const val KEY_RECITER_NAME = "reciter_name"
        const val KEY_SURAHS = "surahs"
        const val KEY_PROGRESS_SURAH = "p_surah"
        const val KEY_PROGRESS_FRACTION = "p_fraction"
        const val KEY_PROGRESS_DONE = "p_done"
        const val KEY_PROGRESS_TOTAL = "p_total"

        private const val CHANNEL_ID = "audio_downloads"
        private const val NOTIFICATION_ID_BASE = 4202
        private const val MAX_ATTEMPTS = 5

        /** Per-reciter unique work, so a reciter has one download queue. */
        fun uniqueName(reciterKey: String) = "audio-download:$reciterKey"
    }
}
