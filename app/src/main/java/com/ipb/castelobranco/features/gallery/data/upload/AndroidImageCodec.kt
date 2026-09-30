package com.ipb.castelobranco.features.gallery.data.upload

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Decodes with `BitmapFactory` (HEIF included on API 28+), sampling from the bounds first so a huge
 * image is never decoded whole, then scales, rotates and flips with one [Matrix]. Transparent areas
 * become white — a JPEG has no alpha and Android would paint them black.
 */
class AndroidImageCodec @Inject constructor() : ImageCodec {

    override fun readInfo(file: File): SourceInfo? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val exif = runCatching { ExifInterface(file) }.getOrNull()
        return SourceInfo(
            width = bounds.outWidth,
            height = bounds.outHeight,
            orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                ?: ExifInterface.ORIENTATION_NORMAL,
            mimeType = bounds.outMimeType,
            byteSize = file.length(),
            dateTags = DATE_TAGS.mapNotNull { tag -> exif?.getAttribute(tag)?.let { tag to it } }.toMap(),
        )
    }

    override fun decode(file: File, plan: PreparationPlan.Reencode): DecodedImage? {
        val options = BitmapFactory.Options().apply { inSampleSize = plan.sampleSize }
        val decoded = BitmapFactory.decodeFile(file.path, options) ?: return null
        return try {
            AndroidDecodedImage(transform(decoded, plan))
        } catch (e: OutOfMemoryError) {
            Timber.w(e, "Gallery upload too large to prepare")
            decoded.recycle()
            null
        }
    }

    override fun writeExif(file: File, tags: Map<String, String>) {
        val exif = ExifInterface(file)
        tags.forEach { (tag, value) -> exif.setAttribute(tag, value) }
        exif.saveAttributes()
    }

    private fun transform(source: Bitmap, plan: PreparationPlan.Reencode): Bitmap {
        val swapped = plan.rotationDegrees % HALF_TURN != 0
        val scaledWidth = if (swapped) plan.targetHeight else plan.targetWidth
        val scaledHeight = if (swapped) plan.targetWidth else plan.targetHeight
        val matrix = Matrix().apply {
            postScale(scaledWidth.toFloat() / source.width, scaledHeight.toFloat() / source.height)
            postRotate(plan.rotationDegrees.toFloat())
            if (plan.flipHorizontal) postScale(-1f, 1f)
        }
        val upright = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (upright !== source) source.recycle()
        if (!upright.hasAlpha()) return upright

        val opaque = Bitmap.createBitmap(upright.width, upright.height, Bitmap.Config.ARGB_8888)
        Canvas(opaque).apply {
            drawColor(Color.WHITE)
            drawBitmap(upright, 0f, 0f, null)
        }
        upright.recycle()
        return opaque
    }

    private class AndroidDecodedImage(private val bitmap: Bitmap) : DecodedImage {
        override fun writeJpeg(quality: Int, output: File): Long {
            FileOutputStream(output).use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
            return output.length()
        }

        override fun release() {
            bitmap.recycle()
        }
    }

    companion object {
        private const val HALF_TURN = 180

        /** Carried from the source to the prepared JPEG; the server reads `date_taken` from them. */
        val DATE_TAGS = listOf(
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
            ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_DATETIME,
        )
    }
}
