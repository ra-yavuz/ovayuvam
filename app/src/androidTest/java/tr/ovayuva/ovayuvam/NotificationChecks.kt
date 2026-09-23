package tr.ovayuva.ovayuvam

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import tr.ovayuva.ovayuvam.location.TrackingNotification

fun checkQuietNotifications(context: Context) {
    check(context.packageName.endsWith(".verification"))
    val manager = context.getSystemService(NotificationManager::class.java)
    TrackingNotification.ensureChannel(context)
    val channel = checkNotNull(manager.getNotificationChannel(TrackingNotification.ChannelId))
    check(channel.importance == NotificationManager.IMPORTANCE_LOW)
    check(channel.sound == null && !channel.shouldVibrate())
    try {
        for (text in listOf("Your world is growing.", "A little more of the world is yours.")) {
            val notification = TrackingNotification.create(context, text)
            check(notification.sound == null && notification.vibrate == null)
            check(notification.defaults and (Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE) == 0)
            check(notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
            check(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
            manager.notify(TrackingNotification.Id, notification)
            Thread.sleep(1000)
            val active = manager.activeNotifications.filter { it.id == TrackingNotification.Id }
            check(active.size == 1)
            check(active.single().notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString() == text)
        }
    } finally {
        manager.cancel(TrackingNotification.Id)
    }
}
