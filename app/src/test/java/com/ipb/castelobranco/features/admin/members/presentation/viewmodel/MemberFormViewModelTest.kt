package com.ipb.castelobranco.features.admin.members.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ipb.castelobranco.features.admin.members.apiError
import com.ipb.castelobranco.features.admin.members.data.api.FakeMembersAdminApi
import com.ipb.castelobranco.features.admin.members.data.dto.PhotoUrlDto
import com.ipb.castelobranco.features.admin.members.data.repository.MembersAdminRepositoryImpl
import com.ipb.castelobranco.features.admin.members.domain.model.BirthDate
import com.ipb.castelobranco.features.admin.members.domain.model.MemberField
import com.ipb.castelobranco.features.admin.members.domain.usecase.GetMemberOptionsUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.GetMemberUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.SaveMemberUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.UploadMemberPhotoUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberDraftUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberPhotoUseCase
import com.ipb.castelobranco.features.admin.members.fieldError
import com.ipb.castelobranco.features.admin.members.fixedDateProvider
import com.ipb.castelobranco.features.admin.members.ok
import com.ipb.castelobranco.features.admin.members.optionsDto
import com.ipb.castelobranco.features.admin.members.presentation.navigation.MembersRoutes
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.recordDto
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonNull
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MemberFormViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeMembersAdminApi()
    private val repository = MembersAdminRepositoryImpl(api)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        api.onGetOptions = { ok(optionsDto()) }
        api.onGetMember = { id, _ -> ok(recordDto(id = id)) }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** @param memberId as navigation stores it: a String from the optional query argument. */
    private fun viewModel(memberId: String? = null) = MemberFormViewModel(
        savedStateHandle = SavedStateHandle(
            memberId?.let { mapOf(MembersRoutes.ARG_MEMBER_ID to it) } ?: emptyMap()
        ),
        getMember = GetMemberUseCase(repository),
        getOptions = GetMemberOptionsUseCase(repository),
        validate = ValidateMemberDraftUseCase(fixedDateProvider),
        saveMember = SaveMemberUseCase(repository),
        validatePhoto = ValidateMemberPhotoUseCase(),
        uploadPhoto = UploadMemberPhotoUseCase(ValidateMemberPhotoUseCase(), repository),
        imageLoader = mockk(),
    )

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0)

    @Test
    fun `new member starts empty and valid with the server options`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isEditing)
        assertEquals("", state.draft.name)
        assertTrue(state.draft.isValid)
        assertEquals(listOf("Ativo", "Inativo", "Visitante"), state.options!!.statuses.map { it.name })
    }

    @Test
    fun `edit loads the member into the draft`() = runTest {
        val vm = viewModel(memberId = "12")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEditing)
        assertEquals("Ana Souza", vm.uiState.value.draft.name)
        assertEquals(setOf(2, 5), vm.uiState.value.draft.ministryIds)
    }

    @Test
    fun `local errors block the save`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDraftChanged(vm.uiState.value.draft.copy(name = " ", birth = BirthDate(1, 1, 2030)))
        vm.onSave()
        advanceUntilIdle()

        assertEquals(setOf(MemberField.NAME, MemberField.BIRTH_YEAR), vm.uiState.value.fieldErrors.keys)
        assertTrue(api.createdBodies.isEmpty())
    }

    @Test
    fun `server field errors land on their fields`() = runTest {
        api.onCreateMember = { fieldError("status_id", "Situação inexistente.") }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDraftChanged(vm.uiState.value.draft.copy(name = "Bruno", statusId = 99))
        vm.onSave()
        advanceUntilIdle()

        assertEquals("Situação inexistente.", vm.uiState.value.fieldErrors[MemberField.STATUS])
        assertNull(vm.uiState.value.generalError)
        assertEquals("Bruno", vm.uiState.value.draft.name)
    }

    @Test
    fun `a refusal that names no field is a general message`() = runTest {
        api.onCreateMember = { apiError(400, "VALIDATION_ERROR", "Cargo 7 não existe.") }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDraftChanged(vm.uiState.value.draft.copy(name = "Bruno"))
        vm.onSave()
        advanceUntilIdle()

        assertEquals("Cargo 7 não existe.", vm.uiState.value.generalError)
    }

    @Test
    fun `a birth date rule refused by the server shows its detail as-is`() = runTest {
        val detail = "Data de nascimento 29/02/1990 não existe: 1990 não é bissexto."
        api.onUpdateMember = { _, _ -> apiError(400, "VALIDATION_ERROR", detail) }
        val vm = viewModel(memberId = "12")
        advanceUntilIdle()

        vm.onDraftChanged(vm.uiState.value.draft.copy(birth = BirthDate(day = 3, month = 4, year = 1990)))
        vm.onSave()
        advanceUntilIdle()

        assertEquals(detail, vm.uiState.value.generalError)
    }

    @Test
    fun `clearing the year patches only the year, as null`() = runTest {
        api.onUpdateMember = { id, _ -> ok(recordDto(id = id, birth = BirthDate(day = 2, month = 4))) }
        val vm = viewModel(memberId = "12")
        advanceUntilIdle()

        val draft = vm.uiState.value.draft
        vm.onDraftChanged(draft.copy(birth = draft.birth.copy(year = null)))
        vm.onSave()
        advanceUntilIdle()

        val (_, body) = api.updatedBodies.single()
        assertEquals(setOf("birth_year"), body.keys)
        assertEquals(JsonNull, body["birth_year"])
    }

    @Test
    fun `unsaved changes are tracked against the original`() = runTest {
        val vm = viewModel(memberId = "12")
        advanceUntilIdle()
        val original = vm.uiState.value.draft

        vm.onDraftChanged(original.copy(lastName = "Lima"))
        assertTrue(vm.uiState.value.hasUnsavedChanges)

        vm.onDraftChanged(original)
        assertFalse(vm.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `a successful create emits the new id`() = runTest {
        api.onCreateMember = { ok(recordDto(id = 40, name = "Bruno")) }
        val vm = viewModel()
        advanceUntilIdle()
        vm.onDraftChanged(vm.uiState.value.draft.copy(name = "Bruno"))

        vm.events.test {
            vm.onSave()
            advanceUntilIdle()
            assertEquals(MembersEvent.Saved(40), awaitItem())
        }
    }

    @Test
    fun `options failure shows a load error to retry`() = runTest {
        api.onGetOptions = { throw IOException("offline") }
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.loadError!!.contains("conexão"))

        api.onGetOptions = { ok(optionsDto()) }
        vm.load()
        advanceUntilIdle()
        assertNull(vm.uiState.value.loadError)
    }

    // region photo

    @Test
    fun `a picked photo is previewed and counts as unsaved`() = runTest {
        val vm = viewModel(memberId = "12")
        advanceUntilIdle()

        vm.onPhotoPicked(jpeg)

        assertTrue(vm.uiState.value.pickedPhoto.contentEquals(jpeg))
        assertTrue(vm.uiState.value.hasUnsavedChanges)
        assertTrue(api.uploadedParts.isEmpty())
    }

    @Test
    fun `a file that is not an image is refused and not kept`() = runTest {
        val vm = viewModel(memberId = "12")
        advanceUntilIdle()

        vm.events.test {
            vm.onPhotoPicked(byteArrayOf(1, 2, 3, 4))
            advanceUntilIdle()
            assertEquals(MembersEvent.ShowMessage("Use uma imagem JPEG, PNG, WEBP ou GIF."), awaitItem())
        }
        assertNull(vm.uiState.value.pickedPhoto)
        assertFalse(vm.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `saving sends the fields and then uploads the photo`() = runTest {
        val calls = mutableListOf<String>()
        api.onUpdateMember = { id, _ -> calls += "fields"; ok(recordDto(id = id, isActive = false)) }
        api.onUploadPhoto = { _, _ -> calls += "photo"; ok(PhotoUrlDto("https://host/n.jpg")) }
        val vm = viewModel(memberId = "12")
        advanceUntilIdle()
        vm.onDraftChanged(vm.uiState.value.draft.copy(isValid = false))
        vm.onPhotoPicked(jpeg)

        vm.events.test {
            vm.onSave()
            advanceUntilIdle()
            assertEquals(MembersEvent.Saved(12), awaitItem())
        }
        assertEquals(listOf("fields", "photo"), calls)
        assertNull(vm.uiState.value.pickedPhoto)
    }

    @Test
    fun `a failed upload keeps the photo and a second save retries only the upload`() = runTest {
        api.onUpdateMember = { id, _ -> ok(recordDto(id = id, isActive = false)) }
        api.onUploadPhoto = { _, _ -> throw IOException("offline") }
        val vm = viewModel(memberId = "12")
        advanceUntilIdle()
        vm.onDraftChanged(vm.uiState.value.draft.copy(isValid = false))
        vm.onPhotoPicked(jpeg)

        vm.events.test {
            vm.onSave()
            advanceUntilIdle()
            assertTrue((awaitItem() as MembersEvent.ShowMessage).message.contains("conexão"))

            api.onUploadPhoto = { _, _ -> ok(PhotoUrlDto("https://host/n.jpg")) }
            vm.onSave()
            advanceUntilIdle()
            assertEquals(MembersEvent.Saved(12), awaitItem())
        }
        assertEquals(1, api.updatedBodies.size)
        assertEquals(2, api.uploadedParts.size)
    }

    @Test
    fun `a new member previews a picked photo`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onPhotoPicked(jpeg)

        assertTrue(vm.uiState.value.pickedPhoto.contentEquals(jpeg))
        assertTrue(vm.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `a new member's initials follow the typed name`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDraftChanged(vm.uiState.value.draft.copy(name = "Bruno Lima"))

        assertEquals("BL", vm.uiState.value.initials)
    }

    @Test
    fun `creating with a photo uploads it to the new id`() = runTest {
        val uploadedTo = mutableListOf<Int>()
        api.onCreateMember = { ok(recordDto(id = 40, name = "Bruno")) }
        api.onUploadPhoto = { id, _ -> uploadedTo += id; ok(PhotoUrlDto("https://host/n.jpg")) }
        val vm = viewModel()
        advanceUntilIdle()
        vm.onDraftChanged(vm.uiState.value.draft.copy(name = "Bruno"))
        vm.onPhotoPicked(jpeg)

        vm.events.test {
            vm.onSave()
            advanceUntilIdle()
            assertEquals(MembersEvent.Saved(40), awaitItem())
        }
        assertEquals(listOf(40), uploadedTo)
        assertNull(vm.uiState.value.pickedPhoto)
    }

    @Test
    fun `a failed upload after creating retries only the upload`() = runTest {
        api.onCreateMember = { ok(recordDto(id = 40, name = "Bruno")) }
        api.onUploadPhoto = { _, _ -> throw IOException("offline") }
        val vm = viewModel()
        advanceUntilIdle()
        vm.onDraftChanged(vm.uiState.value.draft.copy(name = "Bruno"))
        vm.onPhotoPicked(jpeg)

        vm.events.test {
            vm.onSave()
            advanceUntilIdle()
            assertTrue((awaitItem() as MembersEvent.ShowMessage).message.contains("conexão"))

            api.onUploadPhoto = { _, _ -> ok(PhotoUrlDto("https://host/n.jpg")) }
            vm.onSave()
            advanceUntilIdle()
            assertEquals(MembersEvent.Saved(40), awaitItem())
        }
        assertEquals(1, api.createdBodies.size)
        assertTrue(api.updatedBodies.isEmpty())
        assertEquals(2, api.uploadedParts.size)
    }

    // endregion
}
