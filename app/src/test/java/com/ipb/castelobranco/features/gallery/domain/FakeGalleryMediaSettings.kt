package com.ipb.castelobranco.features.gallery.domain

import com.ipb.castelobranco.features.gallery.domain.repository.GalleryMediaSettings
import kotlinx.coroutines.flow.MutableStateFlow

/** The gallery's media settings in memory. */
class FakeGalleryMediaSettings(
    autoDownloadVideos: Boolean = false,
    cardDismissed: Boolean = false,
    photoDownloadsPaused: Boolean = false,
) : GalleryMediaSettings {
    override val autoDownloadVideos = MutableStateFlow(autoDownloadVideos)
    override val videoCardDismissed = MutableStateFlow(cardDismissed)
    override val photoDownloadsPaused = MutableStateFlow(photoDownloadsPaused)

    override suspend fun isAutoDownloadVideos(): Boolean = autoDownloadVideos.value

    override suspend fun setAutoDownloadVideos(enabled: Boolean) {
        autoDownloadVideos.value = enabled
    }

    override suspend fun dismissVideoCard() {
        videoCardDismissed.value = true
    }

    override suspend fun isPhotoDownloadsPaused(): Boolean = photoDownloadsPaused.value

    override suspend fun setPhotoDownloadsPaused(paused: Boolean) {
        photoDownloadsPaused.value = paused
    }
}
