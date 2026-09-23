package tr.ovayuva.ovayuvam.location

import android.content.Context

class VisitPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("visit-settings", Context.MODE_PRIVATE)
    var showHeat: Boolean
        get() = prefs.getBoolean("show-heat", true)
        set(value) { prefs.edit().putBoolean("show-heat", value).apply() }
    var stayRadius: Int
        get() = prefs.getInt("stay-radius", 150).coerceIn(150, 1000)
        set(value) { prefs.edit().putInt("stay-radius", value.coerceIn(150, 1000)).apply() }
}
