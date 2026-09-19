package tr.ovayuva.ovayuvam.domain

data class WorldSummary(
    val cells: Int,
    val samples: Int,
    val lastSeenMs: Long?,
)

