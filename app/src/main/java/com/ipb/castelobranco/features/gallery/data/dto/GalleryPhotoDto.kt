package com.ipb.castelobranco.features.gallery.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GalleryPhotoDto(
    val id: Long,
    val name: String,
    val description: String = "",
    @SerialName("album_id")
    val albumId: Long,
    @SerialName("album_name")
    val albumName: String = "",
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("date_taken")
    val dateTaken: String? = null,
    @SerialName("uploaded_at")
    val uploadedAt: String? = null,
    val position: Int = 0,
    /** Stored for the member tags feature; not shown yet. */
    val members: List<GalleryPhotoMemberDto> = emptyList(),
)

@Serializable
data class GalleryPhotoMemberDto(
    val id: Long,
    val name: String,
)
