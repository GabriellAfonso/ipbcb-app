package com.ipb.castelobranco.features.admin.members.presentation.viewmodel

import com.ipb.castelobranco.features.admin.members.domain.repository.MemberPhotoExporter
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.members.domain.usecase.DownloadMemberPhotoUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.ObserveMemberPhotoRevisionsUseCase
import com.ipb.castelobranco.features.admin.members.data.photo.FakeMemberPhotoStore
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.features.admin.members.apiError
import com.ipb.castelobranco.features.admin.members.data.api.FakeMembersAdminApi
import com.ipb.castelobranco.features.admin.members.data.dto.HistoryDto
import com.ipb.castelobranco.features.admin.members.data.dto.PhotoUrlDto
import com.ipb.castelobranco.features.admin.members.data.repository.MembersAdminRepositoryImpl
import com.ipb.castelobranco.features.admin.members.domain.model.BirthDate
import com.ipb.castelobranco.features.admin.members.domain.usecase.BuildHistorySentenceUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.ComputeMemberAgeUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.DeleteMemberUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.GetMemberHistoryUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.GetMemberUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.RemoveMemberPhotoUseCase
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
    private val repository = MembersAdminRepositoryImpl(api, FakeMemberPhotoStore())
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
        uploadPhoto = UploadMemberPhotoUseCase(ValidateMemberPhotoUseCase(), repository),
        removePhoto = RemoveMemberPhotoUseCase(repository),
        deleteMember = DeleteMemberUseCase(repository),
        computeAge = ComputeMemberAgeUseCase(fixedDateProvider),
        // Owner: these tests exercise every action; the level gates live in MemberProfileFlagsTest.
        observeAccess = ObserveAccessUseCase(
            FakeAccessRepository(accessOf(Role.ADMIN, Scope.MEMBERS to AccessLevel.OWNER))
        ),
        observePhotoRevisions = ObserveMemberPhotoRevisionsUseCase(photos),
        downloadPhoto = DownloadMemberPhotoUseCase(exporter, fixedDateProvider),
        imageLoader = mockk(),
    ).also { it.load() }

    private val photos = FakeMemberPhotoStore()
    private val exporter = FakeMemberPhotoExporter()

    // region download

    @Test
    fun `download saves the photo under the member name and says where`() = runTest {
        api.onGetMember = { id, _ -> ok(recordDto(id = id, photoUrl = PHOTO_URL)) }
        val vm = viewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.onDownloadPhoto()
            assertEquals(MembersEvent.ShowMessage(MemberProfileViewModel.PHOTO_SAVED), awaitItem())
        }
        assertEquals(1, exporter.saved.size)
        assertEquals(PHOTO_URL, exporter.saved.single().second)
        assertTrue(exporter.saved.single().first.endsWith(fixedDateProvider.today().toString()))
        assertFalse(vm.uiState.value.isDownloadingPhoto)
    }

    @Test
    fun `failed download shows the message and stays in the area`() = runTest {
        api.onGetMember = { id, _ -> ok(recordDto(id = id, photoUrl = PHOTO_URL)) }
        exporter.result = Result.failure(AppError.Auth(code = 403, userMessage = "Sem permissão."))
        val vm = viewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.onDownloadPhoto()
            assertEquals(MembersEvent.ShowMessage("Sem permissão."), awaitItem())
        }
    }

    @Test
    fun `second tap while downloading is ignored`() = runTest {
        api.onGetMember = { id, _ -> ok(recordDto(id = id, photoUrl = PHOTO_URL)) }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDownloadPhoto()
        vm.onDownloadPhoto()
        advanceUntilIdle()

        assertEquals(1, exporter.saved.size)
    }

    @Test
    fun `no photo means nothing to download`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDownloadPhoto()
        advanceUntilIdle()

        assertTrue(exporter.saved.isEmpty())
    }

    @Test
    fun `a bumped revision of the shown photo reaches the state`() = runTest {
        api.onGetMember = { id, _ -> ok(recordDto(id = id, photoUrl = PHOTO_URL)) }
        val vm = viewModel()
        advanceUntilIdle()

        photos.revisions.value = mapOf(PHOTO_URL to 2)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.photoRevision)
    }

    // endregion

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
                    id = id, name = "Carla", birth = BirthDate.NONE, baptismDate = null, role = null,
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
    fun `day and month only show dd-MM and an unknown age`() = runTest {
        api.onGetMember = { id, _ -> ok(recordDto(id = id, birth = BirthDate(day = 2, month = 4))) }
        val vm = viewModel()
        advanceUntilIdle()

        val profile = vm.uiState.value.profile!!
        assertEquals("02/04", profile.birthDateLabel)
        assertEquals("Desconhecida", profile.ageLabel)
        assertEquals("Feminino", profile.headline)
    }

    @Test
    fun `year only shows the year and the age by year`() = runTest {
        api.onGetMember = { id, _ -> ok(recordDto(id = id, birth = BirthDate(year = 1990))) }
        val vm = viewModel()
        advanceUntilIdle()

        val profile = vm.uiState.value.profile!!
        assertEquals("1990", profile.birthDateLabel)
        assertEquals("36 anos", profile.ageLabel)
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

private const val PHOTO_URL = "https://gabrielafonso.com.br/ipbcb/media/members/abc.jpg"

private class FakeMemberPhotoExporter : MemberPhotoExporter {
    val saved = mutableListOf<Pair<String, String>>()
    var result: Result<Unit> = Result.success(Unit)

    override suspend fun save(fileBaseName: String, url: String): Result<Unit> {
        saved += fileBaseName to url
        return result
    }
}
