package com.ipb.castelobranco.features.gallery.domain.model

/** A gallery item: a photo or a video. Videos share the photo's resource, order, trash and tags. */
data class GalleryPhoto(
    val id: Long,
    val name: String,
    val description: String,
    val albumId: Long,
    val albumName: String,
    /** The item's file: the photo, or the video itself. */
    val imageUrl: String,
    /** The preview; a video's poster. */
    val thumbnailUrl: String?,
    val dateTaken: String?,
    val uploadedAt: String?,
    val position: Int,
    /** The people tagged in the photo, by name then id; empty when untagged. */
    val members: List<GalleryMember>,
    val mediaType: MediaType = MediaType.PHOTO,
    /** Videos only. */
    val durationMs: Long? = null,
    /** Bytes of [imageUrl]; `null` for an old photo whose size the server never measured. */
    val fileSize: Long? = null,
) {
    val isVideo: Boolean get() = mediaType == MediaType.VIDEO

    /** Extension of the file on disk, derived from [imageUrl]: `jpg` / `mp4` when unknown. */
    fun fileExtension(): String = when {
        isVideo -> if (imageUrl.endsWith(".$MOV", ignoreCase = true)) MOV else MP4
        imageUrl.endsWith(".png", ignoreCase = true) -> "png"
        imageUrl.endsWith(".webp", ignoreCase = true) -> "webp"
        else -> "jpg"
    }

    /** What the file is, for sharing and saving to the device. */
    fun mimeType(): String = when (fileExtension()) {
        MOV -> "video/quicktime"
        MP4 -> "video/mp4"
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }

    companion object {
        const val MP4 = "mp4"
        const val MOV = "mov"
    }
}

enum class MediaType {
    PHOTO,
    VIDEO,
    ;

    companion object {
        private const val VIDEO_KEY = "video"

        /** The server's `media_type`; anything but `video` (missing, unknown) is a photo. */
        fun fromApi(value: String?): MediaType = if (value == VIDEO_KEY) VIDEO else PHOTO

        fun toApi(type: MediaType): String = if (type == VIDEO) VIDEO_KEY else "photo"
    }
}

data class GalleryMember(val id: Long, val name: String)
