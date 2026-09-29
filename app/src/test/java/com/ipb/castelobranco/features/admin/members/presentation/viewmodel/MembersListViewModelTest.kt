package com.ipb.castelobranco.features.admin.members.presentation.viewmodel

import app.cash.turbine.test
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.features.admin.members.apiError
import com.ipb.castelobranco.features.admin.members.data.api.FakeMembersAdminApi
import com.ipb.castelobranco.features.admin.members.data.dto.MemberListDto
import com.ipb.castelobranco.features.admin.members.data.repository.MembersAdminRepositoryImpl
import com.ipb.castelobranco.features.admin.members.domain.usecase.ObserveMembersUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.RefreshMembersUseCase
import com.ipb.castelobranco.features.admin.members.ok
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.summaryDto
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MembersListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeMembersAdminApi()
    private val repository = MembersAdminRepositoryImpl(api)
    private val access = FakeAccessRepository()

    private val roll = MemberListDto(
        listOf(
            summaryDto(1, "Ana Souza"),
            summaryDto(2, "José Ribeiro", status = null),
            summaryDto(3, "Joselina Prado", isActive = false),
        )
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        MembersListViewModel(
            ObserveMembersUseCase(repository),
            RefreshMembersUseCase(repository),
            ObserveAccessUseCase(access),
            mockk(),
        )

    @Test
    fun `new member is offered only with manage`() = runTest {
        api.onGetMembers = { ok(roll) }
        val vm = viewModel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.canAdd)

        access.state.value = accessOf(Role.LEADER, Scope.MEMBERS to AccessLevel.MANAGE)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.canAdd)
    }

    @Test
    fun `loads the roll into cards`() = runTest {
        api.onGetMembers = { ok(roll) }
        val vm = viewModel()
        assertTrue(vm.uiState.value.isLoading)

        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.members.size)
        assertEquals("Sem situação", state.members[1].statusLabel)
        assertFalse(state.members[2].isValid)
        assertEquals("AS", state.members[0].initials)
    }

    @Test
    fun `search ignores accents and case`() = runTest {
        api.onGetMembers = { ok(roll) }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onQueryChange("jose")
        advanceUntilIdle()

        assertEquals(listOf("José Ribeiro", "Joselina Prado"), vm.uiState.value.members.map { it.name })
    }

    @Test
    fun `no search result is told apart from an empty roll`() = runTest {
        api.onGetMembers = { ok(roll) }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onQueryChange("zzz")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.members.isEmpty())
        assertEquals(3, vm.uiState.value.totalCount)
    }

    @Test
    fun `load failure shows an error to retry`() = runTest {
        api.onGetMembers = { throw IOException("offline") }
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error!!.contains("conexão"))

        api.onGetMembers = { ok(roll) }
        vm.refresh()
        advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        assertEquals(3, vm.uiState.value.members.size)
    }

    @Test
    fun `a refused leader leaves the area`() = runTest {
        api.onGetMembers = { apiError(403, "PERMISSION_DENIED", "Apenas líderes.") }
        val vm = viewModel()

        vm.events.test {
            advanceUntilIdle()
            assertEquals(MembersEvent.LeaveArea("Apenas líderes."), awaitItem())
        }
    }
}
