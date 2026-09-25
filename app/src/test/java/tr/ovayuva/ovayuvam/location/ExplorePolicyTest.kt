package tr.ovayuva.ovayuvam.location

import org.junit.Assert.*
import org.junit.Test
import tr.ovayuva.ovayuvam.domain.GeoPosition

class ExplorePolicyTest {
    private val home = GeoPosition(0.0, 0.0)
    private fun east(meters: Double) = GeoPosition(0.0, meters / 111195.0)
    @Test fun selectsNearFrontierNotRemoteCity() {
        assertEquals(east(50.0), ExplorePolicy.select(home, home, listOf(east(10000.0), east(50.0)), listOf(home to 26.0), null))
    }
    @Test fun neverMarksRevealedOrFarBeyondFrontier() {
        assertNull(ExplorePolicy.select(home, home, listOf(east(20.0), east(450.0)), listOf(home to 26.0), null))
    }
    @Test fun respectsCurrentPositionAndAnchor() {
        assertNull(ExplorePolicy.select(home, east(1000.0), listOf(east(50.0)), listOf(home to 26.0), null))
        assertNull(ExplorePolicy.select(east(1000.0), home, listOf(east(50.0)), listOf(home to 26.0), null))
    }
    @Test fun avoidsLastSuggestionAndEmptyHistory() {
        assertNull(ExplorePolicy.select(home, home, listOf(east(50.0)), listOf(home to 26.0), east(50.0)))
        assertNull(ExplorePolicy.select(home, home, listOf(east(50.0)), emptyList(), null))
    }
    @Test fun expiresWithoutIssuingMoreThanOneAWeek() {
        val issued = 100000L
        assertFalse(ExplorePolicy.due(issued + ExplorePolicy.LifetimeMs, issued))
        assertTrue(ExplorePolicy.due(issued + ExplorePolicy.WeekMs, issued))
        assertFalse(ExplorePolicy.valid(east(50.0), home, issued, issued + ExplorePolicy.LifetimeMs))
        assertFalse(ExplorePolicy.valid(east(501.0), home, issued, issued + 1))
        assertFalse(ExplorePolicy.due(issued - 1, issued))
    }
    @Test fun filtersMotorwaysPrivateAndUnknownPaths() {
        assertTrue(ExplorePolicy.walkable(mapOf("class" to "minor")))
        assertTrue(ExplorePolicy.walkable(mapOf("class" to "minor", "subclass" to "residential")))
        assertTrue(ExplorePolicy.walkable(mapOf("class" to "path", "subclass" to "footway")))
        for (access in listOf("no", "private", "customers")) {
            assertFalse(ExplorePolicy.walkable(mapOf("class" to "minor", "subclass" to "residential", "access" to access)))
        }
        assertFalse(ExplorePolicy.walkable(mapOf("class" to "motorway")))
        assertFalse(ExplorePolicy.walkable(mapOf("class" to "path", "subclass" to "cycleway")))
        assertFalse(ExplorePolicy.walkable(emptyMap()))
    }
    @Test fun distanceWrapsDateLine() {
        assertTrue(ExplorePolicy.distance(GeoPosition(0.0, 179.999), GeoPosition(0.0, -179.999)) < 230)
    }
}
