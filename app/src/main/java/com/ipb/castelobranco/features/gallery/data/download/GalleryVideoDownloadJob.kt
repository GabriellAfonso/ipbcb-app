package com.ipb.castelobranco.features.gallery.data.download

import com.ipb.castelobranco.features.gallery.domain.repository.GalleryMediaSettings
import com.ipb.castelobranco.features.gallery.domain.model.GalleryTree
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import javax.inject.Inject

/** How a video run ends, free of WorkManager types. */
sealed interface VideoWorkDecision {
    data object Success : VideoWorkDecision
    data object Retry : VideoWorkDecision
    data object LowSpace : VideoWorkDecision
    data class Fail(val message: String, val code: Int) : VideoWorkDecision
}

/**
 * One run of the video download work: only while "Baixar vídeos automaticamente" is on, over every
 * video of the index in tree order — the undownloaded ones are exactly what the gallery hides. Each
 * finished video is published at once, so it appears without waiting for the rest.
 */
class GalleryVideoDownloadJob @Inject constructor(
    private val repository: GalleryRepository,
    private val preferences: GalleryMediaSettings,
    private val downloader: GalleryVideoDownloader,
) {

    suspend fun run(runAttemptCount: Int, maxRetries: Int): VideoWorkDecision {
        if (!preferences.isAutoDownloadVideos()) return VideoWorkDecision.Success
        repository.preload()
        val index = repository.localState.value.index ?: return VideoWorkDecision.Success
        val videos = GalleryTree(index).allPhotosInTreeOrder().filter { it.isVideo }
        if (videos.isEmpty()) return VideoWorkDecision.Success

        val run = downloader.download(videos, onVideoDone = { repository.refreshLocalFiles() })
        repository.refreshLocalFiles()
        return when (run) {
            is VideoDownloadRun.Completed ->
                if (run.networkFailures > 0 && runAttemptCount < maxRetries) VideoWorkDecision.Retry
                else VideoWorkDecision.Success
            is VideoDownloadRun.LowSpace -> VideoWorkDecision.LowSpace
            is VideoDownloadRun.Stopped -> when (run.reason) {
                StopReason.RATE_LIMITED -> VideoWorkDecision.Retry
                StopReason.FORBIDDEN -> VideoWorkDecision.Fail(run.error.userMessage ?: NO_ACCESS, HTTP_FORBIDDEN)
                StopReason.UNAUTHENTICATED ->
                    VideoWorkDecision.Fail(run.error.userMessage ?: SESSION_EXPIRED, HTTP_UNAUTHORIZED)
            }
        }
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val NO_ACCESS = "Disponível apenas para membros."
        const val SESSION_EXPIRED = "Sua sessão expirou. Entre novamente para acessar a galeria."
    }
}
