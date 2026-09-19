package tr.ovayuva.ovayuvam.domain

data class VisitedCell(
    val x: Int,
    val y: Int,
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val samples: Int,
)

