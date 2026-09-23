package tr.ovayuva.ovayuvam.location

import org.junit.Assert.*
import org.junit.Test
import tr.ovayuva.ovayuvam.domain.GeoPosition

class VisitDetectorTest {
    private val origin = GeoPosition(41.0, 29.0)
    private fun fix(seconds: Long, meters: Double = 0.0, accuracy: Double = 5.0, boot: Int = 1) =
        VisitFix(GeoPosition(origin.latitude + meters / 111_195, origin.longitude), accuracy,
            1_700_000_000_000 + seconds * 1000, 1000 + seconds * 1000, boot)
    private fun confirmed(): StayZone = listOf(0L, 5L, 10L).fold(StayZone(1, origin, 150.0)) { z, s -> VisitDetector.advance(z, fix(s)) }

    @Test fun walkingPassageConfirmsWithoutStopping() {
        var zone = StayZone(1, origin, 150.0)
        for (s in listOf(0L, 5L, 10L)) zone = VisitDetector.advance(zone, fix(s, s * 1.4))
        assertEquals(1L, zone.epoch)
    }

    @Test fun movingAroundLargeHouseForHoursIsOneVisit() {
        var zone = confirmed()
        for (s in 15L..28_800L step 5) zone = VisitDetector.advance(zone, fix(s, if (s % 10 == 0L) 120.0 else -110.0))
        assertEquals(1L, zone.epoch)
        assertFalse(zone.armed)
        assertEquals(origin, zone.anchor)
    }

    @Test fun departureAndReturnIncrementExactlyOnce() {
        var zone = confirmed()
        for (s in 15L..630L step 5) zone = VisitDetector.advance(zone, fix(s, 500.0))
        assertTrue(zone.armed)
        for (s in 635L..1200L step 5) zone = VisitDetector.advance(zone, fix(s))
        assertEquals(2L, zone.epoch)
    }

    @Test fun shortTripDoesNotCountAsReturn() {
        var zone = confirmed()
        for (s in 15L..300L step 5) zone = VisitDetector.advance(zone, fix(s, 500.0))
        for (s in 305L..360L step 5) zone = VisitDetector.advance(zone, fix(s))
        assertEquals(1L, zone.epoch)
    }

    @Test fun missingGpsDoesNotProveAbsence() {
        var zone = VisitDetector.advance(confirmed(), fix(15, 500.0))
        zone = VisitDetector.advance(zone, fix(3600, 500.0))
        for (s in 3605L..3620L step 5) zone = VisitDetector.advance(zone, fix(s))
        assertEquals(1L, zone.epoch)
    }

    @Test fun rebootAndWallClockChangesDoNotCreateReturn() {
        var zone = confirmed()
        for (s in 0L..30L step 5) zone = VisitDetector.advance(zone, fix(s, boot = 2).copy(timeMs = 1L))
        assertEquals(1L, zone.epoch)
    }

    @Test fun duplicatesAndOldFixesCannotConfirmArrival() {
        var zone = StayZone(1, origin, 150.0)
        repeat(20) { zone = VisitDetector.advance(zone, fix(1)) }
        zone = VisitDetector.advance(zone, fix(0))
        assertEquals(0L, zone.epoch)
    }

    @Test fun poorAccuracyInterruptsDepartureEvidence() {
        var zone = confirmed()
        for (s in 15L..610L step 5) zone = VisitDetector.advance(zone, fix(s, 500.0))
        zone = VisitDetector.advance(zone, fix(615, 500.0, accuracy = 100.0))
        zone = VisitDetector.advance(zone, fix(620, 500.0))
        assertFalse(zone.armed)
    }

    @Test fun largerStayRadiusProtectsLargeProperty() {
        var zone = confirmed()
        for (s in 15L..1200L step 5) zone = VisitDetector.advance(zone, fix(s, 400.0), radius = 500.0)
        for (s in 1205L..1220L step 5) zone = VisitDetector.advance(zone, fix(s), radius = 500.0)
        assertEquals(1L, zone.epoch)
    }

    @Test fun resizingAnArmedZoneCannotCreateVisit() {
        var zone = confirmed().copy(armed = true)
        for (s in 15L..30L step 5) zone = VisitDetector.advance(zone, fix(s), radius = 500.0)
        assertEquals(1L, zone.epoch)
    }

    @Test fun distanceWrapsDateLineAndHandlesLatitude() {
        val distance = VisitDetector.groundDistance(GeoPosition(0.0, 179.999), GeoPosition(0.0, -179.999))
        assertEquals(222.4, distance, 1.0)
        assertTrue(VisitDetector.groundDistance(GeoPosition(70.0, 0.0), GeoPosition(70.0, 0.001)) < 40)
    }
}
