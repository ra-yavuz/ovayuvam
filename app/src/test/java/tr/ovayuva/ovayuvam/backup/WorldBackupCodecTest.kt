package tr.ovayuva.ovayuvam.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.ovayuva.ovayuvam.domain.GeoPosition
import tr.ovayuva.ovayuvam.domain.RevealCell
import tr.ovayuva.ovayuvam.domain.VisitedCell
import tr.ovayuva.ovayuvam.location.GoalPin

class WorldBackupCodecTest {
    @Test
    fun encryptedBackupRoundTripsWithPassphrase() {
        val backup = WorldBackup(
            visitedCells = listOf(VisitedCell(12, 34, 100L, 200L, 3)),
            revealCells = listOf(RevealCell(13, 35, RevealCell.Kind.Road, 120L, 220L, 2)),
            goal = GoalPin(GeoPosition(41.0, 29.0), 300L),
            exportedMs = 400L,
        )

        val encrypted = WorldBackupCodec.encrypt(backup, "correct horse")
        val restored = WorldBackupCodec.decrypt(encrypted, "correct horse")

        assertNotEquals(String(encrypted), "correct horse")
        assertEquals(backup, restored)
    }

    @Test
    fun encryptedBackupRejectsWrongPassphrase() {
        val backup = WorldBackup(
            visitedCells = listOf(VisitedCell(1, 2, 3L, 4L, 5)),
            revealCells = emptyList(),
            goal = null,
            exportedMs = 6L,
        )

        val encrypted = WorldBackupCodec.encrypt(backup, "correct horse")
        val failed = runCatching { WorldBackupCodec.decrypt(encrypted, "wrong horse") }

        assertTrue(failed.isFailure)
    }
}
