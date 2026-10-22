package com.ipb.castelobranco.features.gallery.domain.usecase

import com.ipb.castelobranco.features.gallery.domain.download.GalleryDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryPreviewCache
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import javax.inject.Inject

/**
 * "Apagar galeria": pauses every sync, then deletes what the gallery keeps on the device. Nothing comes
 * back until the user taps "Baixar galeria". The upload queue is left alone.
 */
class DeleteGalleryUseCase @Inject constructor(
    private val repository: GalleryRepository,
    private val downloadScheduler: GalleryDownloadScheduler,
    private val previewCache: GalleryPreviewCache,
) {
    suspend operator fun invoke() {
        downloadScheduler.cancel()
        // Before clearing: a sync that starts in between must already find the pause.
        repository.setSyncPaused(true)
        repository.clear()
        previewCache.clear()
    }
}
