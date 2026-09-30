package com.ipb.castelobranco.features.gallery.domain.usecase

import com.ipb.castelobranco.features.gallery.domain.download.GalleryDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import javax.inject.Inject

/**
 * Brings the gallery in step with the server, then queues the originals still missing from disk —
 * on WiFi, keeping a download that is already queued or running.
 */
class SyncGalleryUseCase @Inject constructor(
    private val repository: GalleryRepository,
    private val downloadScheduler: GalleryDownloadScheduler,
) {
    suspend operator fun invoke(): GallerySyncResult = queueMissing(repository.sync())

    /** After a write: never skipped, so derived changes (covers, album names, positions) arrive now. */
    suspend fun afterWrite(): GallerySyncResult = queueMissing(repository.syncAfterWrite())

    private fun queueMissing(result: GallerySyncResult): GallerySyncResult {
        if (result is GallerySyncResult.Synced && result.missingOriginals > 0) {
            downloadScheduler.enqueueWifiOnly(replaceExisting = false)
        }
        return result
    }
}
