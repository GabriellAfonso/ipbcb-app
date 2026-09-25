package com.ipb.castelobranco.features.gallery.data.download

import com.ipb.castelobranco.core.domain.error.AppError

/** Why a gallery run ended before trying every photo. Each reason describes the requester, not a photo. */
enum class StopReason { UNAUTHENTICATED, FORBIDDEN, RATE_LIMITED }

/**
 * Outcome of one pass over the gallery photo list. `downloaded` always equals the photos of the list
 * that are on disk when the run ends — a photo that failed is never counted.
 */
sealed interface GalleryDownloadRun {
    val downloaded: Int
    val total: Int

    /** Every photo was tried. [failed] photos are retried on the next run. */
    data class Completed(
        override val downloaded: Int,
        override val total: Int,
        val failed: Int,
        val networkFailures: Int,
    ) : GalleryDownloadRun

    /** A requester-level refusal ended the run; no photo after the refused one was requested. */
    data class Stopped(
        val reason: StopReason,
        val error: AppError,
        override val downloaded: Int,
        override val total: Int,
    ) : GalleryDownloadRun
}
