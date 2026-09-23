package tr.ovayuva.ovayuvam.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

        assertEquals("Today you revealed about 800 m² of your map.", TrackingNotificationText.text(stats, zone).substringBefore('\n'))
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

        assertEquals("Today you revealed about 11,200 m² of your map.", text.substringBefore('\n'))
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

        assertEquals("Today you walked about 1.7 km through the fog.", text.substringBefore('\n'))
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

        assertEquals("A little more of your world is visible today.", text.substringBefore('\n'))
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

        assertTrue(text.isNotBlank())
        assertFalse(text.contains("Tap"))
        assertFalse(text.contains("Let's expand"))
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

    @Test fun manyGentleMessagesStayStableWithinEachRotation() {
        val rotation = TrackingNotificationText.RotationMs
        val start = 100_000L * rotation
        val messages = (0..105).map { slot ->
            val now = start + slot * rotation + 1000L
            val stats = TrackingNotificationStats(0, 0f, null, now)
            val text = TrackingNotificationText.text(stats, zone)
            assertEquals(text, TrackingNotificationText.text(stats.copy(nowMs = now + 60_000L), zone))
            assertTrue(text.length <= 100)
            text
        }
        assertTrue(messages.toSet().size >= 50)
        messages.zipWithNext().forEach { (first, second) -> assertNotEquals(first, second) }
    }

    @Test fun eveningSummaryKeepsItsFactsWhileTheCompanionLineRotates() {
        val now = LocalDateTime.of(2026, 9, 23, 20, 0).atZone(zone).toInstant().toEpochMilli()
        val stats = TrackingNotificationStats(1200, 100f, now, now)
        val first = TrackingNotificationText.text(stats, zone)
        val next = TrackingNotificationText.text(stats.copy(nowMs = now + TrackingNotificationText.RotationMs), zone)
        assertEquals(first.substringBefore('\n'), next.substringBefore('\n'))
        assertNotEquals(first.substringAfter('\n'), next.substringAfter('\n'))
    }

    @Test fun nextRotationIsAlwaysInTheFutureAndHandlesClockChanges() {
        val rotation = TrackingNotificationText.RotationMs
        assertEquals(rotation, TrackingNotificationText.nextRotationDelay(0))
        assertEquals(1L, TrackingNotificationText.nextRotationDelay(rotation - 1))
        assertEquals(rotation, TrackingNotificationText.nextRotationDelay(rotation))
        assertEquals(1L, TrackingNotificationText.nextRotationDelay(-1))
    }
}
