package tr.ovayuva.ovayuvam.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TrackingNotificationTextTest {
    private val zone = ZoneId.of("Europe/Istanbul")

    @Test
    fun eveningSummaryUsesTheRequestedTimeZone() {
        val now = LocalDateTime.of(2026, 9, 21, 17, 0)
            .atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val stats = TrackingNotificationStats(800, 200f, now, now)

        assertEquals("Today you revealed about 800 m² of your map.", TrackingNotificationText.text(stats, zone))
        assertTrue(!TrackingNotificationText.text(stats, ZoneId.of("UTC")).startsWith("Today"))
    }

    @Test
    fun eveningTextSummarizesTodayRevealAreaInSquareMeters() {
        val now = LocalDateTime.of(2026, 9, 20, 21, 15).atZone(zone).toInstant().toEpochMilli()
        val text = TrackingNotificationText.text(
            TrackingNotificationStats(
                todayRevealedSquareMeters = 11_200,
                todayDistanceMeters = 1_650f,
                lastProgressMs = now,
                nowMs = now,
            ),
            zone,
        )

        assertEquals("Today you revealed about 11,200 m² of your map.", text)
    }

    @Test
    fun eveningTextFallsBackToDistanceWhenNoNewAreaWasStored() {
        val now = LocalDateTime.of(2026, 9, 20, 21, 15).atZone(zone).toInstant().toEpochMilli()
        val text = TrackingNotificationText.text(
            TrackingNotificationStats(
                todayRevealedSquareMeters = 0,
                todayDistanceMeters = 1_650f,
                lastProgressMs = now,
                nowMs = now,
            ),
            zone,
        )

        assertEquals("Today you walked about 1.7 km through the fog.", text)
    }

    @Test
    fun eveningTextStaysGentleForTinyProgress() {
        val now = LocalDateTime.of(2026, 9, 20, 21, 15).atZone(zone).toInstant().toEpochMilli()
        val text = TrackingNotificationText.text(
            TrackingNotificationStats(
                todayRevealedSquareMeters = 0,
                todayDistanceMeters = 42f,
                lastProgressMs = now,
                nowMs = now,
            ),
            zone,
        )

        assertEquals("A little more of your world is visible today.", text)
    }

    @Test
    fun inactivityTextAppearsAfterMultipleDaysWithoutProgress() {
        val now = LocalDateTime.of(2026, 9, 20, 12, 0).atZone(zone).toInstant().toEpochMilli()
        val fourDaysAgo = now - 4L * 24L * 60L * 60L * 1_000L

        val text = TrackingNotificationText.text(
            TrackingNotificationStats(
                todayRevealedSquareMeters = 0,
                todayDistanceMeters = 0f,
                lastProgressMs = fourDaysAgo,
                nowMs = now,
            ),
            zone,
        )

        assertEquals("Let's expand your world map. Tap to reveal a new way.", text)
    }

    @Test
    fun defaultTextRotatesQuietly() {
        val now = LocalDateTime.of(2026, 9, 20, 11, 0).atZone(zone).toInstant().toEpochMilli()

        val text = TrackingNotificationText.text(
            TrackingNotificationStats(
                todayRevealedSquareMeters = 800,
                todayDistanceMeters = 40f,
                lastProgressMs = now,
                nowMs = now,
            ),
            zone,
        )

        assertTrue(text.isNotBlank())
    }
}
