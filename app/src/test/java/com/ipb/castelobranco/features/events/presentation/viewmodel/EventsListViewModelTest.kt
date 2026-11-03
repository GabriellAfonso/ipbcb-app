package com.ipb.castelobranco.features.events.presentation.viewmodel

import app.cash.turbine.test
import coil.ImageLoader
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.features.events.domain.usecase.ObserveActiveEventsUseCase
import com.ipb.castelobranco.features.events.domain.usecase.RefreshEventsUseCase
import com.ipb.castelobranco.features.events.presentation.state.EventsListUiState
import com.ipb.castelobranco.features.events.testing.FakeEventsRepository
import com.ipb.castelobranco.features.events.testing.event
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class EventsListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 11, 10)
    private val repository = FakeEventsRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = EventsListViewModel(
        observeActiveEvents = ObserveActiveEventsUseCase(repository, DateProvider { today }),
        refreshEvents = RefreshEventsUseCase(repository),
        imageLoader = mockk<ImageLoader>(),
    )

    /** The state after every pending update, read while subscribed: the flow only runs with a collector. */
    private suspend fun settledState(vm: EventsListViewModel): EventsListUiState {
        var state = EventsListUiState()
        vm.uiState.test {
            dispatcher.scheduler.advanceUntilIdle()
            state = expectMostRecentItem()
            cancelAndIgnoreRemainingEvents()
        }
        return state
    }

    @Test
    fun `lists every active event, beyond the carousel limit`() = runTest(dispatcher) {
        repository.state.value = SnapshotState.Data((1..5).map { event(it, start = today.plusDays(it.toLong())) })

        val state = settledState(viewModel())

        assertEquals(listOf(1, 2, 3, 4, 5), state.events.map { it.id })
        assertFalse(state.isLoading)
    }

    @Test
    fun `leaves out an event already over`() = runTest(dispatcher) {
        repository.state.value = SnapshotState.Data(
            listOf(event(1, start = today.minusDays(1)), event(2, start = today))
        )

        assertEquals(listOf(2), settledState(viewModel()).events.map { it.id })
    }

    @Test
    fun `no events and an answered fetch is the empty state, not an error`() = runTest(dispatcher) {
        repository.state.value = SnapshotState.Data(emptyList())

        val state = settledState(viewModel())

        assertFalse(state.isLoading)
        assertFalse(state.loadFailed)
        assertTrue(state.events.isEmpty())
    }

    @Test
    fun `a failed fetch with nothing saved is an error`() = runTest(dispatcher) {
        repository.state.value = SnapshotState.Error(AppError.Network())
        repository.refreshResult = RefreshResult.Error(IllegalStateException("offline"))

        val state = settledState(viewModel())

        assertTrue(state.loadFailed)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a failed fetch keeps the saved list`() = runTest(dispatcher) {
        repository.state.value = SnapshotState.Data(listOf(event(1, start = today)))
        repository.refreshResult = RefreshResult.Error(IllegalStateException("offline"))

        val state = settledState(viewModel())

        assertFalse(state.loadFailed)
        assertEquals(listOf(1), state.events.map { it.id })
    }

    @Test
    fun `opening the screen fetches, and retrying fetches again`() = runTest(dispatcher) {
        repository.state.value = SnapshotState.Data(emptyList())
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(1, repository.refreshCalls)

        vm.refresh()
        advanceUntilIdle()

        assertEquals(2, repository.refreshCalls)
    }
}
