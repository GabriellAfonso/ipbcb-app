package com.ipb.castelobranco.features.gallery.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One entry of `GET api/gallery/trash/`: a delete action (an album with what went with it, or a photo). */
@Serializable
data class GalleryTrashEntryDto(
    val kind: String,
    val id: Long,
    val name: String,
    @SerialName("deleted_at")
    val deletedAt: String,
    @SerialName("deleted_by")
    val deletedBy: String? = null,
    @SerialName("uploaded_by")
    val uploadedBy: String? = null,
    @SerialName("purge_on")
    val purgeOn: String,
    @SerialName("sub_album_count")
    val subAlbumCount: Int = 0,
    @SerialName("photo_count")
    val photoCount: Int = 0,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
)
