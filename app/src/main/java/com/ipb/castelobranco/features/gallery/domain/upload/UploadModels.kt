package com.ipb.castelobranco.features.gallery.domain.upload

import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto

/**
 * One picked image waiting to be sent. [uploadId] is generated once, at enqueue, and sent as the
 * `client_upload_id` of every attempt, so a retry after a lost answer never creates a second photo.
 */
data class UploadItem(
    val uploadId: String,
    val albumId: Long,
    /** The name the photo gets on the server, extension matching what is sent. */
    val displayName: String,
    /** File under `gallery/uploads/`. */
    val fileName: String,
    val state: UploadState,
    /** Why it failed, for the user; set only in [UploadState.Failed]. */
    val failure: String? = null,
    val enqueuedAt: Long,
)

enum class UploadState {
    /** Raw copy of the picked image; not prepared yet. */
    Waiting,

    /** Prepared (downscaled, upright JPEG — or a GIF within limits); ready to send. */
    Prepared,

    /** Will not be retried; stays listed until dismissed. */
    Failed,
}

/** How the server answered one upload. */
sealed interface UploadOutcome {
    /** Accepted — or recognized as a repeat of a live photo, possibly in another album now. */
    data class Sent(val photo: GalleryPhoto) : UploadOutcome
    data class Failed(val reason: String) : UploadOutcome

    /** The album no longer exists: every queued photo of it fails. */
    data object AlbumGone : UploadOutcome

    /** The level on `gallery` is gone: every remaining photo fails. */
    data class AccessLost(val reason: String) : UploadOutcome

    /** Session problem: stop without failing anything; the session path decides. */
    data object Stop : UploadOutcome

    /** Network failure or server error: try again later with the same id. */
    data object Retry : UploadOutcome
}
