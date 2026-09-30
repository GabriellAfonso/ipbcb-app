package com.ipb.castelobranco.features.gallery.data.upload

import java.io.File

/**
 * The Android side of preparing an upload (decode, scale, rotate, encode, EXIF), behind an
 * interface so [GalleryImagePreparer]'s rules are tested without real bitmaps.
 */
interface ImageCodec {
    /** Size, orientation and capture-date tags; `null` when the file is not a readable image. */
    fun readInfo(file: File): SourceInfo?

    /** Decodes [file] and applies [plan]; `null` when it cannot be decoded. */
    fun decode(file: File, plan: PreparationPlan.Reencode): DecodedImage?

    /** Writes exactly [tags] (EXIF tag name → value) into the JPEG at [file]. */
    fun writeExif(file: File, tags: Map<String, String>)
}

/** An image in memory, ready to encode. Always [release]d. */
interface DecodedImage {
    /** Encodes as JPEG at [quality] into [output]; returns the file size in bytes. */
    fun writeJpeg(quality: Int, output: File): Long
    fun release()
}

data class SourceInfo(
    val width: Int,
    val height: Int,
    /** EXIF orientation, 1 (normal) when absent. */
    val orientation: Int,
    val mimeType: String?,
    val byteSize: Long,
    /** The capture-date tags present in the source, to carry over. */
    val dateTags: Map<String, String>,
)
