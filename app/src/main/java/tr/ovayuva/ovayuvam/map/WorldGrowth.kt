package tr.ovayuva.ovayuvam.map

import tr.ovayuva.ovayuvam.domain.GeoPosition
import tr.ovayuva.ovayuvam.domain.RevealCell
import tr.ovayuva.ovayuvam.domain.VisitedCell
import tr.ovayuva.ovayuvam.domain.WorldCell
import java.time.Instant
import java.time.ZoneId

enum class GrowthPeriod(val label: String) { Week("Week"), Month("Month"), Year("Year"), All("All") }

data class GrowthFrame(val cells: List<VisitedCell>, val reveal: List<RevealCell>)
data class GrowthBounds(val south: Double, val west: Double, val north: Double, val east: Double)

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
    )

    val bounds: GrowthBounds? = boundsOf(
        if (this.reveal.isNotEmpty()) this.reveal.map { WorldCell(it.x, it.y).centerPosition(WorldCell.RevealCellSizeMeters) }
        else this.cells.map { WorldCell(it.x, it.y).centerPosition() },
    )

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
