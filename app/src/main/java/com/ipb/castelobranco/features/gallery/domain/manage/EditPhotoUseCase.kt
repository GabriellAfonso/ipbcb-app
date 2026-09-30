package com.ipb.castelobranco.features.gallery.domain.manage

import javax.inject.Inject

/** Name, description and date taken (`yyyy-MM-dd`, `null` clears it). */
class EditPhotoUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
) {
    suspend operator fun invoke(photoId: Long, name: String, description: String, dateTaken: String?) =
        repository.editPhoto(
            PhotoEdit(
                photoId = photoId,
                name = Field.Set(name.trim()),
                description = Field.Set(description.trim()),
                dateTaken = Field.Set(dateTaken),
            )
        )
}
