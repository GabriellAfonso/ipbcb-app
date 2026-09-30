package com.ipb.castelobranco.features.gallery.domain.tags

import com.ipb.castelobranco.core.domain.error.AppError

/** How saving the people of one photo ended. */
sealed interface TagSaveResult {
    /** The set did not change: nothing was sent. */
    data object Unchanged : TagSaveResult

    /** Saved, applied to the local copy, and a sync has run. */
    data object Saved : TagSaveResult

    data class Failed(val error: AppError) : TagSaveResult
}

/**
 * Outcome of adding or removing people in many photos: [updated] of [total] photos went through
 * (whole requests of up to 200); [failure] is the first request's error, if any.
 */
data class TagBatchResult(val updated: Int, val total: Int, val failure: AppError?)
