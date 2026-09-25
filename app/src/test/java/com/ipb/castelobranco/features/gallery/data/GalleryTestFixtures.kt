package com.ipb.castelobranco.features.gallery.data

import com.ipb.castelobranco.features.gallery.data.dto.GalleryPhotoDto

fun galleryPhoto(
    id: Long,
    albumId: Long = 1L,
    url: String = photoUrl(id),
    albumName: String = "Álbum $albumId",
): GalleryPhotoDto = GalleryPhotoDto(
    id = id,
    name = "img$id.jpg",
    description = "",
    albumId = albumId,
    albumName = albumName,
    imageUrl = url,
    dateTaken = null,
    uploadedAt = "2026-09-25T12:00:00Z",
)

fun photoUrl(id: Long): String = "https://example.com/ipbcb/media/gallery/$id.jpg"
