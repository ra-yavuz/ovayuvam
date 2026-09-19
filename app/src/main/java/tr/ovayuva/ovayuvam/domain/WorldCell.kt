package tr.ovayuva.ovayuvam.domain

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

data class WorldCell(val x: Int, val y: Int) {
    companion object {
        private const val EarthRadiusM = 6_378_137.0
        private const val MaxMercatorLat = 85.05112878

        fun fromLocation(latitude: Double, longitude: Double, cellSizeMeters: Double = 75.0): WorldCell {
            require(cellSizeMeters > 0.0) { "cell size must be positive" }
            val lat = latitude.coerceIn(-MaxMercatorLat, MaxMercatorLat)
            val lon = longitude.coerceIn(-180.0, 180.0)
            val xMeters = EarthRadiusM * lon.toRadians()
            val yMeters = EarthRadiusM * ln(tan(PI / 4.0 + lat.toRadians() / 2.0))
            return WorldCell(
                x = floor(xMeters / cellSizeMeters).toInt(),
                y = floor(yMeters / cellSizeMeters).toInt(),
            )
        }

        private fun Double.toRadians() = this * PI / 180.0
    }
}

