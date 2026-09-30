package com.ipb.castelobranco.features.gallery.domain.trash

import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.features.gallery.domain.manage.GalleryManageRepository
import com.ipb.castelobranco.features.gallery.domain.manage.conflictingAlbumId
import com.ipb.castelobranco.features.gallery.domain.manage.isNotFound
import com.ipb.castelobranco.features.gallery.domain.manage.trashedParentId
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import javax.inject.Inject

/** The server's trash, read on demand (online only). */
class LoadTrashUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
) {
    suspend operator fun invoke(): Result<List<TrashEntry>> = repository.trash()
}

/**
 * Restores one trash entry and says how it ended. On a name conflict, makes sure the conflicting
 * album is in the local copy (syncing when it is missing) so the screen can open it.
 */
class RestoreTrashItemUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
    private val gallery: GalleryRepository,
) {
    suspend operator fun invoke(key: TrashKey): RestoreResult {
        val error = repository.restore(key).exceptionOrNull()?.toAppError() ?: return RestoreResult.Restored
        error.trashedParentId()?.let { return RestoreResult.TrashedParent(it, error) }
        error.conflictingAlbumId()?.let { albumId ->
            if (!isOnDevice(albumId)) repository.syncAfterWrite()
            return RestoreResult.NameConflict(albumId, isOnDevice(albumId), error)
        }
        return if (error.isNotFound()) RestoreResult.NotInTrash else RestoreResult.Failed(error)
    }

    private fun isOnDevice(albumId: Long): Boolean =
        gallery.localState.value.index?.albums?.containsKey(albumId) == true
}
