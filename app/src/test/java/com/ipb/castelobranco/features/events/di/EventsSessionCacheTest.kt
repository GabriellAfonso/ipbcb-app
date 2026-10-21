package com.ipb.castelobranco.features.events.di

import com.ipb.castelobranco.core.domain.events.EventImageCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.events.testing.FakeEventsRepository
import com.ipb.castelobranco.features.events.testing.event
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EventsSessionCacheTest {

    @Test
    fun `ending the session forgets the list and the flyers, without fetching`() = runTest {
        val repository = FakeEventsRepository(
            SnapshotState.Data(listOf(event(1, start = LocalDate.of(2026, 11, 14), membersOnly = true)))
        )
        var imageClears = 0

        eventsSessionCache(repository, EventImageCache { imageClears++ }).clear()

        assertEquals(1, repository.clearCalls)
        assertTrue(repository.state.value is SnapshotState.Loading)
        assertEquals(1, imageClears)
        assertEquals(0, repository.refreshCalls)
    }
}
