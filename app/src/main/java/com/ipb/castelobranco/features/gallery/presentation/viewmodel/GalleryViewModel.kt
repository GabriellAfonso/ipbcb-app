package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import timber.log.Timber
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ipb.castelobranco.core.data.NetworkConnectivityObserver
import com.ipb.castelobranco.features.gallery.data.work.GalleryDownloadWorker
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.usecase.GalleryAutoDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class GalleryDownloadState(
    val isDownloading: Boolean = false,
    val isPending: Boolean = false,
    /** Waiting out a backoff after a rate limit or a network failure — not an error. */
    val isResuming: Boolean = false,
    val downloaded: Int = 0,
    val total: Int = 0,
    val error: String? = null,
    val errorCode: Int? = null,
    val isResolved: Boolean = false,
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: GalleryRepository,
    private val autoDownload: GalleryAutoDownloadUseCase,
    connectivityObserver: NetworkConnectivityObserver,
    workManager: WorkManager,
) : ViewModel() {

    val albums = repository.albumsFlow
    val thumbnails = repository.thumbnailsFlow

    val isOnWifi: StateFlow<Boolean> = connectivityObserver.isOnWifi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val downloadState: StateFlow<GalleryDownloadState> = workManager
        .getWorkInfosForUniqueWorkFlow(GalleryDownloadWorker.WORK_NAME)
        .map { infos ->
            val info = infos.firstOrNull()
            when (info?.state) {
                WorkInfo.State.RUNNING -> {
                    val done = info.progress.getInt(GalleryDownloadWorker.KEY_DOWNLOADED, 0)
                    val total = info.progress.getInt(GalleryDownloadWorker.KEY_TOTAL, 0)
                    GalleryDownloadState(isDownloading = true, downloaded = done, total = total, isResolved = true)
                }
                WorkInfo.State.ENQUEUED ->
                    if (info.runAttemptCount > 0) GalleryDownloadState(isResuming = true, isResolved = true)
                    else GalleryDownloadState(isPending = true, isResolved = true)
                WorkInfo.State.FAILED -> {
                    val errorMsg = info.outputData.getString(GalleryDownloadWorker.KEY_ERROR)
                        ?: "Falha ao baixar galeria"
                    val code = info.outputData.getInt(GalleryDownloadWorker.KEY_ERROR_CODE, 0)
                    GalleryDownloadState(error = errorMsg, errorCode = code.takeIf { it != 0 }, isResolved = true)
                }
                else -> GalleryDownloadState(isResolved = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GalleryDownloadState())

    fun downloadAllPhotos() {
        autoDownload.enqueueWifiOnly()
    }

    fun downloadWithMobileData() {
        autoDownload.enqueueAnyNetwork()
    }

    /** Nova tentativa após falha — REPLACE para descartar o WorkInfo com erro. */
    fun retryDownload() {
        autoDownload.enqueueWifiOnly(replaceExisting = true)
    }

    fun clearGallery() {
        viewModelScope.launch {
            repository.clearAllPhotos()
        }
    }

    suspend fun getLocalPhotos(albumId: Long): List<File> =
        repository.getLocalPhotos(albumId)

    suspend fun getPhotoName(albumId: Long, photoId: Long): String {
        Timber.d("getPhotoName: %d - %d", albumId, photoId)
        return repository.getPhotoName(albumId, photoId) ?: "Foto"
    }
}
