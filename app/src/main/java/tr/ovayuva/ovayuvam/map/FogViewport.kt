package tr.ovayuva.ovayuvam.map

import tr.ovayuva.ovayuvam.domain.GeoPosition
import kotlin.math.PI
import kotlin.math.cosh
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

data class MercatorPoint(val x: Double, val y: Double) {
    companion object {
        const val EarthRadius = 6_378_137.0
        const val WorldWidth = 2 * PI * EarthRadius
        fun from(position: GeoPosition): MercatorPoint = MercatorPoint(
            EarthRadius * Math.toRadians(position.longitude),
            EarthRadius * ln(tan(PI / 4 + Math.toRadians(position.latitude.coerceIn(-85.05112878,85.05112878)) / 2)),
        )
        fun nearestDelta(delta: Double): Double = delta - floor(delta / WorldWidth + 0.5) * WorldWidth
    }
}

/** A north-up, untilted map view in physical pixels. Radius stays in ground meters. */
data class FogViewport(val center: MercatorPoint, val metersPerPixel: Double, val width: Int, val height: Int) {
    init {
        require(metersPerPixel.isFinite() && metersPerPixel > 0)
        require(width > 0 && height > 0)
    }
    fun screenX(x: Double): Double = width / 2.0 + MercatorPoint.nearestDelta(x - center.x) / metersPerPixel
    fun screenY(y: Double): Double = height / 2.0 + (center.y - y) / metersPerPixel
    fun radiusPixels(meters: Double, y: Double): Double = meters * cosh(y / MercatorPoint.EarthRadius) / metersPerPixel
    fun covers(view: FogViewport): Boolean {
        val ratio = metersPerPixel/view.metersPerPixel
        if (ratio !in 0.67..1.5) return false
        val dx = kotlin.math.abs(MercatorPoint.nearestDelta(view.center.x-center.x))
        val dy = kotlin.math.abs(view.center.y-center.y)
        return dx + view.width*view.metersPerPixel/2 <= width*metersPerPixel/2 &&
            dy + view.height*view.metersPerPixel/2 <= height*metersPerPixel/2
    }
    fun rasterViewport(): FogViewport {
        val scale = minOf(1.0, 1536.0 / (maxOf(width,height) * 1.5))
        return copy(metersPerPixel = metersPerPixel / scale,
            width = (width * 1.5 * scale).roundToInt().coerceAtLeast(1),
            height = (height * 1.5 * scale).roundToInt().coerceAtLeast(1))
    }
}
