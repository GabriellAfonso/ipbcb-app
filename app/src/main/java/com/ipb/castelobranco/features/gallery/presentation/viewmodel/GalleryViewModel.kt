package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil.ImageLoader
import com.ipb.castelobranco.core.data.NetworkConnectivityObserver
import com.ipb.castelobranco.core.di.DefaultDispatcher
import com.ipb.castelobranco.features.gallery.data.work.GalleryDownloadWorker
import com.ipb.castelobranco.features.gallery.di.GalleryThumbnailLoader
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GalleryTree
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.usecase.GalleryAutoDownloadUseCase
import com.ipb.castelobranco.features.gallery.domain.usecase.SyncGalleryUseCase
import com.ipb.castelobranco.features.gallery.presentation.state.AlbumUiState
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryDownloadState
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryMessage
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryRootUiState
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoViewerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Graph-scoped: one instance serves the root, every album entry on the back stack and the viewer.
 * Per-album and per-viewer state are memoized by key.
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: GalleryRepository,
    private val syncGallery: SyncGalleryUseCase,
    private val autoDownload: GalleryAutoDownloadUseCase,
    connectivityObserver: NetworkConnectivityObserver,
    workManager: WorkManager,
    @param:GalleryThumbnailLoader val previewLoader: ImageLoader,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private data class LocalView(val local: GalleryLocalState, val tree: GalleryTree?)

    /** The local copy with its tree, built once per change off the main thread. */
    private val view: StateFlow<LocalView?> = repository.localState
        .map { local -> LocalView(local, local.index?.let(::GalleryTree)) }
        .flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _message = MutableStateFlow<GalleryMessage?>(null)

    /** Held until a screen shows it, so it survives the back-stack unwind of a removed album. */
    val message: StateFlow<GalleryMessage?> = _message.asStateFlow()

    private val isOnWifi: StateFlow<Boolean> = connectivityObserver.isOnWifi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    private val downloadState: StateFlow<GalleryDownloadState> = workManager
        .getWorkInfosForUniqueWorkFlow(GalleryDownloadWorker.WORK_NAME)
        .map { infos -> infos.firstOrNull().toDownloadState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GalleryDownloadState())

    val rootState: StateFlow<GalleryRootUiState> = combine(
        view,
        repository.syncStatus,
        downloadState,
        isOnWifi,
    ) { view, status, download, wifi ->
        val index = view?.local?.index
        val isLoading = view == null || (index == null && (!status.hasAnswered || status.isRunning))
        val error = status.lastError.takeIf { index == null && !isLoading }
        GalleryRootUiState(
            isLoading = isLoading,
            hasIndex = index != null,
            albums = view?.tree?.roots()?.map { GalleryUiMapper.albumTile(it, view.local) }.orEmpty(),
            download = download,
            isOnWifi = wifi,
            syncError = error?.let(GalleryUiMapper::syncErrorMessage),
            syncErrorCode = error?.let(GalleryUiMapper::errorCode),
            showMembersOnlyNotice = index != null && GalleryUiMapper.isForbidden(status.lastError),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GalleryRootUiState())

    private val albumStates = mutableMapOf<Long, StateFlow<AlbumUiState>>()
    private val viewers = mutableMapOf<Pair<Long, Long>, Viewer>()

    init {
        // Abrir a galeria é um dos gatilhos do sync.
        sync()
    }

    fun albumState(albumId: Long): StateFlow<AlbumUiState> = albumStates.getOrPut(albumId) {
        var wasPresent = false
        view.filterNotNull()
            .map { view ->
                val state = GalleryUiMapper.albumState(albumId, view.local, view.tree)
                if (state.isRemoved && wasPresent) post(GalleryMessage.AlbumRemoved)
                if (!state.isLoading && !state.isRemoved) wasPresent = true
                state
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AlbumUiState())
    }

    fun viewerState(albumId: Long, photoId: Long): StateFlow<PhotoViewerUiState> =
        viewers.getOrPut(albumId to photoId) { Viewer(albumId, photoId) }.state

    /** The pager settled on [currentPhotoId]; used to pick the next photo if this one leaves. */
    fun onPageChanged(albumId: Long, openedPhotoId: Long, currentPhotoId: Long) {
        viewers[albumId to openedPhotoId]?.current?.value = currentPhotoId
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun retrySync() = sync()

    fun downloadWithMobileData() {
        autoDownload.enqueueAnyNetwork()
    }

    /** Nova tentativa após falha — REPLACE para descartar o WorkInfo com erro. */
    fun retryDownload() {
        autoDownload.enqueueWifiOnly(replaceExisting = true)
    }

    private fun sync() {
        viewModelScope.launch { syncGallery() }
    }

    private fun post(message: GalleryMessage) {
        _message.value = message
    }

    /**
     * One open viewer. It follows the photo on screen; when that photo leaves the album it moves to
     * the next one (the previous one when it was the last) and posts why.
     */
    private inner class Viewer(private val albumId: Long, openedPhotoId: Long) {
        val current = MutableStateFlow(openedPhotoId)
        private var lastIds: List<Long> = emptyList()

        val state: StateFlow<PhotoViewerUiState> = combine(view.filterNotNull(), current) { view, currentId ->
            val tree = view.tree ?: return@combine PhotoViewerUiState(isLoading = true)
            val photos = tree.photosOf(albumId)
            val ids = photos.map { it.id }
            var index = ids.indexOf(currentId)

            if (index < 0) {
                val left = lastIds.isNotEmpty() && currentId in lastIds
                if (left) {
                    val stillThere = view.local.index?.photos?.containsKey(currentId) == true
                    post(if (stillThere) GalleryMessage.PhotoMoved else GalleryMessage.PhotoRemoved)
                }
                if (ids.isEmpty()) {
                    lastIds = ids
                    return@combine PhotoViewerUiState(isLoading = false, isClosed = true)
                }
                val position = lastIds.indexOf(currentId)
                val next = lastIds.drop(position + 1).firstOrNull { it in ids }
                    ?: lastIds.take(position.coerceAtLeast(0)).lastOrNull { it in ids }
                    ?: ids.first()
                index = ids.indexOf(next)
                current.value = next
            }
            lastIds = ids
            PhotoViewerUiState(
                isLoading = false,
                photos = photos.map { GalleryUiMapper.viewerPhoto(it, view.local) },
                currentIndex = index,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PhotoViewerUiState())
    }

    private fun WorkInfo?.toDownloadState(): GalleryDownloadState = when (this?.state) {
        WorkInfo.State.RUNNING -> GalleryDownloadState(
            isDownloading = true,
            downloaded = progress.getInt(GalleryDownloadWorker.KEY_DOWNLOADED, 0),
            total = progress.getInt(GalleryDownloadWorker.KEY_TOTAL, 0),
            isResolved = true,
        )
        WorkInfo.State.ENQUEUED ->
            if (runAttemptCount > 0) GalleryDownloadState(isResuming = true, isResolved = true)
            else GalleryDownloadState(isPending = true, isResolved = true)
        WorkInfo.State.FAILED -> GalleryDownloadState(
            error = outputData.getString(GalleryDownloadWorker.KEY_ERROR) ?: DOWNLOAD_FAILED_MESSAGE,
            errorCode = outputData.getInt(GalleryDownloadWorker.KEY_ERROR_CODE, 0).takeIf { it != 0 },
            isResolved = true,
        )
        else -> GalleryDownloadState(isResolved = true)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val DOWNLOAD_FAILED_MESSAGE = "Falha ao baixar galeria"
    }
}
