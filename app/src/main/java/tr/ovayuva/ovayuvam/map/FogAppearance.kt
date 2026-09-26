package tr.ovayuva.ovayuvam.map

object FogAppearance {
    const val MinimumOpacity = 0.55f
    const val MaximumOpacity = 0.96f
    const val DefaultOpacity = 0.82f
    const val PlanErasure = 0.40f

    fun opacity(value: Float): Float = if (value.isFinite())
        value.coerceIn(MinimumOpacity, MaximumOpacity) else DefaultOpacity
}
