package tr.ovayuva.ovayuvam.map

import kotlin.math.log10
import kotlin.math.roundToInt

object VisitHeat {
    const val MaxOpacity = 0.36f
    private const val MinOpacity = 0.12f
    private val colors = longArrayOf(0xFF66AFBD, 0xFF83B8A0, 0xFFDAAD4B, 0xFFE57970, 0xFFAD4258)

    private fun strength(visits: Int): Double {
        val decades = log10(visits.coerceAtLeast(1).toDouble())
        // Equal space for 1, 10, 100, and 1,000 visits, then a gradual tail with no hard ceiling.
        return if (decades <= 3.0) decades / 4.0
        else 0.75 + 0.25 * (decades - 3.0) / (decades - 2.0)
    }

    fun opacity(zoom: Double, visits: Int): Float {
        if (visits <= 0 || !zoom.isFinite()) return 0f
        val zoomFade = ((14.5 - zoom) / 2.5).coerceIn(0.0, 1.0)
        return (zoomFade * (MinOpacity + (MaxOpacity - MinOpacity) * strength(visits))).toFloat()
    }

    fun color(visits: Int): Long {
        val position = strength(visits) * (colors.size - 1)
        val index = position.toInt().coerceAtMost(colors.lastIndex - 1)
        val fraction = position - index
        fun channel(shift: Int): Long {
            val from = (colors[index] shr shift) and 255
            val to = (colors[index + 1] shr shift) and 255
            return (from + (to - from) * fraction).roundToInt().toLong()
        }
        return 0xFF000000 or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
