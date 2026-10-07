package com.ipb.castelobranco.features.admin.panel.presentation.viewmodel

import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.worship.ObserveWorshipAccessUseCase
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.FakeWorshipAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.features.admin.members.apiError
import com.ipb.castelobranco.features.admin.members.data.api.FakeMembersAdminApi
import com.ipb.castelobranco.features.admin.members.data.dto.MemberListDto
import com.ipb.castelobranco.features.admin.members.data.repository.MembersAdminRepositoryImpl
import com.ipb.castelobranco.features.admin.members.domain.usecase.ObserveMembersUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.RefreshMembersUseCase
import com.ipb.castelobranco.features.admin.members.ok
import com.ipb.castelobranco.features.admin.members.summaryDto
import com.ipb.castelobranco.features.admin.panel.domain.VisiblePanelCardsUseCase
import com.ipb.castelobranco.features.admin.register.domain.FakeSetlistConfirmationRepository
import com.ipb.castelobranco.features.admin.register.domain.usecase.DeletePendingSetlistUseCase
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminPanelViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeMembersAdminApi()
    private val repository = MembersAdminRepositoryImpl(api)
    private val access = FakeAccessRepository()
    private val setlists = FakeSetlistConfirmationRepository()

    private val roll = MemberListDto(listOf(summaryDto(1, "Ana Souza"), summaryDto(2, "José Ribeiro")))

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        AdminPanelViewModel(
            ObserveAccessUseCase(access),
            VisiblePanelCardsUseCase(),
            ObserveMembersUseCase(repository),
            ObserveWorshipAccessUseCase(FakeWorshipAccessRepository()),
            RefreshMembersUseCase(repository),
            GetPendingConfirmationsUseCase(setlists),
            DeletePendingSetlistUseCase(setlists),
        )

    @Test
    fun `members card shows the roll size`() = runTest {
        api.onGetMembers = { ok(roll) }
        access.state.value = accessOf(Role.LEADER, Scope.MEMBERS to AccessLevel.VIEW)
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.memberCount)
    }

    @Test
    fun `failed load hides the count`() = runTest {
        api.onGetMembers = { apiError(500) }
        access.state.value = accessOf(Role.LEADER, Scope.MEMBERS to AccessLevel.VIEW)
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        advanceUntilIdle()

        assertNull(vm.uiState.value.memberCount)
    }

    @Test
    fun `no members card means no request`() = runTest {
        var calls = 0
        api.onGetMembers = { calls++; ok(roll) }
        access.state.value = accessOf(Role.LEADER, Scope.HYMNAL_HISTORY_REPORT to AccessLevel.VIEW)
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        advanceUntilIdle()

        assertEquals(0, calls)
        assertNull(vm.uiState.value.memberCount)
    }
}
