package com.ipb.castelobranco.features.admin.members.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.features.admin.members.presentation.navigation.MembersRoutes
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
import org.junit.Before
import org.junit.Test

/** The profile's actions follow the level on `members` (spec 006, FR-015–FR-017). */
@OptIn(ExperimentalCoroutinesApi::class)
class MemberProfileFlagsTest {

    private val access = FakeAccessRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = MemberProfileViewModel(
        savedStateHandle = SavedStateHandle(mapOf(MembersRoutes.ARG_MEMBER_ID to 1)),
        getMember = mockk(relaxed = true),
        getHistory = mockk(relaxed = true),
        uploadPhoto = mockk(relaxed = true),
        removePhoto = mockk(relaxed = true),
        deleteMember = mockk(relaxed = true),
        computeAge = mockk(relaxed = true),
        observeAccess = ObserveAccessUseCase(access),
        imageLoader = mockk(relaxed = true),
    )

    private data class Flags(val edit: Boolean, val photo: Boolean, val delete: Boolean, val remove: Boolean)

    private fun MemberProfileViewModel.flags() = uiState.value.let {
        Flags(it.canEdit, it.canChangePhoto, it.canDelete, it.canRemovePhoto)
    }

    @Test
    fun `view only reads`() = runTest {
        access.state.value = accessOf(Role.LEADER, Scope.MEMBERS to AccessLevel.VIEW)
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(Flags(edit = false, photo = false, delete = false, remove = false), vm.flags())
    }

    @Test
    fun `manage edits and changes the photo but never deletes`() = runTest {
        access.state.value = accessOf(Role.LEADER, Scope.MEMBERS to AccessLevel.MANAGE)
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(Flags(edit = true, photo = true, delete = false, remove = false), vm.flags())
    }

    @Test
    fun `owner can do everything`() = runTest {
        access.state.value = accessOf(Role.ADMIN, Scope.MEMBERS to AccessLevel.OWNER)
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(Flags(edit = true, photo = true, delete = true, remove = true), vm.flags())
    }

    @Test
    fun `losing owner hides delete in place and blocks the dialog`() = runTest {
        access.state.value = accessOf(Role.ADMIN, Scope.MEMBERS to AccessLevel.OWNER)
        val vm = viewModel()
        advanceUntilIdle()

        access.state.value = accessOf(Role.LEADER, Scope.MEMBERS to AccessLevel.MANAGE)
        advanceUntilIdle()
        vm.onDeleteRequested()
        vm.onRemovePhotoRequested()

        assertFalse(vm.uiState.value.canDelete)
        assertFalse(vm.uiState.value.showDeleteDialog)
        assertFalse(vm.uiState.value.showRemovePhotoDialog)
    }
}
