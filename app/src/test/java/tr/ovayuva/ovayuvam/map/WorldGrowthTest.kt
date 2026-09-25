package tr.ovayuva.ovayuvam.map

import org.junit.Assert.*
import org.junit.Test
import tr.ovayuva.ovayuvam.domain.*
import java.time.LocalDateTime
import java.time.ZoneId

class WorldGrowthTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private fun time(month: Int, day: Int): Long = LocalDateTime.of(2026,month,day,12,0).atZone(zone).toInstant().toEpochMilli()
    private fun reveal(x: Int, first: Long) = RevealCell(x,0,RevealCell.Kind.Core,first,first+10,8)

    @Test fun firstDiscoveriesAppearInOrderWithoutChangingSavedCells() {
        val source = mutableListOf(reveal(1,100),reveal(2,200),reveal(3,300))
        val growth = WorldGrowth(emptyList(),source,400)
        val range = growth.range(GrowthPeriod.All,zone)
        assertEquals(99L,range.startMs)
        assertTrue(growth.at(range.startMs).reveal.isEmpty())
        assertEquals(listOf(1),growth.at(100).reveal.map { it.x })
        assertEquals(listOf(1,2),growth.at(250).reveal.map { it.x })
        assertEquals(source,growth.at(range.endMs).reveal)
        source.clear()
        assertEquals(3,growth.at(400).reveal.size)
    }

    @Test fun weekKeepsOlderDiscoveriesAsItsStartingWorld() {
        val growth = WorldGrowth(emptyList(),listOf(reveal(1,time(1,1)),reveal(2,time(9,20))),time(9,23))
        val range = growth.range(GrowthPeriod.Week,zone)
        assertEquals(time(9,16),range.startMs)
        assertEquals(listOf(1),growth.at(range.startMs).reveal.map { it.x })
        assertEquals(2,growth.at(range.endMs).reveal.size)
        assertEquals(time(8,23),growth.range(GrowthPeriod.Month,zone).startMs)
        assertEquals(time(1,1)-1,growth.range(GrowthPeriod.Year,zone).startMs)
    }

    @Test fun oldWorldsAndUnknownDatesAreNotInventedAsNewDiscoveries() {
        val old = listOf(VisitedCell(1,2,100,200,50))
        val legacy = WorldGrowth(old,emptyList(),300)
        assertEquals(old,legacy.at(300).cells)
        assertTrue(legacy.at(99).cells.isEmpty())
        val growth = WorldGrowth(old,listOf(reveal(9,0),reveal(10,500)),300)
        assertFalse(growth.hasHistory)
        assertEquals(listOf(9),growth.at(300).reveal.map { it.x })
        assertTrue(growth.at(300).cells.isEmpty())
        assertFalse(WorldGrowth(emptyList(),listOf(reveal(1,0)),300).hasHistory)
        assertFalse(WorldGrowth(emptyList(),emptyList(),300).hasHistory)
    }

    @Test fun preciseHistoryNeverSwitchesFromLegacyCellsDuringPlayback() {
        val growth = WorldGrowth(listOf(VisitedCell(1,2,100,200,50)),listOf(reveal(9,200)),300)
        assertTrue(growth.hasHistory)
        assertEquals(200L,growth.firstSeenMs)
        assertTrue(growth.at(150).cells.isEmpty())
        assertTrue(growth.at(150).reveal.isEmpty())
        assertEquals(listOf(9),growth.at(300).reveal.map { it.x })
        assertTrue(growth.at(300).cells.isEmpty())
    }

    @Test fun quietPeriodsKeepTheBaselineWithoutClaimingNewDiscoveries() {
        val growth = WorldGrowth(emptyList(),listOf(reveal(1,time(1,1))),time(9,23))
        assertFalse(growth.hasDiscoveries(growth.range(GrowthPeriod.Week,zone)))
        assertFalse(growth.hasDiscoveries(growth.range(GrowthPeriod.Month,zone)))
        assertTrue(growth.hasDiscoveries(growth.range(GrowthPeriod.All,zone)))
        assertEquals(1,growth.at(growth.range(GrowthPeriod.Week,zone).startMs).reveal.size)
    }

    @Test fun seekingClampsSafelyAndIncludesTheFinalDiscovery() {
        val range = GrowthRange(100,500)
        assertEquals(100L,range.timeAt(-1f))
        assertEquals(100L,range.timeAt(Float.NaN))
        assertEquals(300L,range.timeAt(.5f))
        assertEquals(500L,range.timeAt(2f))
        assertEquals(1L,GrowthRange(1,1).timeAt(.5f))
    }

    @Test fun cameraBoundsUseTheShortPathAcrossTheDateLine() {
        val bounds = WorldGrowth.boundsOf(listOf(GeoPosition(10.0,179.9),GeoPosition(11.0,-179.9)))!!
        assertEquals(.2,bounds.east-bounds.west,.00001)
        assertEquals(10.0,bounds.south,0.0)
        assertEquals(11.0,bounds.north,0.0)
        val ordinary = WorldGrowth.boundsOf(listOf(GeoPosition(40.0,-5.0),GeoPosition(41.0,20.0)))!!
        assertEquals(-5.0,ordinary.west,0.0)
        assertEquals(20.0,ordinary.east,0.0)
        val single = WorldGrowth.boundsOf(listOf(GeoPosition(40.0,29.0)))!!
        assertEquals(single.west,single.east,0.0)
        assertNull(WorldGrowth.boundsOf(emptyList()))
    }

    @Test fun newerSamplesDoNotDelayAnOldDiscovery() {
        val cell = reveal(1,100).copy(lastSeenMs=10000,samples=500)
        val growth = WorldGrowth(emptyList(),listOf(cell),10000)
        assertEquals(listOf(cell),growth.at(100).reveal)
    }

    @Test fun replayCameraStartsCloseAndExpandsOnlyWithVisibleDiscoveries() {
        val growth = WorldGrowth(emptyList(),listOf(reveal(10000,300),reveal(0,100),reveal(10,200)),400)
        assertNull(growth.at(99).bounds)
        val first = growth.at(100).bounds!!
        assertEquals(first,growth.initialBounds)
        assertEquals(first.west,first.east,0.0)
        val middle = growth.at(200).bounds!!
        val end = growth.at(400).bounds!!
        assertTrue(middle.east > first.east)
        assertTrue(end.east > middle.east + 1.0)
        assertEquals(first,growth.at(100).bounds) // Seeking back excludes future places again.
        assertEquals(end,growth.bounds)
    }

    @Test fun interiorDiscoveriesDoNotKeepMovingTheCamera() {
        val growth = WorldGrowth(emptyList(),listOf(reveal(0,100),reveal(100,100),reveal(50,200)),300)
        assertEquals(growth.at(100).bounds,growth.at(200).bounds)
        assertEquals(growth.at(100).bounds,growth.initialBounds)
        assertEquals(2,growth.at(100).reveal.size)
    }

    @Test fun legacyWorldStartsAtItsFirstPlaceDespiteLaterDistantTripsAndRevisits() {
        val saved = listOf(
            VisitedCell(10000,2,300,10000,500),
            VisitedCell(1,2,100,10000,500),
            VisitedCell(10,2,200,10000,500),
        )
        val growth = WorldGrowth(saved,emptyList(),10000)
        val first = growth.at(100)
        val middle = growth.at(200)
        val last = growth.at(10000)
        assertEquals(listOf(saved[1]),first.cells)
        assertEquals(first.bounds,growth.initialBounds)
        assertEquals(first.bounds!!.west,first.bounds.east,0.0)
        assertTrue(middle.bounds!!.east > first.bounds.east)
        assertTrue(last.bounds!!.east > middle.bounds.east + 1.0)
        assertEquals(first,growth.at(100))
        assertEquals(saved,last.cells)
    }

    @Test fun largeDatedHistoryDoesNotLeakItsFinalExtentIntoTheOpeningFrame() {
        val saved = (0 until 10000).map { reveal(it,it.toLong()+100) }
        val growth = WorldGrowth(emptyList(),saved,20000)
        val first = growth.at(growth.firstSeenMs!!)
        assertEquals(listOf(saved.first()),first.reveal)
        assertEquals(first.bounds,growth.initialBounds)
        assertEquals(first.bounds!!.west,first.bounds.east,0.0)
        assertTrue(growth.bounds!!.east > first.bounds.east + 1.0)
        assertEquals(saved,growth.at(20000).reveal)
    }

    @Test fun periodCameraIncludesItsBaselineButNotLaterTrips() {
        val growth = WorldGrowth(emptyList(),listOf(reveal(0,time(1,1)),reveal(10,time(9,17)),
            reveal(10000,time(9,22))),time(9,23))
        val week = growth.range(GrowthPeriod.Week,zone)
        assertEquals(growth.initialBounds,growth.at(week.startMs).bounds)
        assertTrue(growth.at(time(9,17)).bounds!!.east < growth.at(week.endMs).bounds!!.east)
    }

    @Test fun replayCameraUsesLegacyAndUndatedBaselineWithoutInventingFutureBounds() {
        val legacy = WorldGrowth(listOf(VisitedCell(1,2,100,200,1)),emptyList(),300)
        assertNull(legacy.at(99).bounds)
        assertEquals(WorldCell(1,2).centerPosition().latitude,legacy.at(100).bounds!!.south,0.0)
        val growth = WorldGrowth(emptyList(),listOf(reveal(0,0),reveal(10,100),reveal(10000,500)),300)
        assertNotNull(growth.at(0).bounds)
        assertEquals(growth.bounds,growth.at(300).bounds)
        assertTrue(growth.bounds!!.east < .01)
    }

    @Test fun expandingCameraCrossesDateLineWithoutWorldWideJump() {
        fun point(lon: Double,time: Long): RevealCell {
            val cell = WorldCell.fromLocation(10.0,lon,WorldCell.RevealCellSizeMeters)
            return RevealCell(cell.x,cell.y,RevealCell.Kind.Core,time,time,1)
        }
        val growth = WorldGrowth(emptyList(),listOf(point(179.9,100),point(-179.9,200),point(-179.8,300)),400)
        val first = growth.at(100).bounds!!
        val second = growth.at(200).bounds!!
        val last = growth.at(300).bounds!!
        assertEquals(first.west,second.west,0.0)
        assertEquals(second.west,last.west,0.0)
        assertTrue(last.east-last.west < .31)
    }

    @Test fun cameraPaddingKeepsBrushesVisibleInGroundMetersAtHighLatitudes() {
        val equator = GrowthBounds(0.0,0.0,0.0,0.0).padded()
        val polar = GrowthBounds(80.0,0.0,80.0,0.0).padded()
        assertTrue(equator.east > 0)
        assertTrue(polar.east > equator.east*5)
        assertEquals(equator.north,polar.north-80.0,1e-10)
        assertTrue(GrowthBounds(85.051,0.0,85.051,0.0).padded().north <= 85.05112878)
    }
}
