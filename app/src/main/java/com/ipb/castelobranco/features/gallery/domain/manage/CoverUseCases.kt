package com.ipb.castelobranco.features.gallery.domain.manage

import com.ipb.castelobranco.features.gallery.domain.model.GalleryAlbum
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadRepository
import javax.inject.Inject

class SetCoverUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
    private val uploads: GalleryUploadRepository,
) {
    /** "Trocar capa": an image picked on the phone ([source] is its URI), prepared like an upload. */
    suspend fun fromPicked(albumId: Long, source: String): Result<GalleryAlbum> =
        uploads.prepareCover(source).fold(
            onSuccess = { file -> repository.setCover(albumId, file, deleteAfter = true) },
            onFailure = { Result.failure(it) },
        )

    /** "Usar como capa": the photo's original, downloaded first when not on the device. */
    suspend fun fromPhoto(albumId: Long, photoId: Long): Result<GalleryAlbum> =
        repository.originalForCover(photoId).fold(
            onSuccess = { source -> repository.setCover(albumId, source.file, deleteAfter = source.isTemp) },
            onFailure = { Result.failure(it) },
        )
}

class RemoveCoverUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
) {
    suspend operator fun invoke(albumId: Long): Result<Unit> = repository.removeCover(albumId)
}
