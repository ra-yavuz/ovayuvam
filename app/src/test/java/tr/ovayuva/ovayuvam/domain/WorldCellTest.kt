package tr.ovayuva.ovayuvam.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WorldCellTest {
    @Test
    fun sameLocationMapsToSameCell() {
        val first = WorldCell.fromLocation(41.0082, 28.9784)
        val second = WorldCell.fromLocation(41.0082, 28.9784)

        assertEquals(first, second)
    }

    @Test
    fun nearbyButFarEnoughLocationChangesCell() {
        val first = WorldCell.fromLocation(41.0082, 28.9784, cellSizeMeters = 50.0)
        val second = WorldCell.fromLocation(41.0092, 28.9784, cellSizeMeters = 50.0)

        assertNotEquals(first, second)
    }

    @Test
    fun extremeLatitudeIsClamped() {
        val north = WorldCell.fromLocation(95.0, 0.0)
        val legalNorth = WorldCell.fromLocation(85.05112878, 0.0)

        assertEquals(legalNorth, north)
    }
}

