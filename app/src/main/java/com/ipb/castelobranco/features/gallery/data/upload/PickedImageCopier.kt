package com.ipb.castelobranco.features.gallery.data.upload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * Copies picked images into app storage at once: a picker URI's grant does not survive the process,
 * and the queue may run long after it. Copy only — decoding happens in the worker.
 */
class PickedImageCopier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaStore: GalleryMediaStore,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /** A copy of one picked image, or `null` when it could not be read. */
    data class Copy(val uploadId: String, val fileName: String, val displayName: String)

    /** [source] is a content URI as a string. A fresh upload id is generated for it, once. */
    suspend fun copy(source: String): Copy? = withContext(ioDispatcher) {
        val uploadId = UUID.randomUUID().toString()
        try {
            val uri = Uri.parse(source)
            val resolver = context.contentResolver
            val extension = extensionOf(resolver.getType(uri))
            val fileName = GalleryImagePreparer.rawFileName(uploadId, extension)
            val input = resolver.openInputStream(uri) ?: return@withContext null
            input.use { mediaStore.saveUpload(fileName, it) }
            Copy(uploadId, fileName, displayNameOf(uri) ?: defaultName())
        } catch (e: Exception) {
            Timber.w(e, "Picked image could not be copied")
            null
        }
    }

    private fun displayNameOf(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.ifBlank { null } else null
        }
    }.getOrNull()

    private fun defaultName(): String =
        "$DEFAULT_NAME_PREFIX${SimpleDateFormat(DEFAULT_NAME_PATTERN, Locale.US).format(Date())}.jpg"

    companion object {
        private const val DEFAULT_NAME_PREFIX = "foto_"
        private const val DEFAULT_NAME_PATTERN = "yyyyMMdd_HHmmss"

        fun extensionOf(mimeType: String?): String = when (mimeType?.lowercase()) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/heic", "image/heif" -> "heic"
            else -> "img"
        }
    }
}
