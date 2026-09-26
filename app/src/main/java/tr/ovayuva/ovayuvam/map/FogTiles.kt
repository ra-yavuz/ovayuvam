package tr.ovayuva.ovayuvam.map

import kotlin.math.ceil
import kotlin.math.cosh
import kotlin.math.floor
import kotlin.math.log2

/** Fixed geographic tiles, independent of screen size and camera position. */
data class FogTileKey(val level: Int, val x: Int, val y: Int) {
    val span: Double get() = MercatorPoint.WorldWidth / (1 shl level)
    val left: Double get() = -MercatorPoint.WorldWidth / 2 + x * span
    val top: Double get() = MercatorPoint.WorldWidth / 2 - y * span
    fun parent() = FogTileKey(level - 1, x / 2, y / 2)
    fun children() = (0..1).flatMap { dy -> (0..1).map { dx -> FogTileKey(level + 1, x * 2 + dx, y * 2 + dy) } }
    fun intersects(mark: FogTileMark): Boolean {
        val dx = kotlin.math.abs(MercatorPoint.nearestDelta(mark.mx - (left + span / 2)))
        val dy = kotlin.math.abs(mark.my - (top - span / 2))
        return dx <= span / 2 + mark.reach && dy <= span / 2 + mark.reach
    }
}

data class FogTilePlacement(val key: FogTileKey, val worldX: Int)

object FogTiles {
    const val Pixels = 256
    const val MaxLevel = 22
    const val MaxVisibleTiles = 48

    fun level(view: FogViewport): Int {
        var level = floor(log2(MercatorPoint.WorldWidth / (view.metersPerPixel * Pixels))).toInt().coerceIn(0, MaxLevel)
        while (level > 0 && placements(view, level).size > MaxVisibleTiles) level--
        return level
    }

    fun placements(view: FogViewport, level: Int): List<FogTilePlacement> {
        val count = 1 shl level
        val span = MercatorPoint.WorldWidth / count
        val half = MercatorPoint.WorldWidth / 2
        val west = floor((view.center.x - view.width * view.metersPerPixel / 2 + half) / span).toInt()
        val east = ceil((view.center.x + view.width * view.metersPerPixel / 2 + half) / span).toInt() - 1
        val north = floor((half - view.center.y - view.height * view.metersPerPixel / 2) / span).toInt().coerceAtLeast(0)
        val south = (ceil((half - view.center.y + view.height * view.metersPerPixel / 2) / span).toInt() - 1).coerceAtMost(count - 1)
        return buildList {
            for (y in north..south) for (x in west..east) add(FogTilePlacement(FogTileKey(level, Math.floorMod(x, count), y), x))
        }
    }
}

data class FogMarkId(val x: Int, val y: Int, val road: Boolean, val legacy: Boolean)

data class FogTileMark(val id: FogMarkId, val samples: Int, val visits: Int, val fresh: Boolean = false) {
    private val cellSize = if (id.legacy) 75.0 else 20.0
    val mx = (id.x + 0.5) * cellSize
    val my = (id.y + 0.5) * cellSize
    val radius = (if (id.road) 11.0 else 20.0) * cosh(my / MercatorPoint.EarthRadius)
    // The offset edge circles can extend beyond the central 1.18-radius circle.
    val reach = radius * 1.26
}

/** Sparse geographic index. Brushes belong to every bucket they touch, including across the date line. */
class FogTileIndex {
    companion object { const val LeafLevel = 14 }
    private val buckets = HashMap<FogTileKey, MutableMap<FogMarkId, FogTileMark>>()
    private val occupied = HashMap<FogTileKey, Int>()

    private fun bucketsFor(mark: FogTileMark): Set<FogTileKey> {
        val view = FogViewport(MercatorPoint(mark.mx, mark.my), mark.reach, 2, 2)
        return FogTiles.placements(view, LeafLevel).mapTo(HashSet()) { it.key }
    }

    fun add(mark: FogTileMark) {
        for (key in bucketsFor(mark)) {
            val bucket = buckets.getOrPut(key) {
                var node = key
                while (true) {
                    occupied[node] = (occupied[node] ?: 0) + 1
                    if (node.level == 0) break
                    node = node.parent()
                }
                HashMap()
            }
            bucket[mark.id] = mark
        }
    }

    fun remove(mark: FogTileMark) {
        for (key in bucketsFor(mark)) {
            val bucket = buckets[key] ?: continue
            bucket.remove(mark.id)
            if (bucket.isEmpty()) {
                buckets.remove(key)
                var node = key
                while (true) {
                    val count = occupied.getValue(node) - 1
                    if (count == 0) occupied.remove(node) else occupied[node] = count
                    if (node.level == 0) break
                    node = node.parent()
                }
            }
        }
    }

    fun hasData(key: FogTileKey): Boolean {
        var ancestor = key
        while (ancestor.level > LeafLevel) ancestor = ancestor.parent()
        return occupied.containsKey(ancestor)
    }

    fun marks(key: FogTileKey, limit: Int = Int.MAX_VALUE): List<FogTileMark> {
        val found = HashMap<FogMarkId, FogTileMark>()
        fun visit(node: FogTileKey) {
            if (found.size >= limit || !hasData(node)) return
            if (node.level >= LeafLevel) {
                var bucket = node
                while (bucket.level > LeafLevel) bucket = bucket.parent()
                for (mark in buckets[bucket]?.values.orEmpty()) {
                    if (key.intersects(mark)) found[mark.id] = mark
                    if (found.size >= limit) break
                }
            } else node.children().forEach(::visit)
        }
        visit(key)
        return found.values.sortedWith(compareBy({ it.id.legacy }, { it.id.road }, { it.id.x }, { it.id.y }))
    }
}
