package tr.ovayuva.ovayuvam.storage

import android.content.Context
import tr.ovayuva.ovayuvam.map.FogAppearance

class MapPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("map-appearance", Context.MODE_PRIVATE)
    var showArea: Boolean
        get() = prefs.getBoolean("show-area", false)
        set(value) { prefs.edit().putBoolean("show-area", value).apply() }
    var fogOpacity: Float
        get() = FogAppearance.opacity(prefs.getFloat("fog-opacity", FogAppearance.DefaultOpacity))
        set(value) { prefs.edit().putFloat("fog-opacity", FogAppearance.opacity(value)).apply() }
}
