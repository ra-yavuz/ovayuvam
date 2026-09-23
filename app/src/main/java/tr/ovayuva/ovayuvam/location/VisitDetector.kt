package tr.ovayuva.ovayuvam.location

import tr.ovayuva.ovayuvam.domain.GeoPosition
import kotlin.math.*

data class VisitFix(
    val position: GeoPosition,
    val accuracyMeters: Double,
    val timeMs: Long,
    val elapsedMs: Long,
    val bootId: Int,
)

data class StayZone(
    val id: Long,
    val anchor: GeoPosition,
    val radiusMeters: Double,
    val epoch: Long = 0,
    val armed: Boolean = false,
    val awaySince: Long = 0,
    val candidateSince: Long = 0,
    val candidateFixes: Int = 0,
    val lastElapsed: Long = 0,
    val bootId: Int = -1,
)

data class VisitCount(val x: Int, val y: Int, val visits: Int, val firstVisitMs: Long, val lastVisitMs: Long)

/** Decisions use ground distances and monotonic time. Missing observations prove nothing. */
object VisitDetector {
    const val DefaultRadius = 150.0
    const val ExitBuffer = 100.0
    const val AbsenceMs = 600_000L
    const val ConfirmationMs = 10_000L
    const val MaxGapMs = 60_000L
    const val MaxAccuracy = 35.0

    fun usable(fix: VisitFix): Boolean = fix.position.latitude.isFinite() &&
        fix.position.longitude.isFinite() && fix.position.latitude in -85.0..85.0 &&
        fix.position.longitude in -180.0..180.0 && fix.accuracyMeters in 0.0..MaxAccuracy

    fun advance(saved: StayZone, fix: VisitFix, radius: Double = saved.radiusMeters): StayZone {
        if (!usable(fix)) return saved.copy(awaySince = 0, candidateSince = 0, candidateFixes = 0)
        if (saved.bootId == fix.bootId && fix.elapsedMs <= saved.lastElapsed) return saved
        val interrupted = saved.bootId != fix.bootId || fix.elapsedMs - saved.lastElapsed > MaxGapMs
        val resized = radius != saved.radiusMeters
        var zone = if (interrupted || resized) saved.copy(
            awaySince = 0, candidateSince = 0, candidateFixes = 0,
            armed = if (resized) false else saved.armed,
        ) else saved
        zone = zone.copy(radiusMeters = radius, lastElapsed = fix.elapsedMs, bootId = fix.bootId)
        val distance = groundDistance(zone.anchor, fix.position)
        if (distance - fix.accuracyMeters > radius + ExitBuffer) {
            val since = zone.awaySince.takeIf { it > 0 } ?: fix.elapsedMs
            return zone.copy(awaySince = since, candidateSince = 0, candidateFixes = 0,
                armed = zone.armed || fix.elapsedMs - since >= AbsenceMs)
        }
        if (distance + fix.accuracyMeters > radius) {
            return zone.copy(awaySince = 0, candidateSince = 0, candidateFixes = 0)
        }
        zone = zone.copy(awaySince = 0)
        if (zone.epoch > 0 && !zone.armed) return zone
        val since = zone.candidateSince.takeIf { it > 0 } ?: fix.elapsedMs
        val fixes = zone.candidateFixes + 1
        return if (fixes >= 3 && fix.elapsedMs - since >= ConfirmationMs) {
            zone.copy(epoch = zone.epoch + 1, armed = false, candidateSince = 0, candidateFixes = 0)
        } else zone.copy(candidateSince = since, candidateFixes = fixes)
    }

    fun groundDistance(a: GeoPosition, b: GeoPosition): Double {
        val lat = Math.toRadians(b.latitude - a.latitude)
        val lon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(lat / 2).pow(2) + cos(Math.toRadians(a.latitude)) *
            cos(Math.toRadians(b.latitude)) * sin(lon / 2).pow(2)
        return 6_371_008.8 * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
}
