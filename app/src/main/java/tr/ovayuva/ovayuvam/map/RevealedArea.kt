package tr.ovayuva.ovayuvam.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tr.ovayuva.ovayuvam.domain.RevealCell
import tr.ovayuva.ovayuvam.domain.VisitedCell
import kotlin.math.tanh

/** Estimates the union of geographic brush footprints, never a sum of overlapping circles. */
class RevealedArea {
    private val mutex = Mutex()
    private var index = FogTileIndex()
    private var marks = emptyMap<FogMarkId, FogTileMark>()
    private val areas = HashMap<FogTileKey, Double>()
    private var total = 0.0
    var renderedTiles = 0
        private set

    suspend fun update(cells: List<VisitedCell>, reveals: List<RevealCell>, excludeDay: DiscoveryDay? = null): Double = mutex.withLock {
        val context = currentCoroutineContext()
        val next = HashMap<FogMarkId, FogTileMark>()
        fun add(id: FogMarkId) { next[id] = FogTileMark(id, 1, 0) }
        if (reveals.isNotEmpty()) reveals.forEachIndexed { i, cell ->
            if (i % 256 == 0) context.ensureActive()
            if (excludeDay?.contains(cell.firstSeenMs) != true) add(FogMarkId(cell.x, cell.y, cell.kind == RevealCell.Kind.Road, false))
        } else cells.forEachIndexed { i, cell ->
            if (i % 256 == 0) context.ensureActive()
            if (excludeDay?.contains(cell.firstSeenMs) != true) add(FogMarkId(cell.x, cell.y, false, true))
        }
        val removed = marks.filterKeys { it !in next }.values
        val added = next.filterKeys { it !in marks }.values
        if (removed.isEmpty() && added.isEmpty()) return@withLock total
        val dirty = (removed + added).flatMapTo(HashSet()) { mark ->
            FogTiles.placements(FogViewport(MercatorPoint(mark.mx, mark.my), mark.reach, 2, 2), Level).map { it.key }
        }
        val bitmap = Bitmap.createBitmap(FogTiles.Pixels, FogTiles.Pixels, Bitmap.Config.ARGB_8888)
        try {
            removed.forEach(index::remove)
            added.forEach(index::add)
            val pixels = IntArray(FogTiles.Pixels * FogTiles.Pixels)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            for (key in dirty) {
                context.ensureActive()
                bitmap.eraseColor(Color.TRANSPARENT)
                val points = index.marks(key)
                val scale = FogTiles.Pixels / key.span
                for ((i, mark) in points.withIndex()) {
                    if (i % 128 == 0) context.ensureActive()
                    val x = (FogTiles.Pixels / 2 + MercatorPoint.nearestDelta(mark.mx - (key.left + key.span / 2)) * scale).toFloat()
                    val y = ((key.top - mark.my) * scale).toFloat()
                    // Opacity changes on revisits do not enlarge the brush footprint.
                    RevealBrush.circles(mark.id.x, mark.id.y, 1, mark.id.road, (mark.radius * scale).toFloat()) { dx, dy, radius, _ ->
                        canvas.drawCircle(x + dx, y + dy, radius, paint)
                    }
                }
                bitmap.getPixels(pixels, 0, FogTiles.Pixels, 0, 0, FogTiles.Pixels, FogTiles.Pixels)
                var area = 0.0
                val step = key.span / FogTiles.Pixels
                for (row in 0 until FogTiles.Pixels) {
                    var covered = 0.0
                    for (column in 0 until FogTiles.Pixels) covered += (pixels[row * FogTiles.Pixels + column] ushr 24) / 255.0
                    // Integrate ground area by latitude; Mercator pixels are not square ground metres.
                    val north = (key.top - row * step) / MercatorPoint.EarthRadius
                    val south = north - step / MercatorPoint.EarthRadius
                    area += covered * step * MercatorPoint.EarthRadius * (tanh(north) - tanh(south))
                }
                total += area - (areas[key] ?: 0.0)
                if (area > 0) areas[key] = area else areas.remove(key)
                renderedTiles++
            }
            marks = next
            total = total.coerceAtLeast(0.0)
            total
        } catch (error: Throwable) {
            // A cancelled partial update must be rebuilt, not treated as a complete cached total.
            index = FogTileIndex(); marks = emptyMap(); areas.clear(); total = 0.0
            throw error
        } finally {
            bitmap.recycle()
        }
    }

    private companion object { const val Level = 16 }
}
