package tr.ovayuva.ovayuvam.ui

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import tr.ovayuva.ovayuvam.R
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ShareWorld {
    const val Website = "https://ovayuva.tr/yuvam/"

    suspend fun capture(activity: Activity): Bitmap = suspendCancellableCoroutine { continuation ->
        val decor = activity.window.decorView
        val bars = ViewCompat.getRootWindowInsets(decor)?.getInsets(WindowInsetsCompat.Type.systemBars())
        val rect = Rect(bars?.left ?: 0, bars?.top ?: 0,
            decor.width - (bars?.right ?: 0), decor.height - (bars?.bottom ?: 0))
        if (rect.width() <= 0 || rect.height() <= 0) {
            continuation.resumeWithException(IllegalStateException("Map has no size"))
            return@suspendCancellableCoroutine
        }
        val bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        try {
            // PixelCopy includes the real map texture and the Compose fog, unlike View.draw().
            PixelCopy.request(activity.window, rect, bitmap, { result ->
                if (!continuation.isActive) bitmap.recycle()
                else if (result == PixelCopy.SUCCESS) continuation.resume(bitmap) { _, value, _ -> value.recycle() }
                else {
                    bitmap.recycle()
                    continuation.resumeWithException(IllegalStateException("Map capture failed: $result"))
                }
            }, Handler(Looper.getMainLooper()))
        } catch (error: Exception) {
            bitmap.recycle()
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }

    suspend fun intent(context: Context, bitmap: Bitmap): Intent {
        val file = try { withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "world-shares").apply { mkdirs() }
            // Keep recent shares available to receiving apps; never touch backups.
            directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 7L * 86400_000 }
                ?.forEach { it.delete() }
            File.createTempFile("ovayuvam-", ".png", directory).also { target ->
                target.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            }
        } } finally { bitmap.recycle() }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.shares", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_text, Website))
            clipData = ClipData.newUri(context.contentResolver, context.getString(R.string.share_world), uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
