package tr.ovayuva.ovayuvam.domain

import tr.ovayuva.ovayuvam.location.ExplorePolicy
import java.util.UUID
import kotlin.math.ceil

data class PlannedPath(val id: String = UUID.randomUUID().toString(),
    val createdMs: Long = System.currentTimeMillis(), val cells: List<WorldCell>) {
    init {
        require(id.length in 1..80 && createdMs >= 0 && cells.size <= MaxCells)
        require(cells.all { it.x in -1_001_877..1_001_877 && it.y in -1_001_877..1_001_877 })
    }

    companion object { const val MaxCells = 50_000 }
}

object PathBrush {
    // Ground-distance sampling keeps a planned route the same width at every zoom.
    fun segment(start: GeoPosition, end: GeoPosition): List<WorldCell> {
        val distance = ExplorePolicy.distance(start, end)
        if (!distance.isFinite() || distance > 2_000) return emptyList()
        val steps = ceil(distance / 8).toInt().coerceIn(1, 256)
        val longitudeDelta = ((end.longitude - start.longitude + 540) % 360) - 180
        return (0..steps).map { step ->
            val fraction = step.toDouble() / steps
            val longitude = ((start.longitude + longitudeDelta * fraction + 540) % 360) - 180
            WorldCell.fromLocation(start.latitude + (end.latitude - start.latitude) * fraction,
                longitude, WorldCell.RevealCellSizeMeters)
        }.distinct()
    }
}
