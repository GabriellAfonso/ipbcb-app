package com.ipb.castelobranco.features.gallery.data.snapshot

import com.ipb.castelobranco.features.gallery.data.dto.GalleryAlbumDto
import com.ipb.castelobranco.features.gallery.data.dto.GalleryPhotoDto
import kotlinx.serialization.Serializable

/** The gallery index on disk. The cursor lives here so it is always written together with the data. */
@Serializable
data class GalleryIndexSnapshot(
    val albums: List<GalleryAlbumDto> = emptyList(),
    val photos: List<GalleryPhotoDto> = emptyList(),
    val cursor: String? = null,
)
