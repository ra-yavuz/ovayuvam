package tr.ovayuva.ovayuvam.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.ovayuva.ovayuvam.domain.GeoPosition
import tr.ovayuva.ovayuvam.domain.RevealCell
import tr.ovayuva.ovayuvam.domain.VisitedCell
import tr.ovayuva.ovayuvam.location.GoalPin
import tr.ovayuva.ovayuvam.location.VisitCount

class WorldBackupCodecTest {
    @Test fun unicodePassphraseAndLargeLegacyWorldRoundTrip() {
        val backup = WorldBackup(
            visitedCells = (0 until 10000).map { VisitedCell(it,34,100L,200L,it+1) },
            revealCells = listOf(RevealCell(13,35,RevealCell.Kind.Core,120L,220L,2)),
            goal = null,
        )
        val passphrase = "\u015fifre-\u00fcber-1234"
        val restored = WorldBackupCodec.decrypt(BackupFiles.read(
            WorldBackupCodec.encrypt(backup,passphrase).inputStream()),passphrase)
        assertEquals(backup,restored)
    }

    @Test
    fun originalBackupPayloadKeepsWorldWithoutInventingVisits() {
        val backup = WorldBackupCodec.fromPlainJson("""{"format":"ovayuvam.world.v1","exportedMs":400,
            "visitedCells":[{"x":12,"y":34,"firstSeenMs":100,"lastSeenMs":200,"samples":999}],
            "revealCells":[{"x":13,"y":35,"kind":0,"firstSeenMs":120,"lastSeenMs":220,"samples":8}],"goal":null}""")
        assertEquals(999, backup.visitedCells.single().samples)
        assertEquals(8, backup.revealCells.single().samples)
        assertTrue(backup.visitCounts.isEmpty())
    }

    @Test
    fun encryptedBackupRoundTripsWithPassphrase() {
        val backup = WorldBackup(
            visitedCells = listOf(VisitedCell(12, 34, 100L, 200L, 3)),
            revealCells = listOf(RevealCell(13, 35, RevealCell.Kind.Road, 120L, 220L, 2)),
            goal = GoalPin(GeoPosition(41.0, 29.0), 300L),
            exportedMs = 400L,
            visitCounts = listOf(VisitCount(12, 34, 7, 100L, 200L)),
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
