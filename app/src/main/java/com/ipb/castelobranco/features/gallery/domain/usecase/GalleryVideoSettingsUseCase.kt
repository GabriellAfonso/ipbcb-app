package com.ipb.castelobranco.features.gallery.domain.usecase

import com.ipb.castelobranco.features.gallery.domain.download.GalleryVideoDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryMediaSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * "Baixar vídeos automaticamente" and the discovery card. Turning the switch on queues every missing
 * video (WiFi only); turning it off stops downloading and keeps what is already on the device.
 */
class GalleryVideoSettingsUseCase @Inject constructor(
    private val settings: GalleryMediaSettings,
    private val videoScheduler: GalleryVideoDownloadScheduler,
) {
    val autoDownload: Flow<Boolean> = settings.autoDownloadVideos
    val cardDismissed: Flow<Boolean> = settings.videoCardDismissed

    suspend fun setAutoDownload(enabled: Boolean) {
        settings.setAutoDownloadVideos(enabled)
        // Replace: a run that stopped for low space or failed is tried again now.
        if (enabled) videoScheduler.enqueue(replaceExisting = true) else videoScheduler.cancel()
    }

    /** "Não, obrigado" on the card: hidden forever. */
    suspend fun dismissCard() = settings.dismissVideoCard()

    /** "Baixar" on the card: hidden forever, and the switch turns on. */
    suspend fun acceptCard() {
        settings.dismissVideoCard()
        setAutoDownload(true)
    }
}
