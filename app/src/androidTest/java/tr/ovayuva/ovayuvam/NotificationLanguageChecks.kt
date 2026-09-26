package tr.ovayuva.ovayuvam

import android.app.Instrumentation
import android.app.LocaleManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.res.Configuration
import android.os.LocaleList
import tr.ovayuva.ovayuvam.location.*
import java.util.Locale

/** Requires granted location/notification permissions and device location enabled in the test harness. */
fun Instrumentation.checkNotificationLanguages() {
    val context = targetContext
    check(context.packageName.endsWith(".verification"))
    val locales = context.getSystemService(LocaleManager::class.java)
    val original = locales.applicationLocales
    val manager = context.getSystemService(NotificationManager::class.java)
    TrackingState(context).acceptDisclosure()
    TrackingState(context).setEnabled(true)
    ExploreState(context).enabled = false
    val activity = startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    try {
        for (language in listOf("en", "de", "tr", "ru", "es", "fr")) {
            runOnMainSync { locales.applicationLocales = LocaleList.forLanguageTags(language) }
            val local = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
            })
            val expected = local.getString(R.string.tracking_notification_title)
            val deadline = System.currentTimeMillis() + 30_000
            var notification: Notification? = null
            while (System.currentTimeMillis() < deadline) {
                notification = manager.activeNotifications.firstOrNull { it.id == TrackingNotification.Id }?.notification
                if (notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() == expected &&
                    manager.getNotificationChannel(TrackingNotification.ChannelId).name.toString() == local.getString(R.string.tracking_channel_name)) break
                Thread.sleep(250)
            }
            val active = checkNotNull(notification) { "Tracking notification missing for $language" }
            check(active.extras.getCharSequence(Notification.EXTRA_TITLE).toString() == expected) { "Stale title for $language" }
            check(active.actions.single().title.toString() == local.getString(R.string.pause_tracking)) { "Stale action for $language" }
            val text = active.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
            val messages = listOf(R.array.notification_everyday, R.array.notification_progress, R.array.notification_quiet)
                .flatMap { local.resources.getStringArray(it).toList() }
            check(messages.any { text.endsWith(it) }) { "Stale notification body for $language: $text" }
            check(manager.getNotificationChannel(TrackingNotification.ChannelId).name.toString() == local.getString(R.string.tracking_channel_name)) {
                "Stale channel for $language: ${manager.getNotificationChannel(TrackingNotification.ChannelId).name}, expected ${local.getString(R.string.tracking_channel_name)}"
            }
            check(active.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
        }
    } finally {
        LocationTrailService.pause(context)
        runOnMainSync { locales.applicationLocales = original; activity.finish() }
    }
}
