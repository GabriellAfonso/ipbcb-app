package com.ipb.castelobranco.features.gallery.domain.manage

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import javax.inject.Inject

/**
 * Saves the "Organizar" draft: one request per group whose order changed, sub-albums first. A
 * refusal as mismatched (someone changed the siblings meanwhile) has already synced in the
 * repository; the caller shows the refreshed list.
 */
class ReorderUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
) {
    suspend operator fun invoke(draft: OrderDraft): ReorderResult {
        val albumsChanged = draft.albumIds != draft.originalAlbumIds
        val photosChanged = draft.photoIds != draft.originalPhotoIds
        if (!albumsChanged && !photosChanged) return ReorderResult.Unchanged

        if (albumsChanged) {
            repository.reorderAlbums(draft.albumId, draft.albumIds).onFailure { return failure(it) }
        }
        if (photosChanged && draft.albumId != null) {
            repository.reorderPhotos(draft.albumId, draft.photoIds).onFailure { return failure(it) }
        }
        return ReorderResult.Saved
    }

    private fun failure(error: Throwable): ReorderResult {
        val appError = error.toAppError()
        return if (appError.isOrderMismatch()) ReorderResult.OrderChanged else ReorderResult.Failed(appError)
    }
}

/**
 * The order being edited in one screen. [albumId] `null` = the root (albums only). `original*` are
 * the orders when the mode opened.
 */
data class OrderDraft(
    val albumId: Long?,
    val albumIds: List<Long>,
    val photoIds: List<Long>,
    val originalAlbumIds: List<Long>,
    val originalPhotoIds: List<Long>,
)

sealed interface ReorderResult {
    data object Saved : ReorderResult
    data object Unchanged : ReorderResult
    data object OrderChanged : ReorderResult
    data class Failed(val error: AppError) : ReorderResult
}
