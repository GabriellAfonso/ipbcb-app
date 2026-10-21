package com.ipb.castelobranco.features.admin.events.presentation.viewmodel

import app.cash.turbine.test
import coil.ImageLoader
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.features.admin.events.domain.model.EventStatusFilter
import com.ipb.castelobranco.features.admin.events.domain.model.EventVisibility
import com.ipb.castelobranco.features.admin.events.domain.usecase.ListEventsUseCase
import com.ipb.castelobranco.features.admin.events.presentation.state.EventsAdminEvent
import com.ipb.castelobranco.features.admin.events.testing.FakeEventsAdminRepository
import com.ipb.castelobranco.features.admin.events.testing.record
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
class EventsAdminListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeEventsAdminRepository()
    private val access = FakeAccessRepository(accessOf(Role.MEDIA, Scope.EVENTS to AccessLevel.VIEW))
    private val today = LocalDate.of(2026, 11, 20)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = EventsAdminListViewModel(
        listEvents = ListEventsUseCase(repository),
        dateProvider = DateProvider { today },
        observeAccess = ObserveAccessUseCase(access),
        imageLoader = mockk<ImageLoader>(),
    )

    @Test
    fun `loads all by default and marks past and members-only rows`() = runTest(dispatcher) {
        repository.listResult = Result.success(
            listOf(
                record(id = 1, start = LocalDate.of(2026, 12, 1), visibility = EventVisibility.MEMBERS),
                record(id = 2, start = LocalDate.of(2026, 11, 1), time = null),
            )
        )
        val vm = viewModel()

        vm.load()
        advanceUntilIdle()

        val items = vm.uiState.value.items
        assertEquals(listOf(EventStatusFilter.ALL), repository.listCalls)
        assertTrue(items[0].isMembersOnly && !items[0].isPast)
        assertTrue(items[1].isPast)
        assertEquals("1 nov", items[1].dateLabel)
        assertFalse(vm.uiState.value.canCreate)
    }

    @Test
    fun `a filter change reloads with that status`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.load()
        advanceUntilIdle()

        vm.onFilterSelected(EventStatusFilter.PAST)
        advanceUntilIdle()

        assertEquals(listOf(EventStatusFilter.ALL, EventStatusFilter.PAST), repository.listCalls)
    }

    @Test
    fun `a refused read leaves the area`() = runTest(dispatcher) {
        repository.listResult = Result.failure(AppError.Auth(code = 403))
        val vm = viewModel()

        vm.events.test {
            vm.load()
            advanceUntilIdle()
            assertTrue(awaitItem() is EventsAdminEvent.LeaveArea)
        }
    }

    @Test
    fun `a network failure shows an error with retry`() = runTest(dispatcher) {
        repository.listResult = Result.failure(AppError.Network())
        val vm = viewModel()

        vm.load()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.error != null)
    }
}
