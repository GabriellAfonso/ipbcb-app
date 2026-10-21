package com.ipb.castelobranco.features.admin.events.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.events.domain.model.EventChanges
import com.ipb.castelobranco.features.admin.events.domain.model.EventDraft
import com.ipb.castelobranco.features.admin.events.domain.model.EventRecord
import com.ipb.castelobranco.features.admin.events.domain.model.EventStatusFilter
import com.ipb.castelobranco.features.admin.events.domain.repository.EventsAdminRepository
import javax.inject.Inject

class ListEventsUseCase @Inject constructor(
    private val repository: EventsAdminRepository,
) {
    suspend operator fun invoke(status: EventStatusFilter): Result<List<EventRecord>> = repository.list(status)
}

class GetEventRecordUseCase @Inject constructor(
    private val repository: EventsAdminRepository,
) {
    suspend operator fun invoke(id: Int): Result<EventRecord> = repository.get(id)
}

/**
 * Creates when there is no [original], otherwise sends only what changed. An edit with nothing changed
 * and no new image is not a request: it answers the original.
 */
class SaveEventUseCase @Inject constructor(
    private val repository: EventsAdminRepository,
) {
    suspend operator fun invoke(
        draft: EventDraft,
        image: ByteArray?,
        original: EventRecord?,
        originalDraft: EventDraft?,
    ): Result<EventRecord> {
        if (original == null) {
            val bytes = image ?: return Result.failure(AppError.Unknown(message = MISSING_IMAGE))
            return repository.create(EventChanges.forCreate(draft), bytes)
        }
        val changes = EventChanges.between(originalDraft ?: EventDraft(), draft)
        if (changes.isEmpty && image == null) return Result.success(original)
        return repository.update(original.id, changes, image)
    }

    private companion object {
        const val MISSING_IMAGE = "Create without image"
    }
}

class DeleteEventUseCase @Inject constructor(
    private val repository: EventsAdminRepository,
) {
    suspend operator fun invoke(id: Int): Result<Unit> = repository.delete(id)
}
