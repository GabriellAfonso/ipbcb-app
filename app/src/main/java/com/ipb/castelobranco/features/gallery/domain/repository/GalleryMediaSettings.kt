package com.ipb.castelobranco.features.gallery.domain.repository

import kotlinx.coroutines.flow.Flow

/** The gallery's device settings about media. All of them survive sign-out. */
interface GalleryMediaSettings {
    /** "Baixar vídeos automaticamente"; off by default. */
    val autoDownloadVideos: Flow<Boolean>
    suspend fun isAutoDownloadVideos(): Boolean
    suspend fun setAutoDownloadVideos(enabled: Boolean)

    /** The "Há vídeos na galeria" card was answered; never cleared. */
    val videoCardDismissed: Flow<Boolean>
    suspend fun dismissVideoCard()

    /** Only the photos were deleted: photo downloads wait for "Baixar galeria" with Fotos. */
    val photoDownloadsPaused: Flow<Boolean>
    suspend fun isPhotoDownloadsPaused(): Boolean
    suspend fun setPhotoDownloadsPaused(paused: Boolean)
}
