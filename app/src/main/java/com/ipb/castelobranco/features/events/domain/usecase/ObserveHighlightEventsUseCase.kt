package com.ipb.castelobranco.features.events.domain.usecase

import com.ipb.castelobranco.features.events.domain.model.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** The carousel's event pages: the first [HIGHLIGHT_LIMIT] active events. */
class ObserveHighlightEventsUseCase @Inject constructor(
    private val observeActiveEvents: ObserveActiveEventsUseCase,
) {
    operator fun invoke(): Flow<List<Event>> = observeActiveEvents().map { it.take(HIGHLIGHT_LIMIT) }

    companion object {
        const val HIGHLIGHT_LIMIT = 3
    }
}
