package com.ipb.castelobranco.features.gallery.domain.model

import com.ipb.castelobranco.core.domain.error.AppError

sealed interface GallerySyncResult {
    /**
     * The index follows the server; [missingOriginals] photos have no original on disk yet, and
     * [missingVideos] videos have no complete file.
     */
    data class Synced(val missingOriginals: Int, val missingVideos: Int = 0) : GallerySyncResult

    /** Another sync was running, there is no session, or the sync was cancelled by logout. */
    data object Skipped : GallerySyncResult

    /** The user deleted the gallery: nothing syncs until they ask for it again. */
    data object Paused : GallerySyncResult

    /** Nothing changed on the device. */
    data class Failed(val error: AppError) : GallerySyncResult
}

data class GallerySyncStatus(
    val isRunning: Boolean = false,
    /** Error of the last finished sync; cleared by the next successful one. */
    val lastError: AppError? = null,
    /** At least one sync finished (successfully or not) in this process. */
    val hasAnswered: Boolean = false,
    /** The user deleted the gallery from settings: every sync answers [GallerySyncResult.Paused]. */
    val isPaused: Boolean = false,
)
