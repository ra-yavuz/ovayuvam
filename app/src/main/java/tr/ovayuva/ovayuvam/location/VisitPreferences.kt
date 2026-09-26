package tr.ovayuva.ovayuvam.location

import android.content.Context

class VisitPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("visit-settings", Context.MODE_PRIVATE)
    var showHeat: Boolean
        get() = prefs.getBoolean("show-heat", true)
        set(value) { prefs.edit().putBoolean("show-heat", value).apply() }
    val stayRadius: Int get() = 150
}
