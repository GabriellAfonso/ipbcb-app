package com.ipb.castelobranco.features.events.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.features.events.domain.model.Event
import com.ipb.castelobranco.features.events.domain.repository.EventsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * One active event, read from the saved list so the detail opens offline; null once it is gone. Waits
 * for the list to load instead of reporting the event gone while the disk is still being read.
 */
class GetEventUseCase @Inject constructor(
    private val repository: EventsRepository,
    private val dateProvider: DateProvider,
) {
    operator fun invoke(id: Int): Flow<Event?> = repository.observeEvents()
        .filterNot { it is SnapshotState.Loading }
        .map { state ->
            (state as? SnapshotState.Data)?.value
                ?.firstOrNull { it.id == id }
                ?.takeIf { it.isActiveOn(dateProvider.today()) }
        }
}
