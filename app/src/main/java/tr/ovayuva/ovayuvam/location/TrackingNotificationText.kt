package tr.ovayuva.ovayuvam.location

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

data class TrackingNotificationStats(
    val todayRevealedSquareMeters: Int,
    val todayDistanceMeters: Float,
    val lastProgressMs: Long?,
    val nowMs: Long,
)

object TrackingNotificationText {
    private const val InactiveDays = 3L
    private const val DayMs = 24L * 60L * 60L * 1_000L
    private const val RotationMs = 90L * 60L * 1_000L

    fun text(stats: TrackingNotificationStats, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val daysSinceProgress = stats.lastProgressMs?.let { (stats.nowMs - it).coerceAtLeast(0L) / DayMs }
        if (daysSinceProgress != null && daysSinceProgress >= InactiveDays) {
            return "Let's expand your world map. Tap to reveal a new way."
        }
        val hour = LocalTime.ofInstant(Instant.ofEpochMilli(stats.nowMs), zoneId).hour
        if (hour >= 19 && stats.hasProgressToday) {
            return if (stats.todayRevealedSquareMeters >= MinimumAreaForNotificationSquareMeters) {
                "Today you revealed about ${formatSquareMeters(stats.todayRevealedSquareMeters)} of your map."
            } else if (stats.todayDistanceMeters >= 1_000f) {
                "Today you walked about ${formatDistance(stats.todayDistanceMeters)} through the fog."
            } else if (stats.todayDistanceMeters >= 100f) {
                "Today you walked about ${formatDistance(stats.todayDistanceMeters)} through the fog."
            } else {
                "A little more of your world is visible today."
            }
        }
        return when (((stats.nowMs / RotationMs) % 3L).toInt()) {
            0 -> "Your walked places clear the fog. Tap to open the map."
            1 -> "Every new street makes your world a little larger."
            else -> "Walk a new way and reveal more of the map."
        }
    }

    private fun formatDistance(meters: Float): String =
        if (meters >= 1_000f) {
            "${oneDecimal(meters.toDouble() / 1_000.0)} km"
        } else {
            "${meters.roundToInt()} m"
        }

    private fun oneDecimal(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }

    private fun formatSquareMeters(squareMeters: Int): String {
        val rounded = if (squareMeters >= 1_000) {
            ((squareMeters + 50) / 100) * 100
        } else {
            squareMeters
        }
        return "%,d m²".format(Locale.US, rounded)
    }

    private val TrackingNotificationStats.hasProgressToday: Boolean
        get() = todayRevealedSquareMeters > 0 || todayDistanceMeters > 0f

    private const val MinimumAreaForNotificationSquareMeters = 400
}
