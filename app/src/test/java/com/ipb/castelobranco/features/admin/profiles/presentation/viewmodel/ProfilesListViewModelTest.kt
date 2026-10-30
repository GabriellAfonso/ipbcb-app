package com.ipb.castelobranco.features.admin.profiles.presentation.viewmodel

import app.cash.turbine.test
import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.profiles.FakeProfilesAdminRepository
import com.ipb.castelobranco.features.admin.profiles.account
import com.ipb.castelobranco.features.admin.profiles.domain.model.ProfilesTab
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.ObserveManagedAccountsUseCase
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.RefreshManagedAccountsUseCase
import com.ipb.castelobranco.features.admin.profiles.pendingRequest
import com.ipb.castelobranco.features.admin.profiles.presentation.state.ProfilesEvent
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilesListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeProfilesAdminRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = ProfilesListViewModel(
        ObserveManagedAccountsUseCase(repository),
        RefreshManagedAccountsUseCase(repository),
        mockk(),
    )

    @Test
    fun `opens on Pendentes with a row per account`() = runTest {
        repository.lists.getValue(ProfilesTab.PENDING).value = listOf(
            account(pending = listOf(pendingRequest(type = AccessRequestType.DATA_CORRECTION)), blocked = true),
        )
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ProfilesTab.PENDING, state.tab)
        assertEquals(listOf(ProfilesTab.PENDING), repository.refreshed)
        val row = state.rows.single()
        assertEquals("AS", row.initials)
        assertEquals("Pedido de correção", row.pendingLabel)
        assertTrue(row.isBlocked)
    }

    @Test
    fun `the tabs count each loaded list and a pending row carries the message`() = runTest {
        repository.lists.getValue(ProfilesTab.PENDING).value = listOf(account(id = "u-1"), account(id = "u-2"))
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.pendingCount)
        assertEquals(null, vm.uiState.value.allCount)
        assertEquals("Sou membro.", vm.uiState.value.rows.first().pendingMessage)

        repository.lists.getValue(ProfilesTab.ALL).value = List(5) { account(id = "a-$it", pending = emptyList()) }
        advanceUntilIdle()
        assertEquals(5, vm.uiState.value.allCount)
    }

    @Test
    fun `switching to Todos loads that list`() = runTest {
        repository.lists.getValue(ProfilesTab.ALL).value = listOf(account(id = "u-1"), account(id = "u-2"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.selectTab(ProfilesTab.ALL)
        advanceUntilIdle()

        assertEquals(listOf(ProfilesTab.PENDING, ProfilesTab.ALL), repository.refreshed)
        assertEquals(listOf("u-1", "u-2"), vm.uiState.value.rows.map { it.id })
    }

    @Test
    fun `a failed first load shows the error and a lost scope leaves the area`() = runTest {
        repository.refreshResult = Result.failure(AppError.Network())
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.error != null)

        repository.refreshResult = Result.failure(AppError.Auth(code = 403, userMessage = "Sem permissão."))
        vm.events.test {
            vm.refresh()
            advanceUntilIdle()
            assertEquals(ProfilesEvent.LeaveArea("Sem permissão."), awaitItem())
        }
    }
}
