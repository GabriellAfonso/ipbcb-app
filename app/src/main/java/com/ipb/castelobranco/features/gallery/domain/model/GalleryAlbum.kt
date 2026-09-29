package com.ipb.castelobranco.features.gallery.domain.model

/**
 * A node of the album tree. [coverUrl] is the cover the server resolved for it (its own or a
 * descendant's); `null` means no cover anywhere below.
 */
data class GalleryAlbum(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val description: String,
    /** `yyyy-MM-dd`, as the server sends it. */
    val eventDate: String?,
    val coverUrl: String?,
    val coverSourceAlbumId: Long?,
    val position: Int,
)
