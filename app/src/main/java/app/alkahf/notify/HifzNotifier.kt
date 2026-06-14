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
import app.alkahf.data.SabaqReference

/** Builds and posts the daily hifz reminder, including its "Listen" action. */
object HifzNotifier {
    private const val CHANNEL_ID = "hifz_reminders"
    private const val NOTIFICATION_ID = 4101

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val ctx = LocaleManager.apply(context)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                ctx.getString(R.string.notify_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = ctx.getString(R.string.notify_channel_desc) },
        )
    }

    fun showReminder(context: Context, sabaq: SabaqReference?) {
        ensureChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val ctx = LocaleManager.apply(context)

        val body = if (sabaq != null) {
            ctx.getString(R.string.notify_reminder_body, sabaq.surahNameLatin, sabaq.from, sabaq.to)
        } else {
            ctx.getString(R.string.notify_reminder_body_empty)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.notify_reminder_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent(context, play = false))

        // Listen only makes sense when there's a sabaq drill to play.
        if (sabaq != null) {
            builder.addAction(
                R.drawable.ic_notification,
                ctx.getString(R.string.notify_action_listen),
                openAppIntent(context, play = true),
            )
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked between the check and notify — nothing to do.
        }
    }

    private fun openAppIntent(context: Context, play: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (play) putExtra(MainActivity.EXTRA_PLAY_DRILL, true)
        }
        return PendingIntent.getActivity(
            context,
            if (play) 1 else 0, // distinct request codes so the two intents stay separate
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
