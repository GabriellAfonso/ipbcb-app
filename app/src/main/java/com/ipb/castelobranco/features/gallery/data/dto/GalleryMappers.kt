package com.ipb.castelobranco.features.gallery.data.dto

import com.ipb.castelobranco.features.gallery.data.snapshot.GalleryIndexSnapshot
import com.ipb.castelobranco.features.gallery.domain.model.GalleryAlbum
import com.ipb.castelobranco.features.gallery.domain.model.GalleryDelta
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryMember
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto

fun GalleryAlbumDto.toDomain() = GalleryAlbum(
    id = id,
    name = name,
    parentId = parentId,
    description = description,
    eventDate = eventDate,
    coverUrl = coverUrl,
    coverSourceAlbumId = coverSourceAlbumId,
    position = position,
)

fun GalleryAlbum.toDto() = GalleryAlbumDto(
    id = id,
    name = name,
    parentId = parentId,
    description = description,
    eventDate = eventDate,
    coverUrl = coverUrl,
    coverSourceAlbumId = coverSourceAlbumId,
    position = position,
)

fun GalleryPhotoDto.toDomain() = GalleryPhoto(
    id = id,
    name = name,
    description = description,
    albumId = albumId,
    albumName = albumName,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    dateTaken = dateTaken,
    uploadedAt = uploadedAt,
    position = position,
    members = members.map { GalleryMember(it.id, it.name) },
)

fun GalleryPhoto.toDto() = GalleryPhotoDto(
    id = id,
    name = name,
    description = description,
    albumId = albumId,
    albumName = albumName,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    dateTaken = dateTaken,
    uploadedAt = uploadedAt,
    position = position,
    members = members.map { GalleryPhotoMemberDto(it.id, it.name) },
)

fun GalleryChangesDto.toDelta() = GalleryDelta(
    albums = albums.map { it.toDomain() },
    photos = photos.map { it.toDomain() },
    deletedAlbumIds = deletedAlbumIds.toSet(),
    deletedPhotoIds = deletedPhotoIds.toSet(),
    cursor = cursor,
)

fun GalleryIndexSnapshot.toDomain() = GalleryIndex(
    albums = albums.associate { it.id to it.toDomain() },
    photos = photos.associate { it.id to it.toDomain() },
    cursor = cursor,
)

fun GalleryIndex.toSnapshot() = GalleryIndexSnapshot(
    albums = albums.values.map { it.toDto() },
    photos = photos.values.map { it.toDto() },
    cursor = cursor,
)
