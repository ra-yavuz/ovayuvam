package tr.ovayuva.ovayuvam.map

import org.junit.Assert.*
import org.junit.Test

class FogTilesTest {
    @Test fun phoneResolutionsReuseTheSameGeographicKeys() {
        for ((width, height) in listOf(720 to 1280, 1080 to 2400, 1440 to 3200, 4000 to 8000)) {
            val view = FogViewport(MercatorPoint(260000.0, 6250000.0), 2.0, width, height)
            val level = FogTiles.level(view)
            val first = FogTiles.placements(view, level).map { it.key }.toSet()
            val next = FogTiles.placements(view.copy(center = view.center.copy(x = view.center.x + 1)), level).map { it.key }.toSet()
            assertEquals(level, FogTiles.level(view.copy(center = view.center.copy(x = view.center.x + 1))))
            assertTrue(first.intersect(next).size >= first.size - 8)
            assertTrue(first.size <= FogTiles.MaxVisibleTiles)
        }
    }

    @Test fun tileExtentsExactlyMeetAndParentsCoverFourChildren() {
        val key = FogTileKey(15, 12000, 14000)
        assertEquals(key.left + key.span, key.copy(x = key.x + 1).left, 1e-7)
        assertEquals(key.top - key.span, key.copy(y = key.y + 1).top, 1e-7)
        for (child in key.children()) assertEquals(key, child.parent())
        assertEquals(key.left, key.children().first().left, 1e-7)
    }

    @Test fun brushCrossesTileEdgesAndDateLineWithoutDuplicates() {
        val index = FogTileIndex()
        val mark = FogTileMark(FogMarkId(0, 0, false, false), 8, 0)
        index.add(mark)
        val keys = listOf(FogTileKey(14, 8191, 8191), FogTileKey(14, 8192, 8191),
            FogTileKey(14, 8191, 8192), FogTileKey(14, 8192, 8192))
        keys.forEach { assertEquals(listOf(mark), index.marks(it)) }
        assertEquals(listOf(mark), index.marks(FogTileKey(0, 0, 0)))
        index.remove(mark)
        assertFalse(index.hasData(FogTileKey(0, 0, 0)))

        val edge = FogTileMark(FogMarkId(1001875, 0, false, false), 8, 0)
        index.add(edge)
        assertEquals(listOf(edge), index.marks(FogTileKey(14, 0, 8191)))
        assertEquals(listOf(edge), index.marks(FogTileKey(14, 16383, 8191)))
    }

    @Test fun repeatedWorldCopiesShareOneImageKey() {
        val world = FogViewport(MercatorPoint(0.0, 0.0), MercatorPoint.WorldWidth / 256, 1024, 512)
        val placements = FogTiles.placements(world, 0)
        assertTrue(placements.size >= 4)
        assertEquals(setOf(FogTileKey(0, 0, 0)), placements.map { it.key }.toSet())
    }

    @Test fun sparseQueriesStopAtTheirBudget() {
        val index = FogTileIndex()
        repeat(1000) { index.add(FogTileMark(FogMarkId(it * 1000 - 500000, 0, false, false), 8, 0)) }
        assertEquals(65, index.marks(FogTileKey(0, 0, 0), 65).size)
        assertEquals(1000, index.marks(FogTileKey(0, 0, 0)).size)
    }

    @Test fun fineZoomQueriesOnlyTouchingBrushesAndUpdatesRemoveOldMarks() {
        val index = FogTileIndex()
        val mark = FogTileMark(FogMarkId(10, 10, false, false), 8, 0)
        val distant = mark.copy(id = mark.id.copy(x = 10000))
        index.add(mark)
        index.add(distant)
        val view = FogViewport(MercatorPoint(mark.mx, mark.my), 1.0, 256, 256)
        val keys = FogTiles.placements(view, 18).map { it.key }
        assertEquals(setOf(mark), keys.flatMap { index.marks(it) }.toSet())
        index.remove(mark)
        assertTrue(keys.flatMap { index.marks(it) }.isEmpty())
        assertEquals(listOf(distant), index.marks(FogTileKey(0, 0, 0)))
    }
}
