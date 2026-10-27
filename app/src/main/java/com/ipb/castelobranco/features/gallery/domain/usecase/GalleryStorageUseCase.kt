package com.ipb.castelobranco.features.gallery.domain.usecase

import com.ipb.castelobranco.core.domain.gallery.GalleryMissing
import com.ipb.castelobranco.core.domain.gallery.GalleryUsage
import com.ipb.castelobranco.core.domain.gallery.GalleryVideoSummary
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Sizes, counts and the "Baixar / Apagar galeria" actions by type, for settings and the gallery. */
class GalleryStorageUseCase @Inject constructor(
    private val repository: GalleryRepository,
    private val deleteGallery: DeleteGalleryUseCase,
    private val autoDownload: GalleryAutoDownloadUseCase,
) {
    /** Every video of the index, those on the device, and the known size of all of them. */
    val videos: Flow<GalleryVideoSummary> = repository.localState
        .map(::summary)
        .distinctUntilChanged()

    suspend fun usage(): GalleryUsage = repository.usage().let { GalleryUsage(it.photoBytes, it.videoBytes) }

    suspend fun missing(): GalleryMissing {
        repository.preload()
        val local = repository.localState.value
        val items = local.index?.photos?.values.orEmpty()
        return GalleryMissing(
            photos = items.count { !it.isVideo && it.id !in local.originals },
            videos = items.count { it.isVideo && it.id !in local.videos },
            isPaused = repository.syncStatus.value.isPaused,
        )
    }

    suspend fun delete(photos: Boolean, videos: Boolean) {
        if (photos || videos) deleteGallery(photos = photos, videos = videos)
    }

    suspend fun download(photos: Boolean, videos: Boolean, useMobileData: Boolean) {
        if (photos || videos) autoDownload.downloadAfterDelete(photos, videos, useMobileData)
    }

    companion object {
        fun summary(local: GalleryLocalState): GalleryVideoSummary {
            val videos = local.index?.photos?.values.orEmpty().filter { it.isVideo }
            return GalleryVideoSummary(
                total = videos.size,
                downloaded = videos.count { it.id in local.videos },
                totalBytes = videos.sumOf { it.fileSize ?: 0L },
            )
        }
    }
}
