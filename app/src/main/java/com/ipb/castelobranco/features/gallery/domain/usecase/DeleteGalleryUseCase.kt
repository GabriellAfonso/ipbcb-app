package com.ipb.castelobranco.features.gallery.domain.usecase

import com.ipb.castelobranco.features.gallery.domain.download.GalleryDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.download.GalleryVideoDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryMediaSettings
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryPreviewCache
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import javax.inject.Inject

/**
 * "Apagar galeria", by type. The upload queue is left alone.
 *
 * - Both: pauses every sync, then deletes what the gallery keeps on the device; nothing comes back
 *   until "Baixar galeria". The video switch turns off.
 * - Only videos: the video files go and the switch turns off; photos and the index keep syncing.
 * - Only photos: the originals go and photo downloads pause; the index keeps syncing and the
 *   downloaded videos stay visible.
 */
class DeleteGalleryUseCase @Inject constructor(
    private val repository: GalleryRepository,
    private val downloadScheduler: GalleryDownloadScheduler,
    private val videoScheduler: GalleryVideoDownloadScheduler,
    private val settings: GalleryMediaSettings,
    private val previewCache: GalleryPreviewCache,
) {
    suspend operator fun invoke(photos: Boolean = true, videos: Boolean = true) {
        if (videos) {
            videoScheduler.cancel()
            settings.setAutoDownloadVideos(false)
        }
        if (photos) downloadScheduler.cancel()
        when {
            photos && videos -> {
                // Before clearing: a sync that starts in between must already find the pause.
                repository.setSyncPaused(true)
                repository.clear()
                previewCache.clear()
            }
            videos -> repository.clearVideos()
            photos -> {
                settings.setPhotoDownloadsPaused(true)
                repository.clearPhotos()
                previewCache.clear()
            }
        }
    }
}
