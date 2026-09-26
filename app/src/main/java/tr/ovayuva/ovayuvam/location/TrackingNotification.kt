package tr.ovayuva.ovayuvam.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import tr.ovayuva.ovayuvam.MainActivity
import tr.ovayuva.ovayuvam.R

object TrackingNotification {
    const val ChannelId = "location-tracking"
    const val Id = 1001

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(ChannelId, context.getString(R.string.tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun create(context: Context, text: String): Notification {
        val localized = tr.ovayuva.ovayuvam.ui.AppLanguage.wrap(context)
        val openIntent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_stat_map)
            .setContentTitle(localized.getString(R.string.tracking_notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, localized.getString(R.string.pause_tracking),
                PendingIntent.getService(context, 1,
                    Intent(context, LocationTrailService::class.java).setAction(LocationTrailService.ActionPause),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
    }
}
