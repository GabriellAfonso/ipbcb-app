package com.ipb.castelobranco.features.gallery.domain.model

data class GalleryPhoto(
    val id: Long,
    val name: String,
    val description: String,
    val albumId: Long,
    val albumName: String,
    val imageUrl: String,
    val thumbnailUrl: String?,
    val dateTaken: String?,
    val uploadedAt: String?,
    val position: Int,
    /** Stored for the member tags feature; not shown yet. */
    val members: List<GalleryMember>,
) {
    /** Extension of the original on disk, derived from [imageUrl]; `jpg` when unknown. */
    fun fileExtension(): String = when {
        imageUrl.endsWith(".png", ignoreCase = true) -> "png"
        imageUrl.endsWith(".webp", ignoreCase = true) -> "webp"
        else -> "jpg"
    }
}

data class GalleryMember(val id: Long, val name: String)
