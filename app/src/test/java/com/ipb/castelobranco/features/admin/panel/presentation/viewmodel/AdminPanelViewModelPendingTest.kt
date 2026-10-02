package com.ipb.castelobranco.features.admin.panel.presentation.viewmodel

import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.core.testing.setlistOf
import com.ipb.castelobranco.features.admin.members.data.api.FakeMembersAdminApi
import com.ipb.castelobranco.features.admin.members.data.repository.MembersAdminRepositoryImpl
import com.ipb.castelobranco.features.admin.members.domain.usecase.ObserveMembersUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.RefreshMembersUseCase
import com.ipb.castelobranco.features.admin.panel.domain.VisiblePanelCardsUseCase
import com.ipb.castelobranco.features.admin.panel.presentation.state.PendingConfirmationsUi
import com.ipb.castelobranco.features.admin.register.domain.FakeSetlistConfirmationRepository
import com.ipb.castelobranco.features.admin.register.domain.usecase.GetPendingConfirmationsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AdminPanelViewModelPendingTest {

    private val dispatcher = StandardTestDispatcher()
    private val members = MembersAdminRepositoryImpl(FakeMembersAdminApi())
    private val access = FakeAccessRepository()
    private val setlists = FakeSetlistConfirmationRepository()

    private val older = LocalDate.of(2026, 9, 27)
    private val newer = LocalDate.of(2026, 10, 4)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = AdminPanelViewModel(
        ObserveAccessUseCase(access),
        VisiblePanelCardsUseCase(),
        ObserveMembersUseCase(members),
        RefreshMembersUseCase(members),
        GetPendingConfirmationsUseCase(setlists),
    )

    private fun manager() {
        access.state.value = accessOf(Role.LEADER, Scope.SONGS to AccessLevel.MANAGE)
    }

    @Test
    fun `pending Sundays are listed newest first`() = runTest {
        manager()
        setlists.pendingResult = Result.success(listOf(setlistOf(older, 1), setlistOf(newer, 2)))
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.refreshPending()
        advanceUntilIdle()

        assertEquals(PendingConfirmationsUi.Dates(listOf(newer, older)), vm.uiState.value.pending)
    }

    @Test
    fun `nothing pending hides the card`() = runTest {
        manager()
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.refreshPending()
        advanceUntilIdle()

        assertEquals(PendingConfirmationsUi.Hidden, vm.uiState.value.pending)
    }

    @Test
    fun `without manage on songs nothing is read`() = runTest {
        access.state.value = accessOf(Role.MEDIA, Scope.GALLERY to AccessLevel.MANAGE)
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.refreshPending()
        advanceUntilIdle()

        assertEquals(PendingConfirmationsUi.Hidden, vm.uiState.value.pending)
        assertEquals(0, setlists.pendingCalls)
    }

    @Test
    fun `a failed read shows an error that can be retried`() = runTest {
        manager()
        setlists.pendingResult = Result.failure(AppError.Network())
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.refreshPending()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.pending is PendingConfirmationsUi.Failed)

        setlists.pendingResult = Result.success(listOf(setlistOf(newer, 2)))
        vm.refreshPending()
        advanceUntilIdle()
        assertEquals(PendingConfirmationsUi.Dates(listOf(newer)), vm.uiState.value.pending)
    }

    @Test
    fun `a Sunday registered meanwhile leaves on the next read`() = runTest {
        manager()
        setlists.pendingResult = Result.success(listOf(setlistOf(newer, 1), setlistOf(older, 2)))
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.refreshPending()
        advanceUntilIdle()

        setlists.pendingResult = Result.success(listOf(setlistOf(older, 2)))
        vm.refreshPending()
        advanceUntilIdle()

        assertEquals(PendingConfirmationsUi.Dates(listOf(older)), vm.uiState.value.pending)
    }
}
