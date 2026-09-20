package tr.ovayuva.ovayuvam.location

import android.content.Context
import tr.ovayuva.ovayuvam.domain.GeoPosition

data class GoalPin(
    val position: GeoPosition,
    val createdMs: Long,
)

class GoalState(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("goal", Context.MODE_PRIVATE)

    fun goal(): GoalPin? {
        if (!preferences.contains("goal_lat") || !preferences.contains("goal_lon")) return null
        return GoalPin(
            position = GeoPosition(
                latitude = Double.fromBits(preferences.getLong("goal_lat", 0L)),
                longitude = Double.fromBits(preferences.getLong("goal_lon", 0L)),
            ),
            createdMs = preferences.getLong("goal_created_ms", 0L),
        )
    }

    fun setGoal(position: GeoPosition, createdMs: Long = System.currentTimeMillis()): GoalPin {
        preferences.edit()
            .putLong("goal_lat", position.latitude.toRawBits())
            .putLong("goal_lon", position.longitude.toRawBits())
            .putLong("goal_created_ms", createdMs)
            .apply()
        return GoalPin(position, createdMs)
    }

    fun clearGoal() {
        preferences.edit()
            .remove("goal_lat")
            .remove("goal_lon")
            .remove("goal_created_ms")
            .apply()
    }
}
