package com.ipb.castelobranco.features.gallery.data

import com.ipb.castelobranco.features.gallery.data.dto.GalleryAlbumDto
import com.ipb.castelobranco.features.gallery.data.dto.GalleryChangesDto
import com.ipb.castelobranco.features.gallery.data.dto.GalleryPhotoDto
import com.ipb.castelobranco.features.gallery.data.dto.toDomain
import com.ipb.castelobranco.features.gallery.domain.model.GalleryAlbum
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto

fun photoDto(
    id: Long,
    albumId: Long = 1L,
    url: String = photoUrl(id),
    position: Int = 0,
    thumbnailUrl: String? = null,
    name: String = "img$id.jpg",
): GalleryPhotoDto = GalleryPhotoDto(
    id = id,
    name = name,
    description = "",
    albumId = albumId,
    albumName = "Álbum $albumId",
    imageUrl = url,
    thumbnailUrl = thumbnailUrl,
    dateTaken = null,
    uploadedAt = "2026-09-25T12:00:00Z",
    position = position,
)

fun albumDto(
    id: Long,
    parentId: Long? = null,
    position: Int = 0,
    coverUrl: String? = null,
    name: String = "Álbum $id",
    eventDate: String? = null,
    description: String = "",
): GalleryAlbumDto = GalleryAlbumDto(
    id = id,
    name = name,
    parentId = parentId,
    description = description,
    eventDate = eventDate,
    coverUrl = coverUrl,
    coverSourceAlbumId = coverUrl?.let { id },
    position = position,
)

fun galleryPhoto(
    id: Long,
    albumId: Long = 1L,
    url: String = photoUrl(id),
    position: Int = 0,
    thumbnailUrl: String? = null,
    name: String = "img$id.jpg",
): GalleryPhoto = photoDto(id, albumId, url, position, thumbnailUrl, name).toDomain()

fun galleryAlbum(
    id: Long,
    parentId: Long? = null,
    position: Int = 0,
    coverUrl: String? = null,
    name: String = "Álbum $id",
    eventDate: String? = null,
    description: String = "",
): GalleryAlbum = albumDto(id, parentId, position, coverUrl, name, eventDate, description).toDomain()

fun changes(
    cursor: String,
    albums: List<GalleryAlbumDto> = emptyList(),
    photos: List<GalleryPhotoDto> = emptyList(),
    deletedAlbumIds: List<Long> = emptyList(),
    deletedPhotoIds: List<Long> = emptyList(),
    fullSyncRequired: Boolean = false,
) = GalleryChangesDto(
    albums = albums,
    photos = photos,
    deletedAlbumIds = deletedAlbumIds,
    deletedPhotoIds = deletedPhotoIds,
    cursor = cursor,
    fullSyncRequired = fullSyncRequired,
)

fun photoUrl(id: Long): String = "https://example.com/ipbcb/media/gallery/$id.jpg"

fun coverUrl(name: String): String = "https://example.com/ipbcb/media/gallery/covers/$name.jpg"

/** A structured error body, as the backend sends it. [extras] are raw JSON fragments. */
fun apiError(errorCode: String, detail: String, vararg extras: Pair<String, String>): String =
    buildString {
        append("""{"error_code":"$errorCode","detail":"$detail"""")
        extras.forEach { (key, json) -> append(""","$key":$json""") }
        append("}")
    }
