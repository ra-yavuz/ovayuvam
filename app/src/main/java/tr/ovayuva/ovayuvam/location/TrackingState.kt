package tr.ovayuva.ovayuvam.location

import android.content.Context
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

    fun setCurrentCell(cell: WorldCell) {
        preferences.edit()
            .putInt("current_x", cell.x)
            .putInt("current_y", cell.y)
            .apply()
    }

    fun clearCurrentCell() {
        preferences.edit()
            .remove("current_x")
            .remove("current_y")
            .apply()
    }
}
