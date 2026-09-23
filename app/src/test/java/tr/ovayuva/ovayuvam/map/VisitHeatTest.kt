package tr.ovayuva.ovayuvam.map

import org.junit.Assert.*
import org.junit.Test

class VisitHeatTest {
    @Test fun tintStaysTranslucentAndDisappearsAtStreetLevel() {
        assertEquals(0.24f, VisitHeat.opacity(0.0), 0.0001f)
        assertEquals(0.12f, VisitHeat.opacity(11.0), 0.0001f)
        assertEquals(0f, VisitHeat.opacity(12.0), 0.0001f)
        assertEquals(0f, VisitHeat.opacity(22.0), 0.0001f)
    }
    @Test fun colorsUseStableVisitBands() {
        assertEquals(VisitHeat.color(2), VisitHeat.color(3))
        assertEquals(VisitHeat.color(4), VisitHeat.color(7))
        assertEquals(VisitHeat.color(8), VisitHeat.color(1000))
        assertNotEquals(VisitHeat.color(1), VisitHeat.color(8))
    }
}
