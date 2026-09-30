package com.ipb.castelobranco.features.gallery.domain.manage

import com.ipb.castelobranco.features.gallery.domain.tags.ChangePhotoMembersUseCase
import com.ipb.castelobranco.features.gallery.domain.tags.LoadTaggableMembersUseCase
import com.ipb.castelobranco.features.gallery.domain.tags.SetPhotoMembersUseCase
import com.ipb.castelobranco.features.gallery.domain.trash.RestoreTrashItemUseCase
import com.ipb.castelobranco.features.gallery.domain.upload.DismissUploadUseCase
import com.ipb.castelobranco.features.gallery.domain.upload.EnqueueUploadsUseCase
import com.ipb.castelobranco.features.gallery.domain.upload.ObserveUploadsUseCase
import javax.inject.Inject

/** Every management use case the gallery screens need, injected as one. */
class GalleryManageUseCases @Inject constructor(
    val createAlbum: CreateAlbumUseCase,
    val editAlbum: EditAlbumUseCase,
    val moveAlbum: MoveAlbumUseCase,
    val deleteAlbum: DeleteAlbumUseCase,
    val reorder: ReorderUseCase,
    val setCover: SetCoverUseCase,
    val removeCover: RemoveCoverUseCase,
    val editPhoto: EditPhotoUseCase,
    val movePhotos: MovePhotosUseCase,
    val deletePhotos: DeletePhotosUseCase,
    val enqueueUploads: EnqueueUploadsUseCase,
    val dismissUpload: DismissUploadUseCase,
    val observeUploads: ObserveUploadsUseCase,
    val restore: RestoreTrashItemUseCase,
    val loadTaggableMembers: LoadTaggableMembersUseCase,
    val setPhotoMembers: SetPhotoMembersUseCase,
    val changePhotoMembers: ChangePhotoMembersUseCase,
)
