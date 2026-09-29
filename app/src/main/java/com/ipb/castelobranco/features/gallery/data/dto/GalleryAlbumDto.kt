package com.ipb.castelobranco.features.gallery.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GalleryAlbumDto(
    val id: Long,
    val name: String,
    @SerialName("parent_id")
    val parentId: Long? = null,
    val description: String = "",
    @SerialName("event_date")
    val eventDate: String? = null,
    @SerialName("cover_url")
    val coverUrl: String? = null,
    @SerialName("cover_source_album_id")
    val coverSourceAlbumId: Long? = null,
    val position: Int = 0,
)
