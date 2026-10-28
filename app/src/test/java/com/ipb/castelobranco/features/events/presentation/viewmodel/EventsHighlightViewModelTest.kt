package com.ipb.castelobranco.features.events.presentation.viewmodel

import coil.ImageLoader
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.features.events.domain.usecase.ObserveActiveEventsUseCase
import com.ipb.castelobranco.features.events.domain.usecase.ObserveHighlightEventsUseCase
import com.ipb.castelobranco.features.events.domain.usecase.RefreshEventsUseCase
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class EventsHighlightViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 11, 10)
    private val repository = FakeEventsRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = EventsHighlightViewModel(
        observeHighlightEvents = ObserveHighlightEventsUseCase(
            ObserveActiveEventsUseCase(repository, DateProvider { today })
        ),
        refreshEvents = RefreshEventsUseCase(repository),
        imageLoader = mockk<ImageLoader>(),
    )

    @Test
    fun `pages carry title and date label`() = runTest(dispatcher) {
        repository.state.value = SnapshotState.Data(
            listOf(event(7, start = LocalDate.of(2026, 11, 14), time = LocalTime.of(19, 30), title = "Culto"))
        )
        val vm = viewModel()
        advanceUntilIdle()

        val page = vm.pages.value!!.single()
        assertEquals(7, page.id)
        assertEquals("Culto", page.title)
        assertEquals("14 nov · 19:30", page.dateLabel)
    }

    @Test
    fun `pages are null while nothing is loaded`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        assertNull(vm.pages.value)
    }

    @Test
    fun `the first login value does not refresh, a change does`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.onLoginStateChanged(true)
        advanceUntilIdle()
        assertEquals(0, repository.refreshCalls)

        vm.onLoginStateChanged(true)
        advanceUntilIdle()
        assertEquals(0, repository.refreshCalls)

        vm.onLoginStateChanged(false)
        advanceUntilIdle()
        assertEquals(1, repository.refreshCalls)
    }
}
