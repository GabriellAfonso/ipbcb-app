package com.ipb.castelobranco.features.gallery.domain.manage

import javax.inject.Inject

/** Sends the album, its sub-albums and all their photos to the trash (30 days). */
class DeleteAlbumUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
) {
    suspend operator fun invoke(albumId: Long): Result<Unit> = repository.deleteAlbum(albumId)
}

/** One DELETE per photo, in order; every photo is attempted. A photo already gone counts as done. */
class DeletePhotosUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
) {
    suspend operator fun invoke(photoIds: List<Long>): BatchResult =
        runBatch(photoIds, repository) { id -> repository.deletePhoto(id, syncAfter = false) }
}
