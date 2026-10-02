package com.ipb.castelobranco.features.worshiphub.tables.domain.model

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.setlist.SundaySetlist

sealed interface SaveSetlistResult {
    data class Saved(val setlist: SundaySetlist) : SaveSetlistResult
    data class Failed(val failure: SaveSetlistFailure) : SaveSetlistResult
}

/** Why a save was refused (`backend/specs/017-sunday-setlist-push/contracts/setlist-api.md`). */
sealed interface SaveSetlistFailure {
    /** `403`: no `manage` on `songs`, or not a worship member any more. */
    data object NoPermission : SaveSetlistFailure

    /** `400`: the server explains in its `detail`. */
    data class Invalid(val error: AppError) : SaveSetlistFailure

    /** `404`: [count] songs of the setlist no longer exist. */
    data class MissingSongs(val count: Int) : SaveSetlistFailure

    data object NoConnection : SaveSetlistFailure

    data class Other(val error: AppError) : SaveSetlistFailure
}

/** One filled row of the repertoire, as sent. */
data class SetlistEntry(
    val position: Int,
    val songId: Int,
    val tone: String,
)
