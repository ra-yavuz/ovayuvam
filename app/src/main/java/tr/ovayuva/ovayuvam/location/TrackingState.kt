package tr.ovayuva.ovayuvam.location

import android.content.Context
import tr.ovayuva.ovayuvam.domain.GeoPosition
import tr.ovayuva.ovayuvam.domain.WorldCell

class TrackingState(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("tracking", Context.MODE_PRIVATE)

    val consented: Boolean get() = preferences.getInt("disclosure-version", 0) >= 1
    val enabled: Boolean get() = consented && preferences.getBoolean("enabled", false)

    fun acceptDisclosure() {
        preferences.edit().putInt("disclosure-version", 1).putBoolean("enabled", true).commit()
    }

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("enabled", enabled && consented).commit()
    }

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
