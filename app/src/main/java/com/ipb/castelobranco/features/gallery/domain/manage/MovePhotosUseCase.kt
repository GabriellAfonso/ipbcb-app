package com.ipb.castelobranco.features.gallery.domain.manage

import javax.inject.Inject

/** One PATCH per photo, in order; each lands last in [targetAlbumId]. Files never move. */
class MovePhotosUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
) {
    suspend operator fun invoke(photoIds: List<Long>, targetAlbumId: Long): BatchResult =
        runBatch(photoIds, repository) { id ->
            repository.editPhoto(PhotoEdit(photoId = id, albumId = Field.Set(targetAlbumId)), syncAfter = false)
        }
}
