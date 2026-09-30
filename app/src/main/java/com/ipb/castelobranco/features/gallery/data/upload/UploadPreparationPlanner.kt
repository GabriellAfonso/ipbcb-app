package com.ipb.castelobranco.features.gallery.data.upload

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * What to do with a picked image before sending it — pure arithmetic, no Android. Everything is
 * re-encoded as an upright JPEG with its longest side at most [MAX_LONG_SIDE], except a GIF already
 * within the server's limits (sent as is, animation kept).
 */
object UploadPreparationPlanner {
    const val MAX_LONG_SIDE = 4000
    const val MAX_BYTES = 10L * 1024 * 1024
    const val MAX_PIXELS = 50_000_000L
    val QUALITY_LADDER = listOf(90, 85, 80, 75, 70, 60)
    const val GIF_MIME = "image/gif"

    // EXIF orientation values (ExifInterface.ORIENTATION_*).
    private const val FLIP_HORIZONTAL = 2
    private const val ROTATE_180 = 3
    private const val FLIP_VERTICAL = 4
    private const val TRANSPOSE = 5
    private const val ROTATE_90 = 6
    private const val TRANSVERSE = 7
    private const val ROTATE_270 = 8
    private const val QUARTER_TURN = 90
    private const val HALF_TURN = 180
    private const val THREE_QUARTER_TURN = 270

    fun plan(width: Int, height: Int, exifOrientation: Int, mimeType: String?, byteSize: Long): PreparationPlan {
        if (mimeType == GIF_MIME && byteSize <= MAX_BYTES && width.toLong() * height <= MAX_PIXELS) {
            return PreparationPlan.SendAsIs
        }
        val (rotation, flip) = orientation(exifOrientation)
        val swapped = rotation == QUARTER_TURN || rotation == THREE_QUARTER_TURN
        val orientedWidth = if (swapped) height else width
        val orientedHeight = if (swapped) width else height
        val longSide = max(orientedWidth, orientedHeight)
        val scale = if (longSide > MAX_LONG_SIDE) MAX_LONG_SIDE.toDouble() / longSide else 1.0
        val targetWidth = (orientedWidth * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (orientedHeight * scale).roundToInt().coerceAtLeast(1)
        return PreparationPlan.Reencode(
            sampleSize = sampleSize(longSide, max(targetWidth, targetHeight)),
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            rotationDegrees = rotation,
            flipHorizontal = flip,
        )
    }

    /** Rotation, then horizontal flip, that turns the stored pixels upright. */
    private fun orientation(exif: Int): Pair<Int, Boolean> = when (exif) {
        FLIP_HORIZONTAL -> 0 to true
        ROTATE_180 -> HALF_TURN to false
        FLIP_VERTICAL -> HALF_TURN to true
        TRANSPOSE -> QUARTER_TURN to true
        ROTATE_90 -> QUARTER_TURN to false
        TRANSVERSE -> THREE_QUARTER_TURN to true
        ROTATE_270 -> THREE_QUARTER_TURN to false
        else -> 0 to false
    }

    /** Largest power of two that still decodes at least [target] pixels on the long side. */
    private fun sampleSize(sourceLong: Int, target: Int): Int {
        var sample = 1
        while (sourceLong / (sample * 2) >= target) sample *= 2
        return sample
    }
}

sealed interface PreparationPlan {
    data object SendAsIs : PreparationPlan

    /** Decode with [sampleSize], scale to the target, rotate then flip, encode as JPEG. */
    data class Reencode(
        val sampleSize: Int,
        val targetWidth: Int,
        val targetHeight: Int,
        val rotationDegrees: Int,
        val flipHorizontal: Boolean,
    ) : PreparationPlan
}
