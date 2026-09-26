package tr.ovayuva.ovayuvam.map

import java.time.Instant
import java.time.ZoneId

data class DiscoveryDay(val startMs: Long, val endMs: Long) {
    fun contains(firstSeenMs: Long) = firstSeenMs > 0 && firstSeenMs >= startMs && firstSeenMs < endMs

    companion object {
        fun at(nowMs: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): DiscoveryDay {
            val date = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
            return DiscoveryDay(date.atStartOfDay(zone).toInstant().toEpochMilli(),
                date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
        }
    }
}
