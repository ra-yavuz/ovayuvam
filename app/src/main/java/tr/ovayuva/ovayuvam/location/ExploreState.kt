package tr.ovayuva.ovayuvam.location

import android.content.Context
import android.content.SharedPreferences
import tr.ovayuva.ovayuvam.domain.GeoPosition
import tr.ovayuva.ovayuvam.domain.WorldCell
import tr.ovayuva.ovayuvam.domain.RevealCell
import tr.ovayuva.ovayuvam.storage.VisitRepository
import tr.ovayuva.ovayuvam.storage.MapWindow
import org.json.JSONArray
import kotlin.math.cos

internal class ExploreState(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("explore", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) = synchronized(Lock) {
            prefs.edit().putBoolean("enabled", value).remove("restLat").remove("restLon")
                .remove("anchorLat").remove("anchorLon").remove("targetLat").remove("targetLon")
                .remove("fixMs").remove("candidates").apply()
        }

    fun observe(position: GeoPosition, accuracy: Float, now: Long) = synchronized(Lock) {
        if (!enabled || accuracy > 30f) return@synchronized
        val rest = point("rest")
        val last = prefs.getLong("fixMs", 0)
        val active = target(now)
        if (active != null && (ExplorePolicy.distance(active.position, position) <= 25 ||
                    !ExplorePolicy.valid(active.position, position, active.createdMs, now))) {
            prefs.edit().remove("targetLat").remove("targetLon").apply()
        }
        if (now - last in 0..29_999 && rest != null && ExplorePolicy.distance(rest, position) <= 60) return@synchronized
        val reset = rest == null || now - last !in 0..ExplorePolicy.MaxGapMs || ExplorePolicy.distance(rest, position) > 60
        val edit = prefs.edit().putLong("fixMs", now)
        if (reset) edit.point("rest", position).putLong("restMs", now)
            .remove("anchorLat").remove("anchorLon")
        else if (now - prefs.getLong("restMs", now) >= ExplorePolicy.RestMs) edit.point("anchor", rest)
        edit.apply()
    }

    fun anchor(now: Long): GeoPosition? = synchronized(Lock) {
        if (!enabled || now - prefs.getLong("fixMs", 0) !in 0..ExplorePolicy.MaxGapMs) null
        else point("anchor")
    }

    fun target(now: Long = System.currentTimeMillis()): GoalPin? = synchronized(Lock) {
        if (!enabled) return@synchronized null
        val position = point("target") ?: return@synchronized null
        val created = prefs.getLong("created", 0)
        if (now - created !in 0 until ExplorePolicy.LifetimeMs) null else GoalPin(position, created)
    }

    fun due(now: Long) = enabled && target(now) == null && ExplorePolicy.due(now, prefs.getLong("last", 0))
    fun previous(): GeoPosition? = point("previous")

    fun cacheCandidates(candidates: List<GeoPosition>, current: GeoPosition, now: Long) = synchronized(Lock) {
        if (!enabled) return@synchronized
        val nearby = candidates.filter { ExplorePolicy.distance(it, current) <= ExplorePolicy.MaxDistance }
            .distinctBy { WorldCell.fromLocation(it.latitude, it.longitude, 10.0) }.take(2000)
        // Panning to a different town must not erase the useful local cache.
        if (nearby.isEmpty()) return@synchronized
        val json = JSONArray()
        nearby.forEach { json.put(JSONArray().put(it.latitude).put(it.longitude)) }
        prefs.edit().putString("candidates", json.toString()).putLong("candidatesMs", now).apply()
    }

    /** Called on the existing worker, at most once a minute. Uses only nearby saved history. */
    fun suggest(repository: VisitRepository, current: GeoPosition, now: Long): GeoPosition? {
        val anchor = anchor(now) ?: return null
        if (!due(now) || now - prefs.getLong("candidatesMs", 0) !in 0..ExplorePolicy.WeekMs) return null
        val candidates = runCatching {
            val json = JSONArray(prefs.getString("candidates", "[]"))
            (0 until json.length()).map { json.getJSONArray(it).let { row -> GeoPosition(row.getDouble(0), row.getDouble(1)) } }
        }.getOrDefault(emptyList()).filter { ExplorePolicy.distance(it, current) <= ExplorePolicy.MaxDistance }
        if (candidates.isEmpty()) return null
        val lat = 650.0 / 110_000
        val lon = lat / cos(Math.toRadians(current.latitude)).coerceAtLeast(0.08)
        fun wrap(value: Double) = (value + 540) % 360 - 180
        val data = repository.mapData(MapWindow(current.latitude - lat, wrap(current.longitude - lon),
            current.latitude + lat, wrap(current.longitude + lon)))
        val revealed = data.reveal.map { row ->
            WorldCell(row.x, row.y).centerPosition(WorldCell.RevealCellSizeMeters) to
                (if (row.kind == RevealCell.Kind.Road) 14.0 else 26.0)
        } + data.cells.map { row -> WorldCell(row.x, row.y).centerPosition() to 26.0 }
        return ExplorePolicy.select(anchor, current, candidates, revealed, previous())
    }

    fun offer(position: GeoPosition, current: GeoPosition, now: Long): Boolean = synchronized(Lock) {
        val anchor = anchor(now) ?: return@synchronized false
        if (!due(now) || ExplorePolicy.distance(anchor, position) > ExplorePolicy.MaxDistance ||
            ExplorePolicy.distance(current, position) !in 35.0..ExplorePolicy.MaxDistance) return@synchronized false
        prefs.edit().point("target", position).point("previous", position)
            .putLong("created", now).putLong("last", now).apply()
        true
    }

    private fun point(prefix: String): GeoPosition? {
        if (!prefs.contains(prefix + "Lat") || !prefs.contains(prefix + "Lon")) return null
        return GeoPosition(Double.fromBits(prefs.getLong(prefix + "Lat", 0)), Double.fromBits(prefs.getLong(prefix + "Lon", 0)))
    }

    private fun SharedPreferences.Editor.point(prefix: String, point: GeoPosition) =
        putLong(prefix + "Lat", point.latitude.toRawBits()).putLong(prefix + "Lon", point.longitude.toRawBits())

    private companion object { val Lock = Any() }
}
