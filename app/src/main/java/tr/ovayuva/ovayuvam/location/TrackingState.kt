package tr.ovayuva.ovayuvam.location

import android.content.Context
import tr.ovayuva.ovayuvam.domain.GeoPosition
import tr.ovayuva.ovayuvam.domain.WorldCell

class TrackingState(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("tracking", Context.MODE_PRIVATE)

    fun isTracking(): Boolean = preferences.getBoolean("active", false)

    fun setTracking(active: Boolean) {
        preferences.edit().putBoolean("active", active).apply()
    }

    fun currentCell(): WorldCell? {
        if (!preferences.contains("current_x") || !preferences.contains("current_y")) return null
        return WorldCell(
            x = preferences.getInt("current_x", 0),
            y = preferences.getInt("current_y", 0),
        )
    }

    fun currentPosition(): GeoPosition? {
        if (preferences.contains("current_lat") && preferences.contains("current_lon")) {
            return GeoPosition(
                latitude = Double.fromBits(preferences.getLong("current_lat", 0L)),
                longitude = Double.fromBits(preferences.getLong("current_lon", 0L)),
            )
        }
        return currentCell()?.centerPosition()
    }

    fun setCurrentLocation(latitude: Double, longitude: Double, cell: WorldCell) {
        preferences.edit()
            .putInt("current_x", cell.x)
            .putInt("current_y", cell.y)
            .putLong("current_lat", latitude.toRawBits())
            .putLong("current_lon", longitude.toRawBits())
            .apply()
    }

    fun clearCurrentCell() {
        preferences.edit()
            .remove("current_x")
            .remove("current_y")
            .remove("current_lat")
            .remove("current_lon")
            .apply()
    }
}
