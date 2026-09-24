package tr.ovayuva.ovayuvam

import android.app.Instrumentation
import android.graphics.Color
import kotlinx.coroutines.*
import tr.ovayuva.ovayuvam.domain.*
import tr.ovayuva.ovayuvam.location.VisitCount
import tr.ovayuva.ovayuvam.map.*
import kotlin.math.floor
import kotlin.math.roundToInt

fun Instrumentation.checkFogRaster() = runBlocking {
    val cell = WorldCell.fromLocation(48.863,2.331,20.0)
    val center = MercatorPoint((cell.x+0.5)*20,(cell.y+0.5)*20)
    val reveal = RevealCell(cell.x,cell.y,RevealCell.Kind.Core,1,2,8)
    val visit = VisitCount(floor(center.x/75).toInt(),floor(center.y/75).toInt(),100,1,2)
    val view = FogViewport(center,1.0,720,1280)
    val raster = withContext(Dispatchers.Default) { FogRaster.render(view,emptyList(),listOf(reveal),listOf(visit)) }
    val x = raster.viewport.screenX(center.x).roundToInt()
    val y = raster.viewport.screenY(center.y).roundToInt()
    check(Color.alpha(raster.reveal.getPixel(x,y)) == 255) { "Center not fully revealed" }
    check(Color.alpha(raster.reveal.getPixel(x+100,y)) == 0) { "Unvisited area revealed" }
    val heatAlpha = Color.alpha(checkNotNull(raster.heat).getPixel(x,y))
    check(heatAlpha in 1..92) { "Heat is not translucent: $heatAlpha" }
    raster.reveal.recycle(); raster.heat?.recycle()

    val legacy = WorldCell.fromLocation(48.863,2.331)
    val point = MercatorPoint((legacy.x+0.5)*75,(legacy.y+0.5)*75)
    val old = FogRaster.render(view.copy(center=point),listOf(VisitedCell(legacy.x,legacy.y,1,2,8)),emptyList(),emptyList())
    check(Color.alpha(old.reveal.getPixel(old.viewport.width/2,old.viewport.height/2)) == 255)
    old.reveal.recycle()

    val east = MercatorPoint.from(GeoPosition(0.0,179.999))
    val westCell = WorldCell.fromLocation(0.0,-179.999,20.0)
    val lineView = FogViewport(east,1.0,720,1280)
    val line = FogRaster.render(lineView,emptyList(),listOf(reveal.copy(x=westCell.x,y=westCell.y)),emptyList())
    val lx = line.viewport.screenX((westCell.x+0.5)*20).roundToInt()
    val ly = line.viewport.screenY((westCell.y+0.5)*20).roundToInt()
    check(Color.alpha(line.reveal.getPixel(lx,ly)) == 255) { "Date-line reveal lost" }
    line.reveal.recycle()

    val job = launch(Dispatchers.Default) {
        FogRaster.render(view,emptyList(),List(1_000_000) { reveal.copy(x=cell.x+it%100,y=cell.y+it/100) },emptyList())
    }
    delay(10)
    job.cancelAndJoin()
    check(job.isCancelled)
}
