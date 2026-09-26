package com.ipb.castelobranco.features.admin.members.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ipb.castelobranco.features.admin.members.apiError
import com.ipb.castelobranco.features.admin.members.data.api.FakeMembersAdminApi
import com.ipb.castelobranco.features.admin.members.data.dto.HistoryDto
import com.ipb.castelobranco.features.admin.members.data.dto.PhotoUrlDto
import com.ipb.castelobranco.features.admin.members.data.repository.MembersAdminRepositoryImpl
import com.ipb.castelobranco.features.admin.members.domain.usecase.BuildHistorySentenceUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.ComputeMemberAgeUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.DeleteMemberUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.GetMemberHistoryUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.GetMemberUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.RemoveMemberPhotoUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.SetMemberValidityUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.UploadMemberPhotoUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberPhotoUseCase
import com.ipb.castelobranco.features.admin.members.fixedDateProvider
import com.ipb.castelobranco.features.admin.members.historyDto
import com.ipb.castelobranco.features.admin.members.ok
import com.ipb.castelobranco.features.admin.members.presentation.navigation.MembersRoutes
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.recordDto
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
class MemberProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeMembersAdminApi()
    private val repository = MembersAdminRepositoryImpl(api)
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        api.onGetMember = { id, _ -> ok(recordDto(id = id)) }
        api.onGetHistory = { ok(HistoryDto(listOf(historyDto(40, "status", "Visitante", "Ativo")))) }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = MemberProfileViewModel(
        savedStateHandle = SavedStateHandle(mapOf(MembersRoutes.ARG_MEMBER_ID to 12)),
        getMember = GetMemberUseCase(repository),
        getHistory = GetMemberHistoryUseCase(repository, BuildHistorySentenceUseCase()),
        setValidity = SetMemberValidityUseCase(repository),
        uploadPhoto = UploadMemberPhotoUseCase(ValidateMemberPhotoUseCase(), repository),
        removePhoto = RemoveMemberPhotoUseCase(repository),
        deleteMember = DeleteMemberUseCase(repository),
        computeAge = ComputeMemberAgeUseCase(fixedDateProvider),
        imageLoader = mockk(),
    ).also { it.load() }

    // region read

    @Test
    fun `profile shows every label already formatted`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val profile = vm.uiState.value.profile!!
        assertEquals("36 anos · Feminino", profile.headline)
        assertEquals("02/04/1990", profile.birthDateLabel)
        assertEquals("12/06/2005 · há 21 anos", profile.baptismLabel)
        assertEquals("Ativo", profile.statusLabel)
        assertEquals(listOf("Louvor", "Recepção"), profile.ministries)
        assertEquals("alterou Situação de Visitante para Ativo", vm.uiState.value.lastChange!!.text)
    }

    @Test
    fun `empty fields are never blank`() = runTest {
        api.onGetMember = { id, _ ->
            ok(
                recordDto(
                    id = id, name = "Carla", birthDate = null, baptismDate = null, role = null,
                    ministries = emptyList(),
                )
            )
        }
        val vm = viewModel()
        advanceUntilIdle()

        val profile = vm.uiState.value.profile!!
        assertEquals("Não informado", profile.birthDateLabel)
        assertEquals("Não informado", profile.ageLabel)
        assertEquals("Não informado", profile.baptismLabel)
        assertEquals("Não informado", profile.lastName)
        assertEquals("Sem cargo", profile.roleText)
        assertNull(profile.roleLabel)
        assertTrue(profile.ministries.isEmpty())
    }

    @Test
    fun `birth year 0001 shows day and month and no age`() = runTest {
        api.onGetMember = { id, _ -> ok(recordDto(id = id, birthDate = "0001-04-02")) }
        val vm = viewModel()
        advanceUntilIdle()

        val profile = vm.uiState.value.profile!!
        assertEquals("02/04 (ano não informado)", profile.birthDateLabel)
        assertEquals("Idade não informada", profile.ageLabel)
        assertEquals("Feminino", profile.headline)
    }

    @Test
    fun `a vanished member sends the leader back to the list`() = runTest {
        api.onGetMember = { _, _ -> apiError(404, "NOT_FOUND", "Not found.") }
        val vm = viewModel()

        vm.events.test {
            advanceUntilIdle()
            assertEquals(MembersEvent.MemberGone("Este membro não existe mais"), awaitItem())
        }
    }

    @Test
    fun `a refused leader leaves the area`() = runTest {
        api.onGetMember = { _, _ -> apiError(403, "PERMISSION_DENIED", "Apenas líderes.") }
        val vm = viewModel()

        vm.events.test {
            advanceUntilIdle()
            assertEquals(MembersEvent.LeaveArea("Apenas líderes."), awaitItem())
        }
    }

    @Test
    fun `connection failure with nothing on screen is the screen error`() = runTest {
        api.onGetMember = { _, _ -> throw IOException("offline") }
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.error!!.contains("conexão"))
        assertNull(vm.uiState.value.profile)
    }

    // endregion

    // region validity

    @Test
    fun `validity switch saves at once`() = runTest {
        api.onUpdateMember = { id, _ -> ok(recordDto(id = id, isActive = false)) }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onValidityChanged(false)
        assertFalse(vm.uiState.value.profile!!.isValid)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.profile!!.isValid)
        assertEquals(setOf("is_active"), api.updatedBodies.single().second.keys)
    }

    @Test
    fun `validity switch goes back when the save fails`() = runTest {
        api.onUpdateMember = { _, _ -> throw IOException("offline") }
        val vm = viewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.onValidityChanged(false)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.profile!!.isValid)
            assertTrue((awaitItem() as MembersEvent.ShowMessage).message.contains("conexão"))
        }
    }

    // endregion

    // region photo

    @Test
    fun `picked photo is uploaded and shown`() = runTest {
        api.onUploadPhoto = { _, _ -> ok(PhotoUrlDto("https://host/media/members/n.jpg")) }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onPhotoPicked(jpeg)
        advanceUntilIdle()

        assertEquals("https://host/media/members/n.jpg", vm.uiState.value.profile!!.photoUrl)
        assertFalse(vm.uiState.value.isPhotoBusy)
    }

    @Test
    fun `a file that is not an image is refused before uploading`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.onPhotoPicked(byteArrayOf(1, 2, 3, 4))
            advanceUntilIdle()

            assertEquals(MembersEvent.ShowMessage("Use uma imagem JPEG, PNG, WEBP ou GIF."), awaitItem())
        }
        assertTrue(api.uploadedParts.isEmpty())
    }

    @Test
    fun `removing the photo asks first and then clears it`() = runTest {
        api.onGetMember = { id, _ -> ok(recordDto(id = id, photoUrl = "https://host/a.jpg")) }
        api.onRemovePhoto = { ok(Unit) }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onRemovePhotoRequested()
        assertTrue(vm.uiState.value.showRemovePhotoDialog)
        vm.onRemovePhotoConfirmed()
        advanceUntilIdle()

        assertNull(vm.uiState.value.profile!!.photoUrl)
        assertFalse(vm.uiState.value.showRemovePhotoDialog)
    }

    // endregion

    // region delete

    @Test
    fun `delete is enabled only by the member name and then leaves`() = runTest {
        api.onDeleteMember = { ok(Unit) }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDeleteRequested()
        vm.onDeleteTypedChange("Ana")
        assertFalse(vm.uiState.value.canConfirmDelete)
        val callsBefore = api.calls
        vm.onDeleteConfirmed()
        advanceUntilIdle()
        assertEquals(callsBefore, api.calls)

        vm.onDeleteTypedChange(" ana souza ")
        assertTrue(vm.uiState.value.canConfirmDelete)
        vm.events.test {
            vm.onDeleteConfirmed()
            advanceUntilIdle()
            assertEquals(MembersEvent.Deleted, awaitItem())
        }
    }

    @Test
    fun `a failed delete keeps the profile and says why`() = runTest {
        api.onDeleteMember = { throw IOException("offline") }
        val vm = viewModel()
        advanceUntilIdle()
        vm.onDeleteRequested()
        vm.onDeleteTypedChange("Ana Souza")

        vm.events.test {
            vm.onDeleteConfirmed()
            advanceUntilIdle()
            assertTrue(awaitItem() is MembersEvent.ShowMessage)
        }
        assertFalse(vm.uiState.value.showDeleteDialog)
        assertEquals("Ana Souza", vm.uiState.value.profile!!.name)
    }

    // endregion
}
