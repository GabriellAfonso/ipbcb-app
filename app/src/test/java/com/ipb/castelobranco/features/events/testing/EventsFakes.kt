package com.ipb.castelobranco.features.events.testing

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.events.domain.model.Event
import com.ipb.castelobranco.features.events.domain.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import java.time.LocalTime

class FakeEventsRepository(
    initial: SnapshotState<List<Event>> = SnapshotState.Loading,
) : EventsRepository {
    val state = MutableStateFlow(initial)
    var refreshCalls = 0
        private set
    var clearCalls = 0
        private set

    override fun observeEvents() = state

    override suspend fun preload() = Unit

    override suspend fun refreshEvents(): RefreshResult {
        refreshCalls++
        return RefreshResult.NotModified
    }

    override suspend fun clear() {
        clearCalls++
        state.value = SnapshotState.Loading
    }
}

fun event(
    id: Int,
    start: LocalDate,
    end: LocalDate? = null,
    time: LocalTime? = null,
    title: String = "Evento $id",
    location: String? = null,
    description: String? = null,
    membersOnly: Boolean = false,
) = Event(
    id = id,
    title = title,
    startDate = start,
    endDate = end,
    startTime = time,
    location = location,
    description = description,
    imageUrl = "https://host/ipbcb/media/events/$id.jpg",
    isMembersOnly = membersOnly,
)
