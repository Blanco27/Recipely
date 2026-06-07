package com.nwe.recipely.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream

/**
 * Manages recipe images inside app-internal storage (filesDir/images).
 * The DB stores only absolute file paths returned by this class.
 */
class ImageStore(private val context: Context) {

    private val imagesDir: File
        get() = File(context.filesDir, "images").apply { mkdirs() }

    private fun newImageFile(): File = File(imagesDir, "img_${System.nanoTime()}.jpg")

    /** Copies the content at [uri] into internal storage. Returns the new file path, or null on failure. */
    fun importFromUri(uri: Uri): String? = try {
        val target = newImageFile()
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input)
            target.outputStream().use { output -> input.copyTo(output) }
        }
        target.absolutePath
    } catch (e: Exception) {
        null
    }

    /** Copies an arbitrary [input] stream (e.g. a ZIP entry) into internal storage. Returns the new path, or null on failure. */
    fun importFromStream(input: InputStream): String? = try {
        val target = newImageFile()
        target.outputStream().use { output -> input.copyTo(output) }
        target.absolutePath
    } catch (e: Exception) {
        null
    }

    /**
     * Creates an empty target file for camera capture and returns the content [Uri] (for the
     * TakePicture contract) together with the absolute path to persist once capture succeeds.
     */
    fun createCameraTarget(): Pair<Uri, String> {
        val file = newImageFile()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return uri to file.absolutePath
    }

    /** Deletes the file at [path] if it exists. No-op for null. */
    fun delete(path: String?) {
        if (path.isNullOrEmpty()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
