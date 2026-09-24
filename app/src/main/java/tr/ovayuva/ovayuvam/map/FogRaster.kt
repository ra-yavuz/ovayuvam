package tr.ovayuva.ovayuvam.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import tr.ovayuva.ovayuvam.domain.RevealCell
import tr.ovayuva.ovayuvam.domain.VisitedCell
import tr.ovayuva.ovayuvam.domain.WorldCell
import tr.ovayuva.ovayuvam.location.VisitCount
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** Immutable after publication. Render on a worker; camera frames only transform these two images. */
data class FogRaster(val viewport: FogViewport, val reveal: Bitmap, val heat: Bitmap?) {
    companion object {
        suspend fun render(view: FogViewport, cells: List<VisitedCell>, reveals: List<RevealCell>, visits: List<VisitCount>): FogRaster {
            val context = currentCoroutineContext()
            val raster = view.rasterViewport()
            val mask = Bitmap.createBitmap(raster.width,raster.height,Bitmap.Config.ARGB_8888)
            var heat: Bitmap? = null
            try {
                val counts = HashMap<WorldCell,Int>(visits.size)
                visits.forEachIndexed { i, visit ->
                    if (i % 256 == 0) context.ensureActive()
                    counts[WorldCell(visit.x,visit.y)] = visit.visits
                }
                if (counts.isNotEmpty()) heat = Bitmap.createBitmap(raster.width,raster.height,Bitmap.Config.ARGB_8888)
                val canvas = Canvas(mask)
                val heatCanvas = heat?.let(::Canvas)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                val heatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }
                val worldPixels = MercatorPoint.WorldWidth / raster.metersPerPixel
                fun mark(x: Int, y: Int, size: Double, samples: Int, road: Boolean) {
                    val mx = (x+0.5)*size
                    val my = (y+0.5)*size
                    val cy = raster.screenY(my).toFloat()
                    val radius = raster.radiusPixels(if (road) 11.0 else 20.0,my).toFloat()
                    val reach = radius*1.18f
                    if (cy+reach < 0 || cy-reach > raster.height) return
                    val cx = raster.screenX(mx)
                    val first = ceil((-reach-cx)/worldPixels).toInt()
                    val last = floor((raster.width+reach-cx)/worldPixels).toInt()
                    val visitsHere = if (road) 0 else counts[WorldCell(floor(mx/75.0).toInt(),floor(my/75.0).toInt())] ?: 0
                    for (copy in first..last) {
                        val px = (cx+copy*worldPixels).toFloat()
                        RevealBrush.circles(x,y,samples,road,radius) { dx,dy,r,a ->
                            paint.alpha = (a*255).roundToInt()
                            canvas.drawCircle(px+dx,cy+dy,r,paint)
                        }
                        if (visitsHere > 0) {
                            heatPaint.color = VisitHeat.color(visitsHere).toInt()
                            heatPaint.alpha = (VisitHeat.opacity(12.0,visitsHere)*255).roundToInt()
                            heatCanvas?.drawCircle(px,cy,radius*0.56f,heatPaint)
                        }
                    }
                }
                if (reveals.isNotEmpty()) reveals.forEachIndexed { i, cell ->
                    if (i % 128 == 0) context.ensureActive()
                    mark(cell.x,cell.y,WorldCell.RevealCellSizeMeters,cell.samples,cell.kind == RevealCell.Kind.Road)
                } else cells.forEachIndexed { i, cell ->
                    if (i % 128 == 0) context.ensureActive()
                    mark(cell.x,cell.y,WorldCell.DefaultCellSizeMeters,cell.samples,false)
                }
                context.ensureActive()
                mask.prepareToDraw()
                heat?.prepareToDraw()
                return FogRaster(raster,mask,heat)
            } catch (error: Throwable) {
                // These images have not reached the UI and are safe to release immediately.
                mask.recycle()
                heat?.recycle()
                throw error
            }
        }
    }
}
