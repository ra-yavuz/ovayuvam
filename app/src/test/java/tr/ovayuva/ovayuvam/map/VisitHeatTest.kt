package tr.ovayuva.ovayuvam.map

import org.junit.Assert.*
import org.junit.Test

class VisitHeatTest {
    @Test fun defaultAndSmallZoomOutHaveNoTintEvenForBusyPlaces() {
        for (zoom in listOf(22.0, 15.6, 15.0, 14.6, 14.5)) {
            assertEquals(0f, VisitHeat.opacity(zoom, Int.MAX_VALUE), 0f)
        }
    }
    @Test fun neighborhoodTintStrengthensWhenZoomingOut() {
        assertEquals(0.048f, VisitHeat.opacity(14.0, 100), 0.0001f)
        assertEquals(0.144f, VisitHeat.opacity(13.0, 100), 0.0001f)
        assertEquals(0.24f, VisitHeat.opacity(12.0, 100), 0.0001f)
        assertEquals(VisitHeat.opacity(12.0, 100), VisitHeat.opacity(0.0, 100), 0f)
        for (visits in listOf(1, 10, 100, 1000, Int.MAX_VALUE)) {
            for (step in 1..24) {
                assertTrue(VisitHeat.opacity(14.5-step/10.0, visits) < VisitHeat.opacity(14.4-step/10.0, visits))
            }
        }
    }
    @Test fun eachVisitGraduallyIncreasesIntensityWithoutEightVisitCeiling() {
        for (visits in 1..2000) {
            assertTrue(VisitHeat.opacity(12.0, visits+1) > VisitHeat.opacity(12.0, visits))
        }
        assertEquals(0.12f, VisitHeat.opacity(12.0, 1), 0.0001f)
        assertEquals(0.18f, VisitHeat.opacity(12.0, 10), 0.0001f)
        assertEquals(0.24f, VisitHeat.opacity(12.0, 100), 0.0001f)
        assertEquals(0.30f, VisitHeat.opacity(12.0, 1000), 0.0001f)
        assertTrue(VisitHeat.opacity(12.0, 10000) > VisitHeat.opacity(12.0, 1000))
        assertTrue(VisitHeat.opacity(12.0, Int.MAX_VALUE) < VisitHeat.MaxOpacity)
    }
    @Test fun colorsInterpolateInsteadOfUsingVisitBuckets() {
        assertNotEquals(VisitHeat.color(2), VisitHeat.color(3))
        assertNotEquals(VisitHeat.color(8), VisitHeat.color(9))
        assertNotEquals(VisitHeat.color(20), VisitHeat.color(100))
        assertNotEquals(VisitHeat.color(100), VisitHeat.color(365))
        assertNotEquals(VisitHeat.color(1000), VisitHeat.color(10000))
        assertEquals(0xFF66AFBD, VisitHeat.color(1))
        assertEquals(0xFF83B8A0, VisitHeat.color(10))
        assertEquals(0xFFDAAD4B, VisitHeat.color(100))
        assertEquals(0xFFE57970, VisitHeat.color(1000))
    }
    @Test fun unmeasuredPlacesAndInvalidZoomHaveNoTint() {
        assertEquals(0f, VisitHeat.opacity(0.0, 0), 0f)
        assertEquals(0f, VisitHeat.opacity(0.0, -1), 0f)
        assertEquals(0f, VisitHeat.opacity(Double.NaN, 10), 0f)
        assertEquals(0f, VisitHeat.opacity(Double.POSITIVE_INFINITY, 10), 0f)
    }
}
