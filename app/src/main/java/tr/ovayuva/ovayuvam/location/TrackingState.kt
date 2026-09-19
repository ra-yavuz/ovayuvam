package tr.ovayuva.ovayuvam.location

import android.content.Context

class TrackingState(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("tracking", Context.MODE_PRIVATE)

    fun isTracking(): Boolean = preferences.getBoolean("active", false)

    fun setTracking(active: Boolean) {
        preferences.edit().putBoolean("active", active).apply()
    }
}
