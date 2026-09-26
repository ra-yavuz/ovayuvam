package tr.ovayuva.ovayuvam.map

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class DiscoveryDayTest {
    private fun time(value: String) = Instant.parse(value).toEpochMilli()

    @Test fun todayUsesLocalCalendarDateNotLast24Hours() {
        val day = DiscoveryDay.at(time("2026-09-26T20:59:00Z"), ZoneId.of("Europe/Istanbul"))
        assertEquals(time("2026-09-25T21:00:00Z"), day.startMs)
        assertEquals(time("2026-09-26T21:00:00Z"), day.endMs)
        assertTrue(day.contains(day.startMs))
        assertFalse(day.contains(day.startMs - 1))
        assertFalse(day.contains(day.endMs))
    }

    @Test fun unknownDatesDoNotBecomeFresh() {
        assertFalse(DiscoveryDay(0, 100).contains(0))
        assertFalse(DiscoveryDay(0, 100).contains(-1))
    }

    @Test fun springClockChangeIs23Hours() {
        val day = DiscoveryDay.at(time("2026-03-29T12:00:00Z"), ZoneId.of("Europe/Berlin"))
        assertEquals(23 * 3_600_000L, day.endMs - day.startMs)
    }

    @Test fun autumnClockChangeIs25Hours() {
        val day = DiscoveryDay.at(time("2026-10-25T12:00:00Z"), ZoneId.of("Europe/Berlin"))
        assertEquals(25 * 3_600_000L, day.endMs - day.startMs)
    }

    @Test fun timezoneChangeReclassifiesCalendarDay() {
        val now = time("2026-09-26T22:00:00Z")
        val earlier = time("2026-09-26T18:00:00Z")
        assertTrue(DiscoveryDay.at(now, ZoneId.of("UTC")).contains(earlier))
        assertFalse(DiscoveryDay.at(now, ZoneId.of("Europe/Istanbul")).contains(earlier))
    }
}
