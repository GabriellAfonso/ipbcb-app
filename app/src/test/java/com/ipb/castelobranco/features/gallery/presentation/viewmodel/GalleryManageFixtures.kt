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
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.tags.ChangePhotoMembersUseCase
import com.ipb.castelobranco.features.gallery.domain.tags.LoadTaggableMembersUseCase
import com.ipb.castelobranco.features.gallery.domain.tags.SetPhotoMembersUseCase
import com.ipb.castelobranco.features.gallery.domain.trash.RestoreTrashItemUseCase
import com.ipb.castelobranco.features.gallery.domain.upload.DismissUploadUseCase
import com.ipb.castelobranco.features.gallery.domain.upload.EnqueueUploadsUseCase
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadRepository
import com.ipb.castelobranco.features.gallery.domain.upload.ObserveUploadsUseCase
import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import com.ipb.castelobranco.features.gallery.domain.upload.PickedMedia
import com.ipb.castelobranco.features.gallery.domain.FakeGalleryMediaSettings
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryMediaSettings
import com.ipb.castelobranco.features.gallery.domain.usecase.GalleryStorageUseCase
import com.ipb.castelobranco.features.gallery.domain.usecase.GalleryVideoSettingsUseCase
import io.mockk.mockk
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoViewerUiState
import com.ipb.castelobranco.features.gallery.presentation.state.ViewerSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** Upload queue in memory: records what was queued and dismissed. */
class FakeUploadRepository : GalleryUploadRepository {
    override val items = MutableStateFlow<List<UploadItem>>(emptyList())
    val enqueued = mutableListOf<Pair<Long, List<String>>>()
    val wifiOnlyBatches = mutableListOf<Boolean>()

    /** What [inspect] reports per source; unknown sources are images of unknown size. */
    val picked = mutableMapOf<String, PickedMedia>()
    val dismissed = mutableListOf<String>()
    var coverResult: Result<File> = Result.success(File("cover.jpg"))

    override suspend fun inspect(sources: List<String>): List<PickedMedia> =
        sources.map { picked[it] ?: PickedMedia(it, "image/jpeg", null) }

    override suspend fun enqueue(albumId: Long, sources: List<String>, videosWifiOnly: Boolean) {
        enqueued += albumId to sources
        wifiOnlyBatches += videosWifiOnly
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

fun manageUseCases(
    repository: GalleryManageRepository,
    uploads: GalleryUploadRepository,
    gallery: GalleryRepository,
) = GalleryManageUseCases(
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
    restore = RestoreTrashItemUseCase(repository, gallery),
    loadTaggableMembers = LoadTaggableMembersUseCase(repository),
    setPhotoMembers = SetPhotoMembersUseCase(repository),
    changePhotoMembers = ChangePhotoMembersUseCase(repository),
)

/** The video switch and the card over [settings]; schedulers are relaxed mocks. */
fun videoSettingsUseCase(settings: GalleryMediaSettings = FakeGalleryMediaSettings()) =
    GalleryVideoSettingsUseCase(settings, mockk(relaxed = true))

/** "Baixar / Apagar galeria" by type, with relaxed collaborators. */
fun storageUseCase(gallery: GalleryRepository) =
    GalleryStorageUseCase(gallery, mockk(relaxed = true), mockk(relaxed = true))

/** Opens a viewer whose id is derived from what it shows, as if each source/photo pair were its own entry. */
fun GalleryViewModel.viewerState(source: ViewerSource, photoId: Long): StateFlow<PhotoViewerUiState> =
    viewerState("$source/$photoId", source, photoId)

fun GalleryViewModel.viewerState(albumId: Long, photoId: Long): StateFlow<PhotoViewerUiState> =
    viewerState(ViewerSource.Album(albumId), photoId)
