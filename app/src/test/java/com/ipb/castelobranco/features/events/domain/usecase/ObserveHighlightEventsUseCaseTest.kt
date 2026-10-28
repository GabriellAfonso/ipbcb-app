package com.ipb.castelobranco.features.events.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.features.events.testing.FakeEventsRepository
import com.ipb.castelobranco.features.events.testing.event
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ObserveHighlightEventsUseCaseTest {

    private val today = LocalDate.of(2026, 11, 15)
    private val repository = FakeEventsRepository()
    private val useCase = ObserveHighlightEventsUseCase(ObserveActiveEventsUseCase(repository, DateProvider { today }))

    @Test
    fun `an event whose last day passed is dropped`() = runTest {
        repository.state.value = SnapshotState.Data(
            listOf(event(1, start = today.minusDays(1)), event(2, start = today))
        )

        assertEquals(listOf(2), useCase().first()!!.map { it.id })
    }

    @Test
    fun `a multi-day event still running is kept`() = runTest {
        repository.state.value = SnapshotState.Data(
            listOf(event(1, start = today.minusDays(2), end = today))
        )

        assertEquals(listOf(1), useCase().first()!!.map { it.id })
    }

    @Test
    fun `only the first three are kept, in the server order`() = runTest {
        repository.state.value = SnapshotState.Data((1..5).map { event(it, start = today.plusDays(it.toLong())) })

        assertEquals(listOf(1, 2, 3), useCase().first()!!.map { it.id })
    }

    @Test
    fun `nothing loaded yet is null`() = runTest {
        assertNull(useCase().first())
    }

    @Test
    fun `an error reads as no events`() = runTest {
        repository.state.value = SnapshotState.Error(AppError.Network())

        assertTrue(useCase().first()!!.isEmpty())
    }
}
