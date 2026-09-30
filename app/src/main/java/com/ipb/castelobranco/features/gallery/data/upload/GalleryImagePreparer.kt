package com.ipb.castelobranco.features.gallery.data.upload

import androidx.exifinterface.media.ExifInterface
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import java.io.File
import javax.inject.Inject

/**
 * Turns a picked copy into what is sent: an upright JPEG, longest side ≤ 4000 px, at most 10 MB,
 * with the capture date of the source so the server still reads `date_taken` — or a GIF within the
 * server's limits, untouched. The raw copy is deleted once the prepared file exists.
 */
class GalleryImagePreparer @Inject constructor(
    private val codec: ImageCodec,
    private val mediaStore: GalleryMediaStore,
) {

    fun prepare(item: UploadItem): PrepareResult {
        val source = mediaStore.uploadFile(item.fileName)
        val info = codec.readInfo(source) ?: return PrepareResult.Unreadable
        val plan = with(info) { UploadPreparationPlanner.plan(width, height, orientation, mimeType, byteSize) }
        return when (plan) {
            PreparationPlan.SendAsIs -> PrepareResult.Ready(
                file = source,
                displayName = withExtension(item.displayName, GIF_EXTENSION),
            )
            is PreparationPlan.Reencode -> reencode(item, source, info, plan)
        }
    }

    private fun reencode(
        item: UploadItem,
        source: File,
        info: SourceInfo,
        plan: PreparationPlan.Reencode,
    ): PrepareResult {
        val image = codec.decode(source, plan) ?: return PrepareResult.Unreadable
        val output = mediaStore.uploadFile(preparedFileName(item.uploadId))
        try {
            for (quality in UploadPreparationPlanner.QUALITY_LADDER) {
                if (image.writeJpeg(quality, output) <= UploadPreparationPlanner.MAX_BYTES) {
                    codec.writeExif(output, info.dateTags + (ExifInterface.TAG_ORIENTATION to ORIENTATION_NORMAL))
                    if (source != output) source.delete()
                    return PrepareResult.Ready(output, withExtension(item.displayName, JPEG_EXTENSION))
                }
            }
        } finally {
            image.release()
        }
        output.delete()
        return PrepareResult.TooLarge
    }

    companion object {
        const val JPEG_EXTENSION = "jpg"
        const val GIF_EXTENSION = "gif"
        private val ORIENTATION_NORMAL = ExifInterface.ORIENTATION_NORMAL.toString()

        /** The prepared file of an upload; the raw copy uses a different name ([rawFileName]). */
        fun preparedFileName(uploadId: String) = "$uploadId.$JPEG_EXTENSION"

        fun rawFileName(uploadId: String, extension: String) = "$uploadId-src.$extension"

        fun withExtension(name: String, extension: String): String =
            "${name.substringBeforeLast('.').ifBlank { name }}.$extension"
    }
}

sealed interface PrepareResult {
    /** [file] is what is sent; [displayName] the photo's name on the server. */
    data class Ready(val file: File, val displayName: String) : PrepareResult
    data object Unreadable : PrepareResult
    data object TooLarge : PrepareResult
}
