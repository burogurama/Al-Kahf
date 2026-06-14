package app.alkahf.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.alkahf.LocaleManager
import app.alkahf.MainActivity
import app.alkahf.R
import app.alkahf.data.KhatamPortionRef

/** Builds and posts the daily khatam reminder, including its "Start reading" action. */
object KhatamNotifier {
    private const val CHANNEL_ID = "khatam_reminders"
    private const val NOTIFICATION_ID = 4102

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val ctx = LocaleManager.apply(context)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                ctx.getString(R.string.khatam_notify_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = ctx.getString(R.string.khatam_notify_channel_desc) },
        )
    }

    fun showReminder(context: Context, portion: KhatamPortionRef) {
        ensureChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val ctx = LocaleManager.apply(context)

        val body = ctx.getString(
            R.string.khatam_notify_body,
            portion.juz,
            portion.surahFromName,
            portion.ayahFrom,
            portion.surahToName,
            portion.ayahTo,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.khatam_notify_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openKhatamIntent(context, requestCode = 0))
            .addAction(
                R.drawable.ic_notification,
                ctx.getString(R.string.khatam_notify_action_read),
                openKhatamIntent(context, requestCode = 1),
            )

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked between the check and notify — nothing to do.
        }
    }

    private fun openKhatamIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_KHATAM, true)
        }
        return PendingIntent.getActivity(
            context,
            // Distinct request codes so the tap and action intents stay separate;
            // also distinct from HifzNotifier's 0/1 by virtue of the different action.
            200 + requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
