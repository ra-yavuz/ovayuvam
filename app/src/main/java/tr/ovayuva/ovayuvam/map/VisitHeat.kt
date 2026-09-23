package tr.ovayuva.ovayuvam.map

object VisitHeat {
    const val MaxOpacity = 0.24f
    fun opacity(zoom: Double): Float = ((12.0 - zoom) / 2.0).coerceIn(0.0, 1.0).toFloat() * MaxOpacity
    fun color(visits: Int): Long = when {
        visits >= 8 -> 0xFFE57970
        visits >= 4 -> 0xFFDAAD4B
        visits >= 2 -> 0xFF83B8A0
        else -> 0xFF66AFBD
    }
}
