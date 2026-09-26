package tr.ovayuva.ovayuvam.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tr.ovayuva.ovayuvam.domain.RevealCell
import tr.ovayuva.ovayuvam.domain.VisitedCell
import tr.ovayuva.ovayuvam.domain.WorldCell
import tr.ovayuva.ovayuvam.location.VisitCount
import kotlin.math.floor
import kotlin.math.roundToInt

data class FogTileImage(val reveal: Bitmap?, val heat: Bitmap?, val fresh: Bitmap? = null) {
    val bytes: Int get() = (reveal?.allocationByteCount ?: 0) + (heat?.allocationByteCount ?: 0) + (fresh?.allocationByteCount ?: 0)
}

data class FogTileFrame(val level: Int, val images: Map<FogTileKey, FogTileImage>, val day: DiscoveryDay? = null)

/** Worker-owned LRU pyramid. Published bitmaps are immutable and never recycled under the UI. */
class FogTileCache(private val maxBytes: Int = 32 * 1024 * 1024) {
    private val mutex = Mutex()
    private val index = FogTileIndex()
    private var marks = emptyMap<FogMarkId, FogTileMark>()
    private var day: DiscoveryDay? = null
    private val images = LinkedHashMap<FogTileKey, FogTileImage>(64, 0.75f, true)
    var bytes: Int = 0
        private set
    var renderedTiles: Int = 0
        private set
    var paintedMarks: Int = 0
        private set

