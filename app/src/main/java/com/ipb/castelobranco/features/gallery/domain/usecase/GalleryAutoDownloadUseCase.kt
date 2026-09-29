package com.ipb.castelobranco.features.gallery.domain.usecase

import com.ipb.castelobranco.features.gallery.domain.download.GalleryDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryPreviewCache
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.sync.GallerySyncScheduler
import javax.inject.Inject

/** Session-level gallery triggers: app start and foreground, login, download buttons and logout. */
class GalleryAutoDownloadUseCase @Inject constructor(
    private val downloadScheduler: GalleryDownloadScheduler,
    private val syncScheduler: GallerySyncScheduler,
    private val syncGallery: SyncGalleryUseCase,
    private val repository: GalleryRepository,
    private val previewCache: GalleryPreviewCache,
) {
    /** App start and every return to the foreground — call only with an active session. */
    suspend fun onAppForeground() {
        syncScheduler.schedulePeriodic()
        syncGallery()
    }

    /**
     * Replaces the download work on purpose: that discards a 401 recorded while the user was still
     * signed out, which the screen would otherwise keep showing.
     */
    suspend fun onLoginSuccess() {
        downloadScheduler.enqueueWifiOnly(replaceExisting = true)
        syncScheduler.schedulePeriodic()
        syncGallery()
    }

    /** Download manual via botão — WiFi only, mantém se já estiver rodando. */
    fun enqueueWifiOnly(replaceExisting: Boolean = false) = downloadScheduler.enqueueWifiOnly(replaceExisting)

    /** Download forçado com dados móveis — substitui qualquer trabalho pendente. */
    fun enqueueAnyNetwork() = downloadScheduler.enqueueAnyNetwork()

    /** Cancela todo o trabalho e apaga o acervo — conteúdo de membro não sobrevive à sessão. */
    suspend fun clearOnLogout() {
        downloadScheduler.cancel()
        syncScheduler.cancel()
        repository.clear()
        previewCache.clear()
    }
}
