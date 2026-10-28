package com.ipb.castelobranco.features.events.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.features.events.domain.model.Event
import com.ipb.castelobranco.features.events.domain.repository.EventsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The events still running today, in the server's order. The server already stops sending an event after
 * its last day, but the saved list can be days old: this keeps an expired event off the screen offline.
 * Null while nothing is loaded yet; a failed load reads as no events — the carousel never shows an error page.
 */
class ObserveActiveEventsUseCase @Inject constructor(
    private val repository: EventsRepository,
    private val dateProvider: DateProvider,
) {
    operator fun invoke(): Flow<List<Event>?> = repository.observeEvents().map { state ->
        val events = when (state) {
            is SnapshotState.Loading -> return@map null
            is SnapshotState.Data -> state.value
            is SnapshotState.Error -> emptyList()
        }
        val today = dateProvider.today()
        events.filter { it.isActiveOn(today) }
    }
}
