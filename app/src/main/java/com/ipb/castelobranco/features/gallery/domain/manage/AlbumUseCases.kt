package com.ipb.castelobranco.features.gallery.domain.manage

import com.ipb.castelobranco.features.gallery.domain.model.GalleryAlbum
import javax.inject.Inject

class CreateAlbumUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
) {
    suspend operator fun invoke(draft: AlbumDraft): Result<GalleryAlbum> =
        repository.createAlbum(draft.copy(name = draft.name.trim(), description = draft.description.trim()))
}

/** Name, description and event date; a move goes through [MoveAlbumUseCase]. */
class EditAlbumUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
) {
    suspend operator fun invoke(albumId: Long, name: String, description: String, eventDate: String?) =
        repository.editAlbum(
            AlbumEdit(
                albumId = albumId,
                name = Field.Set(name.trim()),
                description = Field.Set(description.trim()),
                eventDate = Field.Set(eventDate),
            )
        )
}

class MoveAlbumUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
) {
    /** [parentId] `null` moves to the root. The album lands last among its new siblings. */
    suspend operator fun invoke(albumId: Long, parentId: Long?): Result<GalleryAlbum> =
        repository.editAlbum(AlbumEdit(albumId = albumId, parentId = Field.Set(parentId)))
}
