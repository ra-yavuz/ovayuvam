package tr.ovayuva.ovayuvam.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TrackingNotificationTextTest {
    private val zone = ZoneId.of("Europe/Istanbul")

    @Test
    fun eveningTextSummarizesTodayDistance() {
        val now = LocalDateTime.of(2026, 9, 20, 21, 15).atZone(zone).toInstant().toEpochMilli()
        val text = TrackingNotificationText.text(
            TrackingNotificationStats(
                todayHasProgress = true,
                todayDistanceMeters = 1_650f,
                lastProgressMs = now,
                nowMs = now,
            ),
            zone,
        )

        assertEquals("Today you walked about 1.7 km through the fog.", text)
    }

    @Test
    fun eveningTextAvoidsInflatedAreaForSmallWalks() {
        val now = LocalDateTime.of(2026, 9, 20, 21, 15).atZone(zone).toInstant().toEpochMilli()
        val text = TrackingNotificationText.text(
            TrackingNotificationStats(
                todayHasProgress = true,
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
                todayHasProgress = false,
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
                todayHasProgress = true,
                todayDistanceMeters = 40f,
                lastProgressMs = now,
                nowMs = now,
            ),
            zone,
        )

        assertTrue(text.isNotBlank())
    }
}
