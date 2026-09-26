package tr.ovayuva.ovayuvam.map

import org.junit.Assert.*
import org.junit.Test
import tr.ovayuva.ovayuvam.domain.*
import tr.ovayuva.ovayuvam.backup.*
import tr.ovayuva.ovayuvam.location.ExplorePolicy

class PlanningTest {
    @Test fun fogCannotFullyRevealEvenRepeatedlyPaintedPlans() {
        for (value in listOf(-1f, 0f, 0.55f, 0.82f, 1f, Float.NaN)) {
            val opacity = FogAppearance.opacity(value)
            assertTrue(opacity in 0.55f..0.96f)
            assertTrue(opacity * (1 - FogAppearance.PlanErasure) >= 0.32f)
        }
    }

    @Test fun pathBrushFillsGapsAndRejectsAccidentalWorldSpanningStrokes() {
        val start = GeoPosition(41.013, 28.982)
        val end = GeoPosition(41.015, 28.982)
        val cells = PathBrush.segment(start, end)
        assertTrue(cells.size > 10)
        assertTrue(cells.zipWithNext().all { (a,b) -> ExplorePolicy.distance(a.centerPosition(20.0), b.centerPosition(20.0)) < 25 })
        assertTrue(PathBrush.segment(start, GeoPosition(42.0, 30.0)).isEmpty())
    }

    @Test fun paintAcrossDateLineStaysAtDateLine() {
        assertTrue(PathBrush.segment(GeoPosition(0.0,179.999), GeoPosition(0.0,-179.999))
            .all { kotlin.math.abs(it.centerPosition(20.0).longitude) > 179.99 })
    }

    @Test fun plannedRoutesRoundTripWithoutInventingDiscoveries() {
        val path = PlannedPath("test-path", 123, listOf(WorldCell(20,30), WorldCell(21,30)))
        val world = WorldBackup(emptyList(), emptyList(), null, 123, plannedPaths = listOf(path))
        val restored = WorldBackupCodec.decrypt(WorldBackupCodec.encrypt(world, "test-password"), "test-password")
        assertEquals(world, restored)
        assertTrue(restored.revealCells.isEmpty() && restored.visitedCells.isEmpty() && restored.visitCounts.isEmpty())
    }

    @Test fun legacyBackupsDoNotNeedPlannedPaths() {
        val world = WorldBackupCodec.fromPlainJson("""{"format":"ovayuvam.world.v1","exportedMs":1,"visitedCells":[],"revealCells":[],"goal":null}""")
        assertTrue(world.plannedPaths.isEmpty())
    }
}
