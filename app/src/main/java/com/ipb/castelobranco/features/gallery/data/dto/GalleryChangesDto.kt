package com.ipb.castelobranco.features.gallery.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One answer of `GET api/gallery/changes/`. [cursor] is opaque: stored and sent back, never parsed. */
@Serializable
data class GalleryChangesDto(
    val albums: List<GalleryAlbumDto> = emptyList(),
    val photos: List<GalleryPhotoDto> = emptyList(),
    @SerialName("deleted_album_ids")
    val deletedAlbumIds: List<Long> = emptyList(),
    @SerialName("deleted_photo_ids")
    val deletedPhotoIds: List<Long> = emptyList(),
    val cursor: String,
    @SerialName("full_sync_required")
    val fullSyncRequired: Boolean = false,
)
