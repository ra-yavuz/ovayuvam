package tr.ovayuva.ovayuvam.map

import tr.ovayuva.ovayuvam.domain.GeoPosition
import tr.ovayuva.ovayuvam.domain.RevealCell
import tr.ovayuva.ovayuvam.domain.VisitedCell
import tr.ovayuva.ovayuvam.domain.WorldCell
import java.time.Instant
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.round

enum class GrowthPeriod(val label: String) { Week("Week"), Month("Month"), Year("Year"), All("All") }

data class GrowthFrame(val cells: List<VisitedCell>, val reveal: List<RevealCell>, val bounds: GrowthBounds? = null)
data class GrowthBounds(val south: Double, val west: Double, val north: Double, val east: Double) {
    fun padded(meters: Double = 35.0): GrowthBounds {
        val latitudeMargin = Math.toDegrees(meters / MercatorPoint.EarthRadius)
        val longitudeMargin = latitudeMargin / cos(Math.toRadians(maxOf(kotlin.math.abs(south), kotlin.math.abs(north))))
        return GrowthBounds((south - latitudeMargin).coerceAtLeast(-85.05112878),
            west - longitudeMargin, (north + latitudeMargin).coerceAtMost(85.05112878), east + longitudeMargin)
    }
}

data class GrowthRange(val startMs: Long, val endMs: Long) {
    fun timeAt(fraction: Float): Long {
        val amount = if (fraction.isFinite()) fraction.coerceIn(0f, 1f).toDouble() else 0.0
        return (startMs + (endMs - startMs).toDouble() * amount).toLong().coerceIn(startMs, endMs)
    }
}

/** Reconstructs first discoveries, not the exact route or historical revisit intensity. */
class WorldGrowth(cells: List<VisitedCell>, reveal: List<RevealCell>, val capturedAtMs: Long) {
    private val reveal = reveal.filter { it.firstSeenMs in 0..capturedAtMs }
    // Keep the same representation throughout replay so legacy cells cannot disappear mid-frame.
    private val cells = if (this.reveal.isEmpty()) cells.filter { it.firstSeenMs in 0..capturedAtMs } else emptyList()
    val firstSeenMs: Long? = (this.cells.asSequence().map { it.firstSeenMs } +
        this.reveal.asSequence().map { it.firstSeenMs }).filter { it > 0 }.minOrNull()
    val hasHistory: Boolean get() = firstSeenMs != null

    // Built once on the history-loading worker. Camera ticks only do a binary search.
    private val cameraSteps: List<Pair<Long, GrowthBounds>> = buildList {
        val discoveries = if (this@WorldGrowth.reveal.isNotEmpty()) this@WorldGrowth.reveal.map {
            it.firstSeenMs to WorldCell(it.x, it.y).centerPosition(WorldCell.RevealCellSizeMeters)
        } else this@WorldGrowth.cells.map { it.firstSeenMs to WorldCell(it.x, it.y).centerPosition() }
        var previous: GrowthBounds? = null
        for ((time, position) in discoveries.sortedBy { it.first }) {
            val old = previous
            // Use the closest world copy so crossing the date line does not zoom out across the globe.
            val longitude = if (old == null) position.longitude else position.longitude +
                360.0 * round(((old.west + old.east) / 2 - position.longitude) / 360.0)
            val next = if (old == null) GrowthBounds(position.latitude, longitude, position.latitude, longitude)
            else GrowthBounds(minOf(old.south, position.latitude), minOf(old.west, longitude),
                maxOf(old.north, position.latitude), maxOf(old.east, longitude))
            if (next != old) {
                if (lastOrNull()?.first == time) removeAt(lastIndex)
                add(time to next)
                previous = next
            }
        }
    }

    val initialBounds: GrowthBounds? get() = boundsAt(firstSeenMs ?: 0)

    private fun boundsAt(timeMs: Long): GrowthBounds? {
        var low = 0
        var high = cameraSteps.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (cameraSteps[middle].first <= timeMs) low = middle + 1 else high = middle
        }
        return cameraSteps.getOrNull(low - 1)?.second
    }

    fun hasDiscoveries(range: GrowthRange): Boolean = cells.any {
        it.firstSeenMs > range.startMs && it.firstSeenMs <= range.endMs
    } || reveal.any { it.firstSeenMs > range.startMs && it.firstSeenMs <= range.endMs }

    fun range(period: GrowthPeriod, zone: ZoneId = ZoneId.systemDefault()): GrowthRange {
        val first = (firstSeenMs ?: capturedAtMs).coerceAtLeast(1) - 1
        val now = Instant.ofEpochMilli(capturedAtMs).atZone(zone)
        val start = when (period) {
            GrowthPeriod.Week -> now.minusWeeks(1).toInstant().toEpochMilli()
            GrowthPeriod.Month -> now.minusMonths(1).toInstant().toEpochMilli()
            GrowthPeriod.Year -> now.minusYears(1).toInstant().toEpochMilli()
            GrowthPeriod.All -> first
        }
        return GrowthRange(maxOf(first, start).coerceAtMost(capturedAtMs), capturedAtMs)
    }

    fun at(timeMs: Long): GrowthFrame = GrowthFrame(
        cells.filter { it.firstSeenMs <= timeMs },
        reveal.filter { it.firstSeenMs <= timeMs },
        boundsAt(timeMs),
    )

    val bounds: GrowthBounds? get() = cameraSteps.lastOrNull()?.second

    companion object {
        fun boundsOf(positions: List<GeoPosition>): GrowthBounds? {
            if (positions.isEmpty()) return null
            val longitudes = positions.map { (it.longitude + 360.0) % 360.0 }.distinct().sorted()
            // The complement of the largest gap is the smallest interval, including across the date line.
            val gapIndex = longitudes.indices.maxBy { index ->
                (if (index == longitudes.lastIndex) longitudes.first() + 360.0 else longitudes[index + 1]) - longitudes[index]
            }
            val start = longitudes[(gapIndex + 1) % longitudes.size]
            val span = 360.0 - ((if (gapIndex == longitudes.lastIndex) longitudes.first() + 360.0
                else longitudes[gapIndex + 1]) - longitudes[gapIndex])
            val west = if (start > 180.0) start - 360.0 else start
            return GrowthBounds(positions.minOf { it.latitude }, west, positions.maxOf { it.latitude }, west + span)
        }
    }
}
