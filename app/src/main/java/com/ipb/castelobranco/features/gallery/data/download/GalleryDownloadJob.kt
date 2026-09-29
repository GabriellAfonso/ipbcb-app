package com.ipb.castelobranco.features.gallery.data.download

import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.model.GalleryTree
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import javax.inject.Inject

/**
 * One run of the original-download work, kept free of WorkManager types so it is unit-testable.
 *
 * The photo list is the index, in tree order. With no index yet (login on WiFi before any sync
 * answered) it syncs first, and a failed sync is treated like the old failed photo list.
 */
class GalleryDownloadJob @Inject constructor(
    private val repository: GalleryRepository,
    private val downloader: GalleryPhotoDownloader,
) {

    suspend fun run(
        runAttemptCount: Int,
        maxRetries: Int,
        onProgress: suspend (downloaded: Int, total: Int) -> Unit,
    ): WorkDecision {
        repository.preload()
        if (repository.localState.value.index == null) {
            when (val result = repository.sync()) {
                is GallerySyncResult.Failed ->
                    return result.error.toListFailureDecision(runAttemptCount, maxRetries)
                GallerySyncResult.Skipped ->
                    return if (runAttemptCount < maxRetries) WorkDecision.Retry else WorkDecision.Success
                is GallerySyncResult.Synced -> Unit
            }
        }
        val index = repository.localState.value.index ?: return WorkDecision.Success

        val run = downloader.download(
            photos = GalleryTree(index).allPhotosInTreeOrder(),
            onProgress = onProgress,
            // A grid ganha as fotos a cada álbum concluído.
            onAlbumDone = { repository.refreshLocalFiles() },
        )
        // Em qualquer desfecho, as fotos salvas antes de uma parada aparecem na grid.
        repository.refreshLocalFiles()
        return run.toWorkDecision(runAttemptCount, maxRetries)
    }
}
