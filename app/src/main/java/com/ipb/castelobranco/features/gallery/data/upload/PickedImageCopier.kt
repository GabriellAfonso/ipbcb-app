package com.ipb.castelobranco.features.gallery.data.upload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.domain.upload.PickedMedia
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
 * Copies picked images and videos into app storage at once: a picker URI's grant does not survive the
 * process, and the queue may run long after it. Copy only — decoding and probing happen elsewhere.
 */
class PickedImageCopier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaStore: GalleryMediaStore,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /** A copy of one picked file, or `null` when it could not be read. */
    data class Copy(
        val uploadId: String,
        val fileName: String,
        val displayName: String,
        val mimeType: String? = null,
    ) {
        val isVideo: Boolean get() = mimeType?.startsWith(VIDEO_PREFIX) == true
    }

    /** [source] is a content URI as a string. A fresh upload id is generated for it, once. */
    suspend fun copy(source: String): Copy? = withContext(ioDispatcher) {
        val uploadId = UUID.randomUUID().toString()
        try {
            val uri = Uri.parse(source)
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri)
            val fileName = GalleryImagePreparer.rawFileName(uploadId, extensionOf(mimeType))
            val input = resolver.openInputStream(uri) ?: return@withContext null
            input.use { mediaStore.saveUpload(fileName, it) }
            val isVideo = mimeType?.startsWith(VIDEO_PREFIX) == true
            Copy(uploadId, fileName, displayNameOf(uri) ?: defaultName(isVideo), mimeType)
        } catch (e: Exception) {
            Timber.w(e, "Picked file could not be copied")
            null
        }
    }

    /** The type and size the picker reports, without copying anything. */
    suspend fun inspect(source: String): PickedMedia = withContext(ioDispatcher) {
        val uri = Uri.parse(source)
        val resolver = context.contentResolver
        val size = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
            }
        }.getOrNull()
        PickedMedia(source, runCatching { resolver.getType(uri) }.getOrNull(), size)
    }

    private fun displayNameOf(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.ifBlank { null } else null
        }
    }.getOrNull()

    private fun defaultName(isVideo: Boolean): String {
        val stamp = SimpleDateFormat(DEFAULT_NAME_PATTERN, Locale.US).format(Date())
        return if (isVideo) "$DEFAULT_VIDEO_PREFIX$stamp.mp4" else "$DEFAULT_NAME_PREFIX$stamp.jpg"
    }

    companion object {
        private const val DEFAULT_NAME_PREFIX = "foto_"
        private const val DEFAULT_VIDEO_PREFIX = "video_"
        private const val DEFAULT_NAME_PATTERN = "yyyyMMdd_HHmmss"
        private const val VIDEO_PREFIX = "video/"

        fun extensionOf(mimeType: String?): String = when (mimeType?.lowercase()) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/heic", "image/heif" -> "heic"
            "video/mp4" -> "mp4"
            "video/quicktime" -> "mov"
            else -> if (mimeType?.startsWith(VIDEO_PREFIX) == true) "video" else "img"
        }
    }
}
