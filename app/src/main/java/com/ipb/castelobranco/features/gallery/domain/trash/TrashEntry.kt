package com.ipb.castelobranco.features.gallery.domain.trash

import com.ipb.castelobranco.core.domain.error.AppError

enum class TrashKind { ALBUM, PHOTO }

/** A row of the trash and a restore target: an album and a photo may share an id. */
data class TrashKey(val kind: TrashKind, val id: Long)

/**
 * One delete action waiting in the trash. Dates are kept as the server sent them ([deletedAt] an ISO
 * instant, [purgeOn] `yyyy-MM-dd`); the screen formats them. [subAlbumCount] and [photoCount] are what
 * went with an album (0 for a photo). [deletedBy] and [uploadedBy] are `null` when unknown.
 */
data class TrashEntry(
    val kind: TrashKind,
    val id: Long,
    val name: String,
    val deletedAt: String,
    val deletedBy: String?,
    val uploadedBy: String?,
    val purgeOn: String,
    val subAlbumCount: Int,
    val photoCount: Int,
    val thumbnailUrl: String?,
) {
    val key: TrashKey get() = TrashKey(kind, id)
}

/** How a restore ended. A success is already applied to the local copy and a sync has run. */
sealed interface RestoreResult {
    data object Restored : RestoreResult

    /** Restored by someone else, purged, or not the root of its batch. */
    data object NotInTrash : RestoreResult

    /** The item's parent album (a photo's album) is itself in the trash: restore that one first. */
    data class TrashedParent(val parentAlbumId: Long, val error: AppError) : RestoreResult

    /**
     * A live sibling album holds the name. [isOnDevice] = the conflicting album is in the local copy
     * (after a sync, when it was missing), so it can be opened.
     */
    data class NameConflict(
        val conflictingAlbumId: Long,
        val isOnDevice: Boolean,
        val error: AppError,
    ) : RestoreResult

    /** Offline, forbidden or anything else; the list is unchanged. */
    data class Failed(val error: AppError) : RestoreResult
}
