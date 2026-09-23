package tr.ovayuva.ovayuvam.location

import java.time.Instant
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
    const val RotationMs = 90L * 60L * 1_000L

    fun nextRotationDelay(nowMs: Long): Long = RotationMs - Math.floorMod(nowMs, RotationMs)

    private val everyday = listOf(
        "A little map, a whole world.",
        "Your corner of the world, drawn one day at a time.",
        "A few footsteps can make a lovely little line.",
        "There is room on this map for ordinary days.",
        "Your world does not have a deadline.",
        "A familiar street can still be a lovely street.",
        "Little by little, the blank spaces become stories.",
        "Some adventures fit between home and the bakery.",
        "Your map is a scrapbook without the glue.",
        "A small world can hold a lot of memories.",
        "Every map begins somewhere.",
        "No finish line. Just your own little world.",
        "The fog can wait. Enjoy the day.",
        "There is no wrong size for an adventure.",
        "A familiar corner, a different sky.",
        "Your map has room for the scenic route.",
        "Some of the best places are quite ordinary.",
        "A little curiosity goes a long way.",
        "Your world. No audience needed.",
        "The best pace is your own.",
        "A pocket-sized collection of places.",
        "Big maps are made of little moments.",
        "Even the everyday routes belong in your story.",
        "A map does not need to be finished to be beautiful.",
        "There is a whole world in the little things.",
        "The map can stay in your pocket for a while.",
        "No score to chase. Just places to remember.",
        "A little ink for the places that become yours.",
        "Some days are for exploring. Some are for tea.",
        "Your world has plenty of room to grow.",
        "A familiar path is a story worth keeping.",
        "Small outings count as outings.",
        "A quiet corner can be a fine discovery.",
        "The world is full of little in-between places.",
        "Your map is allowed to take its time.",
        "A little collection of here and there.",
        "Not every adventure needs a destination.",
        "There is something lovely about knowing a place.",
        "One map. All your everyday little journeys.",
        "The fog is patient.",
        "Your world is not a competition.",
        "The long way and the short way both belong.",
        "A small excuse to notice your surroundings.",
        "A map full of places, not obligations.",
        "A pocket atlas of your own making.",
        "Somewhere becomes familiar, little by little.",
        "No hurry. The streets will still be there.",
        "Your own little patchwork of places.",
        "There is room for both routine and surprise.",
        "Even a tiny map is entirely yours.",
        "A little more here, a little more there.",
        "Ordinary places make a wonderfully personal map.",
        "Wherever the day takes you, go gently.",
    )

    private val progress = listOf(
        "A few more footsteps in your little atlas.",
        "Your everyday journey has a place on the map.",
        "A little outing, a little story.",
        "Your world has a few more moments in it.",
        "Small steps make a lovely map.",
        "A bit of today, tucked into your world.",
        "Your map is collecting the ordinary magic.",
        "A little journey worth keeping.",
        "A few footsteps, entirely your own.",
        "A little chapter in your pocket atlas.",
        "Your familiar places have company today.",
        "A small trail through a very big world.",
        "A little more of your day belongs on the map.",
        "Your world is made of days like this.",
        "A few places, a few steps, a little story.",
        "Some journeys are small enough to fit into a lunch break.",
        "A little piece of the day, remembered.",
    )

    private val quiet = listOf(
        "Your world is right here whenever you return.",
        "No catching up needed. Your map is still yours.",
        "A quiet day is a perfectly good day.",
        "The fog can wait for another day.",
        "No lost streaks. No rush. Just your world.",
        "Your discoveries are staying right where you left them.",
        "Your little atlas keeps its pages.",
        "Rest belongs in a good adventure, too.",
        "There will be time for another little outing.",
        "Your map has no attendance sheet.",
        "All your places are still here.",
        "A pause does not undo a journey.",
        "A little world, ready whenever you are.",
    )

    private fun pick(messages: List<String>, nowMs: Long): String =
        messages[Math.floorMod(Math.floorDiv(nowMs, RotationMs), messages.size.toLong()).toInt()]

    fun text(stats: TrackingNotificationStats, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val daysSinceProgress = stats.lastProgressMs?.let { (stats.nowMs - it).coerceAtLeast(0L) / DayMs }
        if (daysSinceProgress != null && daysSinceProgress >= InactiveDays) {
            return pick(quiet, stats.nowMs)
        }
        val hour = Instant.ofEpochMilli(stats.nowMs).atZone(zoneId).hour
        if (hour >= 19 && stats.hasProgressToday) {
            val summary = if (stats.todayRevealedSquareMeters >= MinimumAreaForNotificationSquareMeters) {
                "Today you revealed about ${formatSquareMeters(stats.todayRevealedSquareMeters)} of your map."
            } else if (stats.todayDistanceMeters >= 1_000f) {
                "Today you walked about ${formatDistance(stats.todayDistanceMeters)} through the fog."
            } else if (stats.todayDistanceMeters >= 100f) {
                "Today you walked about ${formatDistance(stats.todayDistanceMeters)} through the fog."
            } else {
                "A little more of your world is visible today."
            }
            return "$summary\n${pick(everyday, stats.nowMs)}"
        }
        return pick(if (stats.hasProgressToday) progress else everyday, stats.nowMs)
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
