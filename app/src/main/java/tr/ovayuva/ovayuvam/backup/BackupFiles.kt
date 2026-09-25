package tr.ovayuva.ovayuvam.backup

import org.json.JSONException
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import javax.crypto.AEADBadTagException

object BackupFiles {
    const val MaxBytes = 64 * 1024 * 1024

    fun read(input: InputStream, maxBytes: Int = MaxBytes): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val size = input.read(buffer)
            if (size < 0) break
            require(output.size().toLong() + size <= maxBytes) { "The backup is too large to open safely on this device." }
            output.write(buffer, 0, size)
        }
        require(output.size() > 0) { "This file is empty. Choose the original exported backup." }
        return output.toByteArray()
    }

    fun errorMessage(error: Exception): String = when (error) {
        is AEADBadTagException -> "The passphrase is incorrect, or the backup is damaged. Use the exact export passphrase."
        is SecurityException -> "Access to this file was lost. Cancel and choose the file again."
        is FileNotFoundException -> "The file is unavailable. Download it to this phone, then choose it again."
        is IOException -> "The file could not be read or saved. Check the file provider, connection and free space, then try again."
        is JSONException -> "This is not a complete ovayuvam backup. Choose the original exported file."
        is IllegalArgumentException -> error.message ?: "The backup file or passphrase is not valid."
        else -> "The backup could not be processed. Keep the original file and try again."
    }
}
