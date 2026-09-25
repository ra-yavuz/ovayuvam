package tr.ovayuva.ovayuvam

import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlinx.coroutines.*
import org.json.JSONObject
import tr.ovayuva.ovayuvam.domain.*
import tr.ovayuva.ovayuvam.location.VisitCount
import tr.ovayuva.ovayuvam.map.*
import java.io.File
import kotlin.math.floor

fun Instrumentation.checkFogTiles() = runBlocking(Dispatchers.Default) {
    val reveal = RevealCell(13000, 312000, RevealCell.Kind.Core, 1, 2, 8)
    val mark = FogTileMark(FogMarkId(reveal.x, reveal.y, false, false), 8, 100)
    val center = MercatorPoint(mark.mx, mark.my)
    val visit = VisitCount(floor(mark.mx / 75).toInt(), floor(mark.my / 75).toInt(), 100, 1, 2)
    val cache = FogTileCache()
    cache.update(emptyList(), listOf(reveal), listOf(visit))
    val view = FogViewport(center, 1.0, 1080, 2400)
    val first = cache.frame(view)
    val painted = cache.paintedMarks
    val rendered = cache.renderedTiles
    repeat(60) { cache.frame(view.copy(center = center.copy(x = center.x + it / 100.0))) }
    check(cache.paintedMarks == painted && cache.renderedTiles == rendered) { "Phone-sized pan repainted saved history" }
    cache.update(emptyList(), listOf(reveal.copy(lastSeenMs = 200)), listOf(visit.copy(lastVisitMs = 500)))
    val unchanged = cache.frame(view)
    first.images.forEach { (key, image) -> check(unchanged.images[key] === image) { "Timestamp-only change invalidated a tile" } }

    fun pixel(frame: FogTileFrame, point: MercatorPoint, heat: Boolean = false): Int {
        val count = 1 shl frame.level
        val span = MercatorPoint.WorldWidth / count
        val x = floor((point.x + MercatorPoint.WorldWidth / 2) / span).toInt()
        val y = floor((MercatorPoint.WorldWidth / 2 - point.y) / span).toInt()
        val key = FogTileKey(frame.level, Math.floorMod(x, count), y)
        val image = frame.images.getValue(key)
        val bitmap = (if (heat) image.heat else image.reveal) ?: return 0
        val px = floor((span / 2 + MercatorPoint.nearestDelta(point.x - (key.left + span / 2))) / span * FogTiles.Pixels).toInt().coerceIn(0, 255)
        val py = floor((key.top - point.y) / span * FogTiles.Pixels).toInt().coerceIn(0, 255)
        return bitmap.getPixel(px, py)
    }
    check(Color.alpha(pixel(first, center)) == 255)
    check(Color.alpha(pixel(first, center.copy(x = center.x + 100))) == 0)
    check(Color.alpha(pixel(first, center, heat = true)) in 1..92)

    cache.update(emptyList(), listOf(reveal.copy(samples = 9), reveal.copy(x = reveal.x + 3)), listOf(visit))
    val changed = cache.frame(view)
    var preserved = 0
    var replaced = 0
    first.images.forEach { (key, image) ->
        if (key.intersects(mark.copy(id = mark.id.copy(x = reveal.x + 3)))) {
            if (changed.images[key] !== image) replaced++
        } else {
            check(changed.images[key] === image) { "A local reveal replaced an unrelated tile" }
            preserved++
        }
    }
    check(preserved > 0 && replaced > 0)

    val coarse = view.copy(metersPerPixel = MercatorPoint.WorldWidth / (256 * (1 shl 12)))
    cache.frame(coarse)
    val afterOverview = cache.paintedMarks
    cache.frame(coarse.copy(metersPerPixel = coarse.metersPerPixel * 2))
    check(cache.paintedMarks == afterOverview) { "Zooming out repainted points instead of using child images" }

    val sparse = FogTileCache()
    sparse.update(emptyList(), listOf(reveal, reveal.copy(x = -500000, y = 0)), emptyList())
    sparse.frame(view.copy(metersPerPixel = MercatorPoint.WorldWidth / 256))
    check(sparse.renderedTiles == 1) { "Sparse world overview built unnecessary detail tiles" }

    // Compare adjacent tiles against a single-canvas reference, including fractional screen pixels.
    val edgeReveal = reveal.copy(x = 0, y = 0)
    cache.update(emptyList(), listOf(edgeReveal), emptyList())
    val edgeView = FogViewport(MercatorPoint(0.37, 0.63), 0.7, 256, 256)
    val edgeFrame = cache.frame(edgeView)
    val output = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    for ((key, tile) in edgeFrame.images) {
        val bitmap = tile.reveal ?: continue
        val left = edgeView.screenX(key.left + key.span / 2) - key.span / edgeView.metersPerPixel / 2
        val top = edgeView.screenY(key.top)
        val size = key.span / edgeView.metersPerPixel
        canvas.drawBitmap(bitmap, null, RectF(left.toFloat(), top.toFloat(), (left + size).toFloat(), (top + size).toFloat()), paint)
    }
    val reference = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    val referenceCanvas = Canvas(reference)
    val brush = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    val edgeMark = FogTileMark(FogMarkId(0, 0, false, false), 8, 0)
    RevealBrush.circles(0, 0, 8, false, (edgeMark.radius / edgeView.metersPerPixel).toFloat()) { dx, dy, radius, alpha ->
        brush.alpha = (alpha * 255).toInt()
        referenceCanvas.drawCircle(edgeView.screenX(10.0).toFloat() + dx, edgeView.screenY(10.0).toFloat() + dy, radius, brush)
    }
    var worstInteriorError = 0
    for (y in 100..155) for (x in 100..155) {
        if (Color.alpha(reference.getPixel(x, y)) == 255) {
            worstInteriorError = maxOf(worstInteriorError, 255 - Color.alpha(output.getPixel(x, y)))
        }
    }
    check(worstInteriorError <= 12) { "Tile seam inside clear brush: $worstInteriorError" }

    val outputDir = File(targetContext.filesDir, "fog-tiles").apply { mkdirs() }
    File(outputDir, "tile-boundaries.png").outputStream().use { output.compress(Bitmap.CompressFormat.PNG, 100, it) }
    output.recycle()
    reference.recycle()

    val legacy = VisitedCell(0, 0, 1, 2, 8)
    cache.update(listOf(legacy), emptyList(), emptyList())
    val legacyView = view.copy(center = MercatorPoint(37.5, 37.5))
    check(Color.alpha(pixel(cache.frame(legacyView), legacyView.center)) == 255)
    val edgeOfWorld = MercatorPoint(MercatorPoint.WorldWidth / 2 - 10, 10.0)
    cache.update(emptyList(), listOf(reveal.copy(x = -1001876, y = 0)), emptyList())
    val dateLine = cache.frame(view.copy(center = edgeOfWorld))
    check(Color.alpha(pixel(dateLine, MercatorPoint(-20037510.0, 10.0))) > 240)
    cache.update(emptyList(), emptyList(), emptyList())
    check(cache.frame(legacyView).images.values.all { it.reveal == null }) { "Replay rewind retained future reveals" }

    val bounded = FogTileCache(1024 * 1024)
    bounded.update(emptyList(), List(2000) { reveal.copy(x = reveal.x + it % 50, y = reveal.y + it / 50) }, emptyList())
    bounded.frame(view)
    check(bounded.bytes <= 1024 * 1024) { "Tile cache exceeded its memory budget" }
    val job = launch {
        bounded.update(emptyList(), List(100000) { reveal.copy(x = it, y = it) }, emptyList())
        bounded.frame(coarse)
    }
    delay(5)
    job.cancelAndJoin()
    bounded.update(emptyList(), listOf(reveal), emptyList())
    check(Color.alpha(pixel(bounded.frame(view), center)) == 255) { "Cancelled update corrupted index" }
    File(outputDir, "checks.json").writeText(JSONObject().put("sameViewPaints", painted)
        .put("extraPaintsFor60Pans", 0).put("unrelatedTilesPreserved", preserved)
        .put("changedTilesReplaced", replaced).put("tileSeamAlphaError", worstInteriorError)
        .put("boundedCacheBytes", bounded.bytes).toString(2))
}
