package tr.ovayuva.ovayuvam.domain

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

data class GeoPosition(val latitude: Double, val longitude: Double)

data class WorldCell(val x: Int, val y: Int) {
    fun centerPosition(cellSizeMeters: Double = DefaultCellSizeMeters): GeoPosition {
        require(cellSizeMeters > 0.0) { "cell size must be positive" }
        val xMeters = (x + 0.5) * cellSizeMeters
        val yMeters = (y + 0.5) * cellSizeMeters
        val lon = (xMeters / EarthRadiusM).toDegrees().coerceIn(-180.0, 180.0)
        val lat = (2.0 * atan(exp(yMeters / EarthRadiusM)) - PI / 2.0)
            .toDegrees()
            .coerceIn(-MaxMercatorLat, MaxMercatorLat)
        return GeoPosition(latitude = lat, longitude = lon)
    }

    companion object {
        const val DefaultCellSizeMeters = 75.0
        const val RevealCellSizeMeters = 20.0
        private const val EarthRadiusM = 6_378_137.0
        private const val MaxMercatorLat = 85.05112878

        fun fromLocation(latitude: Double, longitude: Double, cellSizeMeters: Double = DefaultCellSizeMeters): WorldCell {
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

private fun Double.toDegrees() = this * 180.0 / PI
