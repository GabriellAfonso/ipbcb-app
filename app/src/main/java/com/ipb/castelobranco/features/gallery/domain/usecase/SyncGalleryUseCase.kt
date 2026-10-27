package com.ipb.castelobranco.features.gallery.domain.usecase

import com.ipb.castelobranco.features.gallery.domain.download.GalleryDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.download.GalleryVideoDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryMediaSettings
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import javax.inject.Inject

/**
 * Brings the gallery in step with the server, then queues what is still missing from disk, keeping a
 * download that is already queued or running: photos on WiFi (unless only the photos were deleted),
 * videos on WiFi while "Baixar vídeos automaticamente" is on.
 */
class SyncGalleryUseCase @Inject constructor(
    private val repository: GalleryRepository,
    private val downloadScheduler: GalleryDownloadScheduler,
    private val videoScheduler: GalleryVideoDownloadScheduler,
    private val settings: GalleryMediaSettings,
) {
    suspend operator fun invoke(): GallerySyncResult = queueMissing(repository.sync())

    /** After a write: never skipped, so derived changes (covers, album names, positions) arrive now. */
    suspend fun afterWrite(): GallerySyncResult = queueMissing(repository.syncAfterWrite())

    private suspend fun queueMissing(result: GallerySyncResult): GallerySyncResult {
        if (result !is GallerySyncResult.Synced) return result
        if (result.missingOriginals > 0 && !settings.isPhotoDownloadsPaused()) {
            downloadScheduler.enqueueWifiOnly(replaceExisting = false)
        }
        if (result.missingVideos > 0 && settings.isAutoDownloadVideos()) {
            videoScheduler.enqueue(replaceExisting = false)
        }
        return result
    }
}
