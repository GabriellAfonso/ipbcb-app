package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.features.gallery.domain.manage.CreateAlbumUseCase
import com.ipb.castelobranco.features.gallery.domain.manage.DeleteAlbumUseCase
import com.ipb.castelobranco.features.gallery.domain.manage.DeletePhotosUseCase
import com.ipb.castelobranco.features.gallery.domain.manage.EditAlbumUseCase
import com.ipb.castelobranco.features.gallery.domain.manage.EditPhotoUseCase
import com.ipb.castelobranco.features.gallery.domain.manage.GalleryManageRepository
import com.ipb.castelobranco.features.gallery.domain.manage.GalleryManageUseCases
import com.ipb.castelobranco.features.gallery.domain.manage.MoveAlbumUseCase
import com.ipb.castelobranco.features.gallery.domain.manage.MovePhotosUseCase
import com.ipb.castelobranco.features.gallery.domain.manage.RemoveCoverUseCase
import com.ipb.castelobranco.features.gallery.domain.manage.ReorderUseCase
import com.ipb.castelobranco.features.gallery.domain.manage.SetCoverUseCase
import com.ipb.castelobranco.features.gallery.domain.upload.DismissUploadUseCase
import com.ipb.castelobranco.features.gallery.domain.upload.EnqueueUploadsUseCase
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadRepository
import com.ipb.castelobranco.features.gallery.domain.upload.ObserveUploadsUseCase
import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/** Upload queue in memory: records what was queued and dismissed. */
class FakeUploadRepository : GalleryUploadRepository {
    override val items = MutableStateFlow<List<UploadItem>>(emptyList())
    val enqueued = mutableListOf<Pair<Long, List<String>>>()
    val dismissed = mutableListOf<String>()
    var coverResult: Result<File> = Result.success(File("cover.jpg"))

    override suspend fun enqueue(albumId: Long, sources: List<String>) {
        enqueued += albumId to sources
    }

    override suspend fun dismiss(uploadId: String) {
        dismissed += uploadId
        items.value = items.value.filterNot { it.uploadId == uploadId }
    }

    override suspend fun prepareCover(source: String): Result<File> = coverResult

    override suspend fun clear() {
        items.value = emptyList()
    }
}

fun manageUseCases(repository: GalleryManageRepository, uploads: GalleryUploadRepository) = GalleryManageUseCases(
    createAlbum = CreateAlbumUseCase(repository),
    editAlbum = EditAlbumUseCase(repository),
    moveAlbum = MoveAlbumUseCase(repository),
    deleteAlbum = DeleteAlbumUseCase(repository),
    reorder = ReorderUseCase(repository),
    setCover = SetCoverUseCase(repository, uploads),
    removeCover = RemoveCoverUseCase(repository),
    editPhoto = EditPhotoUseCase(repository),
    movePhotos = MovePhotosUseCase(repository),
    deletePhotos = DeletePhotosUseCase(repository),
    enqueueUploads = EnqueueUploadsUseCase(uploads),
    dismissUpload = DismissUploadUseCase(uploads),
    observeUploads = ObserveUploadsUseCase(uploads),
)
