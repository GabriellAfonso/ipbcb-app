package com.ipb.castelobranco.core.domain.gallery

import kotlinx.coroutines.flow.Flow

/**
 * What other features (settings) may do with the gallery's storage, without importing it. Implemented
 * by the gallery feature.
 */
interface GalleryStoragePort {
    /** The videos of the gallery and how many are on the device; follows the local copy. */
    val videos: Flow<GalleryVideoSummary>

    /** "Baixar vídeos automaticamente". */
    val autoDownloadVideos: Flow<Boolean>

    suspend fun setAutoDownloadVideos(enabled: Boolean)

    /** Bytes on the device, per type. */
    suspend fun usage(): GalleryUsage

    /** What "Baixar galeria" could still bring. */
    suspend fun missing(): GalleryMissing

    /** "Apagar galeria" with the checked types. */
    suspend fun delete(photos: Boolean, videos: Boolean)

    /** "Baixar galeria" with the checked types; [useMobileData] applies to photos only. */
    suspend fun download(photos: Boolean, videos: Boolean, useMobileData: Boolean)
}

data class GalleryUsage(val photoBytes: Long, val videoBytes: Long)

/** [totalBytes] adds the known sizes of every video; [isDownloading] = the video work is running. */
data class GalleryVideoSummary(
    val total: Int = 0,
    val downloaded: Int = 0,
    val totalBytes: Long = 0,
    val isDownloading: Boolean = false,
)

/**
 * [photos] photos without their original; [videos] videos not on the device. [isPaused] = the whole
 * gallery was deleted and nothing is known until it syncs again.
 */
data class GalleryMissing(val photos: Int, val videos: Int, val isPaused: Boolean)
