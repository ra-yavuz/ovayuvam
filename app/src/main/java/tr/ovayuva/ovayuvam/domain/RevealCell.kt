package tr.ovayuva.ovayuvam.domain

data class RevealCell(
    val x: Int,
    val y: Int,
    val kind: Kind,
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val samples: Int,
) {
    enum class Kind(val id: Int) {
        Core(0),
        Road(1);

        companion object {
            fun fromId(id: Int): Kind = entries.firstOrNull { it.id == id } ?: Core
        }
    }
}
