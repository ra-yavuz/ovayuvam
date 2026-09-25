package tr.ovayuva.ovayuvam.location

import tr.ovayuva.ovayuvam.domain.GeoPosition
import kotlin.math.*

internal object ExplorePolicy {
    const val WeekMs = 7 * 24 * 60 * 60 * 1000L
    const val LifetimeMs = 3 * 24 * 60 * 60 * 1000L
    const val RestMs = 60 * 60 * 1000L
    const val MaxGapMs = 5 * 60 * 1000L
    const val MaxDistance = 500.0

    fun distance(a: GeoPosition, b: GeoPosition): Double {
        val lat = Math.toRadians(b.latitude - a.latitude)
        val lon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(lat / 2).pow(2) + cos(Math.toRadians(a.latitude)) *
            cos(Math.toRadians(b.latitude)) * sin(lon / 2).pow(2)
        return 6_371_000 * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun due(now: Long, last: Long): Boolean = last == 0L || now - last >= WeekMs

    fun valid(target: GeoPosition, current: GeoPosition, created: Long, now: Long): Boolean =
        now >= created && now - created < LifetimeMs && distance(target, current) <= MaxDistance

    fun walkable(tags: Map<String, String>): Boolean {
        if (tags["access"] in setOf("no", "private", "restricted", "customers", "permit") ||
            tags["foot"] in setOf("no", "private") || tags["indoor"] == "1" || tags["indoor"] == "yes" ||
            tags["brunnel"] == "tunnel") return false
        return when (tags["class"]) {
            // OpenMapTiles normally omits subclass for minor streets.
            "minor" -> tags["subclass"].orEmpty() in setOf("", "residential", "living_street", "unclassified")
            "path" -> tags["subclass"] in setOf("footway", "pedestrian", "path", "steps")
            else -> false
        }
    }

    /** Nearest unrevealed candidate just beyond existing brush edges, never an arbitrary fallback. */
    fun select(anchor: GeoPosition, current: GeoPosition, candidates: List<GeoPosition>,
               revealed: List<Pair<GeoPosition, Double>>, previous: GeoPosition?): GeoPosition? =
        candidates.asSequence()
            .filter { distance(it, anchor) <= MaxDistance && distance(it, current) in 35.0..MaxDistance }
            .filter { previous == null || distance(it, previous) > 35.0 }
            .map { candidate -> candidate to (revealed.minOfOrNull { (point, radius) ->
                distance(candidate, point) - radius
            } ?: Double.POSITIVE_INFINITY) }
            .filter { (_, edgeDistance) -> edgeDistance in 8.0..65.0 }
            .minByOrNull { (candidate, _) -> distance(candidate, current) }?.first
}
