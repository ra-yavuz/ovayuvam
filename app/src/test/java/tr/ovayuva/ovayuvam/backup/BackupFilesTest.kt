package tr.ovayuva.ovayuvam.backup

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import javax.crypto.AEADBadTagException

class BackupFilesTest {
    @Test fun readsAProviderStreamEvenWhenItReturnsSmallChunks() {
        val bytes = ByteArray(20000) { (it % 127).toByte() }
        val stream = object : ByteArrayInputStream(bytes) {
            override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b,off,minOf(len,7))
        }
        assertArrayEquals(bytes,BackupFiles.read(stream))
    }

    @Test fun emptyAndOversizedFilesGiveActionableErrors() {
        val empty = runCatching { BackupFiles.read(ByteArrayInputStream(byteArrayOf())) }.exceptionOrNull()!!
        assertTrue(empty.message!!.contains("empty"))
        val large = runCatching { BackupFiles.read(ByteArrayInputStream(ByteArray(100)),99) }.exceptionOrNull()!!
        assertTrue(large.message!!.contains("too large"))
        assertEquals(100,BackupFiles.read(ByteArrayInputStream(ByteArray(100)),100).size)
    }

    @Test fun permissionAndDecryptionErrorsExplainTheNextStep() {
        assertTrue(BackupFiles.errorMessage(SecurityException()).contains("choose the file again"))
        assertTrue(BackupFiles.errorMessage(FileNotFoundException()).contains("Download it"))
        assertTrue(BackupFiles.errorMessage(AEADBadTagException()).contains("exact export passphrase"))
    }
}
