package com.ipb.castelobranco.features.admin.profiles.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.features.admin.members.data.api.FakeMembersAdminApi
import com.ipb.castelobranco.features.admin.members.data.dto.MemberListDto
import com.ipb.castelobranco.features.admin.members.data.photo.FakeMemberPhotoStore
import com.ipb.castelobranco.features.admin.members.data.repository.MembersAdminRepositoryImpl
import com.ipb.castelobranco.features.admin.members.domain.usecase.ObserveMembersUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.RefreshMembersUseCase
import com.ipb.castelobranco.features.admin.members.ok
import com.ipb.castelobranco.features.admin.members.summaryDto
import com.ipb.castelobranco.features.admin.profiles.FakeProfilesAdminRepository
import com.ipb.castelobranco.features.admin.profiles.account
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.ApproveAccessRequestUseCase
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.DenyAccessRequestUseCase
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.GetManagedAccountUseCase
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.LinkAccountToMemberUseCase
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.RevokeMemberAccessUseCase
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.SetAccessRequestsBlockedUseCase
import com.ipb.castelobranco.features.admin.profiles.presentation.state.ConfirmAction
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeProfilesAdminRepository()
    private val access = FakeAccessRepository(accessOf(Role.LEADER, Scope.PROFILES to AccessLevel.MANAGE))
    private val membersApi = FakeMembersAdminApi()
    private val members = MembersAdminRepositoryImpl(membersApi, FakeMemberPhotoStore())

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = ProfileDetailViewModel(
        SavedStateHandle(mapOf(ProfileDetailViewModel.ARG_USER_ID to "u-1")),
        GetManagedAccountUseCase(repository),
        ApproveAccessRequestUseCase(repository),
        DenyAccessRequestUseCase(repository),
        RevokeMemberAccessUseCase(repository),
        SetAccessRequestsBlockedUseCase(repository),
        LinkAccountToMemberUseCase(repository),
        ObserveMembersUseCase(members),
        RefreshMembersUseCase(members),
        ObserveAccessUseCase(access),
        mockk(),
    )

    @Test
    fun `loads the account and offers actions with manage`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals("Ana Souza", vm.uiState.value.account?.name)
        assertTrue(vm.uiState.value.canManage)
    }

    @Test
    fun `view only hides the actions`() = runTest {
        access.state.value = accessOf(Role.MEDIA, Scope.PROFILES to AccessLevel.VIEW)
        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.canManage)
    }

    @Test
    fun `approving replaces the account and confirms`() = runTest {
        repository.actionResult = Result.success(account(memberAccess = true, pending = emptyList()))
        val vm = viewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.approve(41)
            advanceUntilIdle()
            assertEquals(ProfilesEvent.ShowMessage(ProfileDetailViewModel.APPROVED_MESSAGE), awaitItem())
        }
        assertEquals(listOf("approve 41"), repository.actions)
        assertTrue(vm.uiState.value.account!!.memberAccess)
    }

    @Test
    fun `deny sends the trimmed reason and closes the dialog`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.openDeny(41)
        vm.deny("  Procure o pastor.  ")
        advanceUntilIdle()

        assertEquals(listOf("deny 41 Procure o pastor."), repository.actions)
        assertNull(vm.uiState.value.denyDialog)
    }

    @Test
    fun `a conflict says why and reloads the account`() = runTest {
        repository.actionResult = Result.failure(
            AppError.Server(code = 409, errorCode = "ACCESS_REQUEST_NOT_PENDING", userMessage = "Já decidido.")
        )
        val vm = viewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.approve(41)
            advanceUntilIdle()
            assertEquals(ProfilesEvent.ShowMessage("Já decidido."), awaitItem())
        }
        assertEquals(2, repository.detailCalls)
    }

    @Test
    fun `revoke and unlink go through a confirmation`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.askRevoke()
        assertEquals(ConfirmAction.REVOKE, vm.uiState.value.confirm)
        vm.confirm()
        advanceUntilIdle()
        vm.askUnlink()
        vm.confirm()
        advanceUntilIdle()

        assertEquals(listOf("revoke u-1", "link u-1 null"), repository.actions)
        assertNull(vm.uiState.value.confirm)
    }

    @Test
    fun `block toggles the requests block`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.setBlocked(true)
        advanceUntilIdle()

        assertEquals(listOf("block u-1 true"), repository.actions)
    }

    @Test
    fun `the picker searches the roll and links the picked member`() = runTest {
        membersApi.onGetMembers = { ok(MemberListDto(listOf(summaryDto(12, "Ana Souza"), summaryDto(13, "Bruno")))) }
        val vm = viewModel()
        advanceUntilIdle()

        vm.openPicker()
        advanceUntilIdle()
        vm.onPickerQuery("ana")
        assertEquals(listOf(12), vm.uiState.value.picker?.members?.map { it.id })

        vm.pick(12)
        advanceUntilIdle()

        assertEquals(listOf("link u-1 12"), repository.actions)
        assertNull(vm.uiState.value.picker)
    }

    @Test
    fun `a member already linked shows the server message`() = runTest {
        repository.actionResult = Result.failure(
            AppError.Server(code = 409, errorCode = "MEMBER_ALREADY_LINKED", userMessage = "Membro já vinculado.")
        )
        val vm = viewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.pick(12)
            advanceUntilIdle()
            assertEquals(ProfilesEvent.ShowMessage("Membro já vinculado."), awaitItem())
        }
    }
}
