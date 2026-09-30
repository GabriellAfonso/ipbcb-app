package com.ipb.castelobranco.features.gallery.domain.tags

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.features.gallery.domain.manage.GalleryManageRepository
import com.ipb.castelobranco.features.gallery.domain.manage.isForbidden
import com.ipb.castelobranco.features.gallery.domain.model.GalleryMember
import javax.inject.Inject

/** Everyone who can be tagged, read from the server each time the picker opens. */
class LoadTaggableMembersUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
) {
    suspend operator fun invoke(): Result<List<GalleryMember>> = repository.taggableMembers()
}

/** Saves the full set of people of one photo — or nothing, when it did not change. */
class SetPhotoMembersUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
) {
    suspend operator fun invoke(photoId: Long, current: Collection<Long>, next: Set<Long>): TagSaveResult {
        if (current.toSet() == next) return TagSaveResult.Unchanged
        return repository.setPhotoMembers(photoId, next.sorted()).fold(
            onSuccess = { TagSaveResult.Saved },
            onFailure = { TagSaveResult.Failed(it.toAppError()) },
        )
    }
}

/**
 * Adds ([remove] `false`) or removes [memberIds] in every one of [photoIds], in requests of at most
 * [MAX_PHOTOS_PER_REQUEST] photos sent one after another. Every request is attempted — one failing
 * never leaves the rest undone — except after a lost access level, which every following request
 * would get too. One sync at the end.
 */
class ChangePhotoMembersUseCase @Inject constructor(
    private val repository: GalleryManageRepository,
) {
    suspend operator fun invoke(photoIds: List<Long>, memberIds: List<Long>, remove: Boolean): TagBatchResult {
        val ids = photoIds.distinct()
        var updated = 0
        var failure: AppError? = null
        for (chunk in ids.chunked(MAX_PHOTOS_PER_REQUEST)) {
            val result = if (remove) {
                repository.changePhotoMembers(chunk, addMemberIds = emptyList(), removeMemberIds = memberIds)
            } else {
                repository.changePhotoMembers(chunk, addMemberIds = memberIds, removeMemberIds = emptyList())
            }
            val error = result.exceptionOrNull()?.toAppError()
            if (error == null) {
                updated += chunk.size
                continue
            }
            if (failure == null) failure = error
            if (error.isForbidden()) break
        }
        if (updated > 0) repository.syncAfterWrite()
        return TagBatchResult(updated = updated, total = ids.size, failure = failure)
    }

    companion object {
        /** The server's limit of distinct photos per request. */
        const val MAX_PHOTOS_PER_REQUEST = 200
    }
}