    suspend fun update(cells: List<VisitedCell>, reveals: List<RevealCell>, visits: List<VisitCount>,
                       today: DiscoveryDay? = null) = mutex.withLock {
        val context = currentCoroutineContext()
        val counts = HashMap<WorldCell, Int>(visits.size)
        visits.forEachIndexed { i, visit ->
            if (i % 256 == 0) context.ensureActive()
            counts[WorldCell(visit.x, visit.y)] = visit.visits
        }
        val next = HashMap<FogMarkId, FogTileMark>(if (reveals.isEmpty()) cells.size else reveals.size)
        fun add(x: Int, y: Int, road: Boolean, legacy: Boolean, samples: Int, firstSeenMs: Long) {
            val size = if (legacy) 75.0 else 20.0
            val count = if (road) 0 else counts[WorldCell(floor((x + 0.5) * size / 75).toInt(), floor((y + 0.5) * size / 75).toInt())] ?: 0
            val id = FogMarkId(x, y, road, legacy)
            next[id] = FogTileMark(id, samples.coerceAtMost(if (road) 6 else 8), count, today?.contains(firstSeenMs) == true)
        }
        if (reveals.isNotEmpty()) reveals.forEachIndexed { i, cell ->
            if (i % 256 == 0) context.ensureActive()
            add(cell.x, cell.y, cell.kind == RevealCell.Kind.Road, false, cell.samples, cell.firstSeenMs)
        } else cells.forEachIndexed { i, cell ->
            if (i % 256 == 0) context.ensureActive()
            add(cell.x, cell.y, false, true, cell.samples, cell.firstSeenMs)
        }
        val removed = marks.values.filter { next[it.id] != it }
        val added = next.values.filter { marks[it.id] != it }
        context.ensureActive()
        // Commit index changes together. Cancellation before this point leaves the old index intact.
        if (removed.size + added.size > 1024) {
            images.clear()
            bytes = 0
        } else if (removed.isNotEmpty() || added.isNotEmpty()) {
            val dirty = removed + added
            val iterator = images.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (dirty.any(entry.key::intersects)) {
                    bytes -= entry.value.bytes
                    iterator.remove()
                }
            }
        }
        removed.forEach(index::remove)
        added.forEach(index::add)
        marks = next
        day = today
    }

    suspend fun frame(view: FogViewport): FogTileFrame = mutex.withLock {
        val level = FogTiles.level(view)
        val keys = FogTiles.placements(view, level).map { it.key }.distinct()
        FogTileFrame(level, keys.associateWith { tile(it) }, day)
    }

    private suspend fun tile(key: FogTileKey): FogTileImage {
        currentCoroutineContext().ensureActive()
        images[key]?.let { return it }
        val result = when {
            !index.hasData(key) -> FogTileImage(null, null)
            key.level >= FogTileIndex.LeafLevel -> paint(key, index.marks(key))
            key.children().any(images::containsKey) -> overview(key)
            else -> {
                // Sparse, distant history need not allocate a chain of mostly empty detail tiles.
                val sparse = index.marks(key, 65)
                if (sparse.size <= 64) paint(key, sparse) else overview(key)
            }
        }
        images[key] = result
        bytes += result.bytes
        renderedTiles++
        val iterator = images.iterator()
        while ((bytes > maxBytes || images.size > 512) && iterator.hasNext()) {
            bytes -= iterator.next().value.bytes
            iterator.remove()
        }
        return result
    }

    private suspend fun overview(key: FogTileKey): FogTileImage {
        val children = key.children().map { it to tile(it) }
        if (children.all { it.second.reveal == null }) return FogTileImage(null, null)
        val mask = bitmap()
        val heat = if (children.any { it.second.heat != null }) bitmap() else null
        val fresh = if (children.any { it.second.fresh != null }) bitmap() else null
        val maskCanvas = Canvas(mask)
        val heatCanvas = heat?.let(::Canvas)
        val freshCanvas = fresh?.let(::Canvas)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        for ((child, image) in children) {
            val x = (child.x % 2) * FogTiles.Pixels / 2f
            val y = (child.y % 2) * FogTiles.Pixels / 2f
            val dest = RectF(x, y, x + FogTiles.Pixels / 2, y + FogTiles.Pixels / 2)
            image.reveal?.let { maskCanvas.drawBitmap(it, null, dest, paint) }
            image.heat?.let { heatCanvas?.drawBitmap(it, null, dest, paint) }
            image.fresh?.let { freshCanvas?.drawBitmap(it, null, dest, paint) }
        }
        mask.prepareToDraw()
        heat?.prepareToDraw()
        fresh?.prepareToDraw()
        return FogTileImage(mask, heat, fresh)
    }

    private suspend fun paint(key: FogTileKey, points: List<FogTileMark>): FogTileImage {
        val context = currentCoroutineContext()
        if (points.isEmpty()) return FogTileImage(null, null)
        val mask = bitmap()
        val heat = if (points.any { it.visits > 0 }) bitmap() else null
        val fresh = if (points.any { it.fresh }) bitmap() else null
        val older = if (fresh != null && points.any { !it.fresh }) bitmap() else null
        try {
            val canvas = Canvas(mask)
            val heatCanvas = heat?.let(::Canvas)
            val freshCanvas = fresh?.let(::Canvas)
            val olderCanvas = older?.let(::Canvas)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            val heatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }
            val pixelsPerMeter = FogTiles.Pixels / key.span
            points.forEachIndexed { i, mark ->
                if (i % 128 == 0) context.ensureActive()
                val x = (FogTiles.Pixels / 2 + MercatorPoint.nearestDelta(mark.mx - (key.left + key.span / 2)) * pixelsPerMeter).toFloat()
                val y = ((key.top - mark.my) * pixelsPerMeter).toFloat()
                val radius = (mark.radius * pixelsPerMeter).toFloat()
                RevealBrush.circles(mark.id.x, mark.id.y, mark.samples, mark.id.road, radius) { dx, dy, r, alpha ->
                    paint.alpha = (alpha * 255).roundToInt()
                    canvas.drawCircle(x + dx, y + dy, r, paint)
                    if (mark.fresh) freshCanvas?.drawCircle(x + dx, y + dy, r, paint)
                    else olderCanvas?.drawCircle(x + dx, y + dy, r, paint)
                }
                if (mark.visits > 0) {
                    heatPaint.color = VisitHeat.color(mark.visits).toInt()
                    heatPaint.alpha = (VisitHeat.opacity(12.0, mark.visits) * 255).roundToInt()
                    heatCanvas?.drawCircle(x, y, radius * 0.56f, heatPaint)
                }
                paintedMarks++
            }
            context.ensureActive()
            // A new brush must not recolour parts already revealed on an earlier day.
            older?.let {
                val subtract = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }
                freshCanvas?.drawBitmap(it, 0f, 0f, subtract)
            }
            mask.prepareToDraw()
            heat?.prepareToDraw()
            fresh?.prepareToDraw()
            return FogTileImage(mask, heat, fresh)
        } catch (error: Throwable) {
            mask.recycle()
            heat?.recycle()
            fresh?.recycle()
            throw error
        } finally {
            older?.recycle()
        }
    }

    private fun bitmap() = Bitmap.createBitmap(FogTiles.Pixels, FogTiles.Pixels, Bitmap.Config.ARGB_8888)
}
