package tr.ovayuva.ovayuvam.location

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt

data class TrackingNotificationStats(
    val todayRevealedSquareMeters: Long,
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
        if (hour >= 19 && stats.todayRevealedSquareMeters > 0L) {
            val area = formatArea(stats.todayRevealedSquareMeters)
            return if (stats.todayDistanceMeters >= 1_000f) {
                "Today you cleared about $area and walked ${formatDistance(stats.todayDistanceMeters)}."
            } else {
                "Today you cleared about $area of the map."
            }
        }
        return when (((stats.nowMs / RotationMs) % 3L).toInt()) {
            0 -> "Your walked places clear the fog. Tap to open the map."
            1 -> "Every new street makes your world a little larger."
            else -> "Walk a new way and reveal more of the map."
        }
    }

    private fun formatArea(squareMeters: Long): String =
        if (squareMeters >= 10_000L) {
            val hectares = squareMeters / 10_000.0
            "${oneDecimal(hectares)} ha"
        } else {
            "${squareMeters} m²"
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
}
