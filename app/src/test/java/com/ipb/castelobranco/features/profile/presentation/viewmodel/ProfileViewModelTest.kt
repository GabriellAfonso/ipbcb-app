package com.ipb.castelobranco.features.profile.presentation.viewmodel

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.profile.domain.model.MeProfile
import com.ipb.castelobranco.core.domain.auth.AuthEventBus
import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.core.domain.access.AccessStatus
import com.ipb.castelobranco.core.domain.access.MemberAccess
import com.ipb.castelobranco.core.domain.access.RequestAccessUseCase
import com.ipb.castelobranco.core.testing.FakeMemberAccessRepository
import com.ipb.castelobranco.features.profile.domain.model.AccountDeletionProof
import com.ipb.castelobranco.features.profile.domain.model.OwnMember
import com.ipb.castelobranco.features.profile.domain.repository.OwnMemberRepository
import com.ipb.castelobranco.features.profile.domain.usecase.GetOwnMemberUseCase
import com.ipb.castelobranco.features.profile.presentation.state.OwnMemberSection
import com.ipb.castelobranco.features.profile.domain.repository.ProfileRepository
import com.ipb.castelobranco.features.profile.domain.usecase.ChangePasswordUseCase
import com.ipb.castelobranco.features.profile.domain.usecase.DeleteAccountUseCase
import com.ipb.castelobranco.features.profile.domain.usecase.FakeAccountRepository
import com.ipb.castelobranco.features.profile.domain.usecase.FetchProfileUseCase
import com.ipb.castelobranco.features.profile.domain.usecase.RecordingAuthEventBus
import com.ipb.castelobranco.features.profile.domain.usecase.RemoveProfilePhotoUseCase
import com.ipb.castelobranco.features.profile.domain.usecase.UpdateProfileNameUseCase
import com.ipb.castelobranco.features.profile.presentation.state.ProfileUiEvent
import com.ipb.castelobranco.features.profile.domain.usecase.UploadProfilePhotoUseCase
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fetchProfileUseCase: FetchProfileUseCase
    private lateinit var uploadProfilePhotoUseCase: UploadProfilePhotoUseCase
    private lateinit var viewModel: ProfileViewModel

    private val accountRepository = FakeAccountRepository()
    private val authEventBus = RecordingAuthEventBus()
    private val profileRepository = mockk<ProfileRepository> {
        coEvery { refreshMeProfile() } returns RefreshResult.Updated
        coEvery { deleteProfilePhoto() } returns Result.success(Unit)
    }

    private var ownMemberResult: Result<OwnMember?> = Result.success(null)
    private val ownMemberRepository = object : OwnMemberRepository {
        var calls = 0
        override suspend fun get(): Result<OwnMember?> {
            calls++
            return ownMemberResult
        }
    }
    private val memberAccessRepository = FakeMemberAccessRepository()

    private val fakeProfile = MeProfile(
        name = "João Silva",
        memberAccess = true,
        photoUrl = null
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fetchProfileUseCase = mockk()
        uploadProfilePhotoUseCase = mockk()

        every { fetchProfileUseCase.observe() } returns emptyFlow()
        every { fetchProfileUseCase.getLocalPhoto() } returns null
        coEvery { fetchProfileUseCase.refresh() } returns RefreshResult.NotModified

        viewModel = ProfileViewModel(
            fetchProfileUseCase,
            uploadProfilePhotoUseCase,
            RemoveProfilePhotoUseCase(profileRepository),
            UpdateProfileNameUseCase(accountRepository, profileRepository),
            ChangePasswordUseCase(accountRepository),
            DeleteAccountUseCase(accountRepository, authEventBus),
            GetOwnMemberUseCase(ownMemberRepository),
            RequestAccessUseCase(memberAccessRepository),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region refreshLocalPhotoPathAndBump (via initialize)

    @Test
    fun `initialize sets localPhotoPath from getLocalPhoto`() = runTest {
        val file = mockk<File> { every { absolutePath } returns "/data/files/profile/profile_photo.jpg" }
        every { fetchProfileUseCase.getLocalPhoto() } returns file

        viewModel.initialize()
        advanceUntilIdle()

        assertEquals("/data/files/profile/profile_photo.jpg", viewModel.uiState.value.localPhotoPath)
    }

    @Test
    fun `initialize sets localPhotoPath to null when no local photo exists`() = runTest {
        every { fetchProfileUseCase.getLocalPhoto() } returns null

        viewModel.initialize()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.localPhotoPath)
    }

    @Test
    fun `initialize bumps localPhotoVersion`() = runTest {
        val initialVersion = viewModel.uiState.value.localPhotoVersion

        viewModel.initialize()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.localPhotoVersion > initialVersion)
    }

    @Test
    fun `initialize calls getLocalPhoto not file system directly`() = runTest {
        viewModel.initialize()
        advanceUntilIdle()

        verify(atLeast = 1) { fetchProfileUseCase.getLocalPhoto() }
    }

    // endregion

    // region refreshFromServer

    @Test
    fun `refreshFromServer success clears error`() = runTest {
        coEvery { fetchProfileUseCase.refresh() } returns RefreshResult.Updated

        viewModel.refreshFromServer()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `refreshFromServer RefreshResult Error sets error message`() = runTest {
        coEvery { fetchProfileUseCase.refresh() } returns
            RefreshResult.Error(Exception("network failure"))

        viewModel.refreshFromServer()
        advanceUntilIdle()

        assertEquals("Falha ao atualizar perfil", viewModel.uiState.value.error)
    }

    @Test
    fun `refreshFromServer RefreshResult Error calls getLocalPhoto`() = runTest {
        coEvery { fetchProfileUseCase.refresh() } returns
            RefreshResult.Error(Exception("network failure"))

        viewModel.refreshFromServer()
        advanceUntilIdle()

        verify(atLeast = 1) { fetchProfileUseCase.getLocalPhoto() }
    }

    @Test
    fun `refreshFromServer exception sets generic error message`() = runTest {
        coEvery { fetchProfileUseCase.refresh() } throws Exception("timeout")

        viewModel.refreshFromServer()
        advanceUntilIdle()

        assertEquals("Algo deu errado. Tente novamente.", viewModel.uiState.value.error)
    }

    @Test
    fun `refreshFromServer AppError with userMessage sets that message`() = runTest {
        coEvery { fetchProfileUseCase.refresh() } throws
            AppError.Server(code = 500, message = "raw body", userMessage = "Falha ao atualizar perfil")

        viewModel.refreshFromServer()
        advanceUntilIdle()

        assertEquals("Falha ao atualizar perfil", viewModel.uiState.value.error)
    }

    // endregion

    // region uploadProfilePhoto

    @Test
    fun `uploadProfilePhoto success updates localPhotoPath via getLocalPhoto`() = runTest {
        val file = mockk<File> { every { absolutePath } returns "/data/files/profile/profile_photo.jpg" }
        every { fetchProfileUseCase.getLocalPhoto() } returns file
        coEvery { uploadProfilePhotoUseCase(any(), any()) } returns UploadProfilePhotoUseCase.Result.Success

        viewModel.uploadProfilePhoto(ByteArray(0))
        advanceUntilIdle()

        assertEquals("/data/files/profile/profile_photo.jpg", viewModel.uiState.value.localPhotoPath)
    }

    @Test
    fun `uploadProfilePhoto success bumps localPhotoVersion`() = runTest {
        coEvery { uploadProfilePhotoUseCase(any(), any()) } returns UploadProfilePhotoUseCase.Result.Success
        val versionBefore = viewModel.uiState.value.localPhotoVersion

        viewModel.uploadProfilePhoto(ByteArray(0))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.localPhotoVersion > versionBefore)
    }

    @Test
    fun `uploadProfilePhoto success clears isUploading`() = runTest {
        coEvery { uploadProfilePhotoUseCase(any(), any()) } returns UploadProfilePhotoUseCase.Result.Success

        viewModel.uploadProfilePhoto(ByteArray(0))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUploading)
    }

    @Test
    fun `uploadProfilePhoto failure sets error message`() = runTest {
        coEvery { uploadProfilePhotoUseCase(any(), any()) } returns
            UploadProfilePhotoUseCase.Result.Failure("Arquivo muito grande")

        viewModel.uploadProfilePhoto(ByteArray(0))
        advanceUntilIdle()

        assertEquals("Arquivo muito grande", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isUploading)
    }

    @Test
    fun `uploadProfilePhoto does not call use case when already uploading`() = runTest {
        coEvery { uploadProfilePhotoUseCase(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(1000)
            UploadProfilePhotoUseCase.Result.Success
        }

        viewModel.uploadProfilePhoto(ByteArray(0)) // first call — starts uploading
        viewModel.uploadProfilePhoto(ByteArray(0)) // second call — should be ignored
        advanceUntilIdle()

        coVerify(exactly = 1) { uploadProfilePhotoUseCase(any(), any()) }
    }

    // endregion

    // region observeProfile

    @Test
    fun `observeProfile SnapshotState Data updates userName trimmed`() = runTest {
        every { fetchProfileUseCase.observe() } returns
            flowOf(SnapshotState.Data(fakeProfile.copy(name = "  Maria  ")))

        viewModel.initialize()
        advanceUntilIdle()

        assertEquals("Maria", viewModel.uiState.value.userName)
    }

    @Test
    fun `observeProfile SnapshotState Data blank name falls back to Usuario`() = runTest {
        every { fetchProfileUseCase.observe() } returns
            flowOf(SnapshotState.Data(fakeProfile.copy(name = "   ")))

        viewModel.initialize()
        advanceUntilIdle()

        assertEquals("Usuário", viewModel.uiState.value.userName)
    }

    @Test
    fun `observeProfile SnapshotState Data shows the joined date`() = runTest {
        every { fetchProfileUseCase.observe() } returns flowOf(
            SnapshotState.Data(
                fakeProfile.copy(username = "joao", hasPassword = true, dateJoined = LocalDate.of(2025, 3, 14))
            )
        )

        viewModel.initialize()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("março de 2025", state.joinedSince)
        assertEquals(true, state.hasPassword)
        assertTrue(state.canDeleteAccount)
    }

    @Test
    fun `a profile cached before the account fields leaves them unknown`() = runTest {
        every { fetchProfileUseCase.observe() } returns flowOf(SnapshotState.Data(fakeProfile))

        viewModel.initialize()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.joinedSince)
        assertNull(state.hasPassword)
        assertFalse(state.canDeleteAccount)
    }

    @Test
    fun `observeProfile SnapshotState Data with photoUrl triggers downloadAndPersistPhoto`() = runTest {
        every { fetchProfileUseCase.observe() } returns flowOf(
            SnapshotState.Data(fakeProfile.copy(photoUrl = "https://example.com/photo.jpg"))
        )
        coEvery { fetchProfileUseCase.downloadAndPersistPhoto(any()) } returns Result.success(null)

        viewModel.initialize()
        advanceUntilIdle()

        coVerify { fetchProfileUseCase.downloadAndPersistPhoto("https://example.com/photo.jpg") }
    }

    @Test
    fun `observeProfile SnapshotState Data with null photoUrl does not trigger download`() = runTest {
        every { fetchProfileUseCase.observe() } returns flowOf(
            SnapshotState.Data(fakeProfile.copy(photoUrl = null))
        )

        viewModel.initialize()
        advanceUntilIdle()

        coVerify(exactly = 0) { fetchProfileUseCase.downloadAndPersistPhoto(any()) }
    }

    @Test
    fun `observeProfile SnapshotState Data with blank photoUrl does not trigger download`() = runTest {
        every { fetchProfileUseCase.observe() } returns flowOf(
            SnapshotState.Data(fakeProfile.copy(photoUrl = "  "))
        )

        viewModel.initialize()
        advanceUntilIdle()

        coVerify(exactly = 0) { fetchProfileUseCase.downloadAndPersistPhoto(any()) }
    }

    @Test
    fun `observeProfile SnapshotState Error sets generic error message`() = runTest {
        every { fetchProfileUseCase.observe() } returns flowOf(
            SnapshotState.Error(AppError.Server(code = 500, message = "snapshot error"))
        )

        // refreshFromServer always clears error before refreshing, so the Error state from
        // observeProfile is transient. Use Turbine to capture the intermediate emission.
        viewModel.uiState.test {
            awaitItem() // initial ProfileUiState
            viewModel.initialize()
            awaitItem() // after synchronous refreshLocalPhotoPathAndBump (localPhotoVersion bumped)
            val errorState = awaitItem() // after observeProfile processes SnapshotState.Error
            assertEquals("Não foi possível completar a operação. Tente novamente mais tarde.", errorState.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeProfile SnapshotState Error uses the authored userMessage when present`() = runTest {
        every { fetchProfileUseCase.observe() } returns flowOf(
            SnapshotState.Error(
                AppError.Server(code = 500, message = "raw body", userMessage = "Erro ao carregar perfil")
            )
        )

        viewModel.uiState.test {
            awaitItem() // initial ProfileUiState
            viewModel.initialize()
            awaitItem() // after synchronous refreshLocalPhotoPathAndBump
            val errorState = awaitItem() // after observeProfile processes SnapshotState.Error
            assertEquals("Erro ao carregar perfil", errorState.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region clearError

    @Test
    fun `clearError clears error in UI state`() = runTest {
        coEvery { fetchProfileUseCase.refresh() } throws Exception("some error")
        viewModel.refreshFromServer()
        advanceUntilIdle()

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    // endregion

    // region account

    private fun passwordAccount(hasPassword: Boolean? = true) {
        every { fetchProfileUseCase.observe() } returns flowOf(
            SnapshotState.Data(fakeProfile.copy(hasPassword = hasPassword))
        )
    }

    private fun validationError(field: String, message: String) = AppError.Server(
        code = 400,
        errorCode = "VALIDATION_ERROR",
        fieldErrors = mapOf(field to listOf(message)),
        userMessage = "Dados inválidos.",
    )

    private val invalidPassword =
        AppError.Server(code = 400, errorCode = "INVALID_PASSWORD", userMessage = "Senha incorreta.")

    @Test
    fun `saveName closes the dialog and confirms`() = runTest {
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.openNameDialog()

        viewModel.events.test {
            viewModel.saveName("  Ana  ")
            advanceUntilIdle()
            assertEquals(ProfileUiEvent.ShowMessage("Nome atualizado"), awaitItem())
        }
        assertEquals(listOf("Ana"), accountRepository.names)
        assertNull(viewModel.uiState.value.nameDialog)
    }

    @Test
    fun `saveName refused shows the server message on the field`() = runTest {
        accountRepository.updateNameResult =
            Result.failure(validationError("name", "O nome pode ter no máximo 100 caracteres."))
        viewModel.openNameDialog()

        viewModel.saveName("Ana")
        advanceUntilIdle()

        val dialog = viewModel.uiState.value.nameDialog!!
        assertFalse(dialog.isSaving)
        assertEquals("O nome pode ter no máximo 100 caracteres.", dialog.nameError)
        assertNull(dialog.error)
    }

    @Test
    fun `password dialog is not offered to an account without password`() = runTest {
        passwordAccount(hasPassword = false)
        viewModel.initialize()
        advanceUntilIdle()

        viewModel.openPasswordDialog()

        assertNull(viewModel.uiState.value.passwordDialog)
    }

    @Test
    fun `changePassword success closes the dialog and confirms`() = runTest {
        passwordAccount()
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.openPasswordDialog()

        viewModel.events.test {
            viewModel.changePassword("atual", "nova123", "nova123")
            advanceUntilIdle()
            assertEquals(ProfileUiEvent.ShowMessage("Senha alterada"), awaitItem())
        }
        assertEquals(listOf(Triple("atual", "nova123", "nova123")), accountRepository.passwordChanges)
        assertNull(viewModel.uiState.value.passwordDialog)
    }

    @Test
    fun `changePassword with wrong current password marks that field only`() = runTest {
        passwordAccount()
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.openPasswordDialog()
        accountRepository.changePasswordResult = Result.failure(invalidPassword)

        viewModel.changePassword("errada", "nova123", "nova123")
        advanceUntilIdle()

        val dialog = viewModel.uiState.value.passwordDialog!!
        assertEquals("Senha incorreta.", dialog.currentError)
        assertNull(dialog.error)
        assertFalse(dialog.isSaving)
    }

    @Test
    fun `changePassword mismatch shows the message on the confirmation`() = runTest {
        passwordAccount()
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.openPasswordDialog()
        accountRepository.changePasswordResult =
            Result.failure(validationError("new_password_confirm", "As senhas não coincidem."))

        viewModel.changePassword("atual", "nova123", "outra")
        advanceUntilIdle()

        assertEquals("As senhas não coincidem.", viewModel.uiState.value.passwordDialog!!.confirmError)
    }

    @Test
    fun `changePassword lockout shows a dialog message`() = runTest {
        passwordAccount()
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.openPasswordDialog()
        accountRepository.changePasswordResult = Result.failure(
            AppError.Server(code = 429, errorCode = "ACCOUNT_LOCKED", userMessage = "Muitas tentativas de login.")
        )

        viewModel.changePassword("atual", "nova123", "nova123")
        advanceUntilIdle()

        assertEquals("Muitas tentativas de login.", viewModel.uiState.value.passwordDialog!!.error)
    }

    @Test
    fun `delete dialog waits until it is known whether the account has a password`() = runTest {
        viewModel.openDeleteDialog()

        assertNull(viewModel.uiState.value.deleteDialog)
    }

    @Test
    fun `delete with password announces the deletion and leaves`() = runTest {
        passwordAccount()
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.openDeleteDialog()
        assertEquals(true, viewModel.uiState.value.deleteDialog!!.requiresPassword)

        viewModel.events.test {
            viewModel.confirmDelete("segredo")
            advanceUntilIdle()
            assertEquals(ProfileUiEvent.AccountDeleted, awaitItem())
        }
        assertEquals(
            listOf<AccountDeletionProof>(AccountDeletionProof.Password("segredo")),
            accountRepository.deletions,
        )
        assertEquals(listOf<AuthEventBus.Event>(AuthEventBus.Event.AccountDeleted), authEventBus.emitted)
        assertNull(viewModel.uiState.value.deleteDialog)
    }

    @Test
    fun `delete with a wrong password keeps the session and marks the field`() = runTest {
        passwordAccount()
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.openDeleteDialog()
        accountRepository.deleteResult = Result.failure(invalidPassword)

        viewModel.events.test {
            viewModel.confirmDelete("errada")
            advanceUntilIdle()
            expectNoEvents()
        }
        val dialog = viewModel.uiState.value.deleteDialog!!
        assertEquals("Senha incorreta.", dialog.passwordError)
        assertFalse(dialog.isDeleting)
        assertTrue(authEventBus.emitted.isEmpty())
    }

    @Test
    fun `delete blocked tells the person to talk to the leadership`() = runTest {
        passwordAccount()
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.openDeleteDialog()
        val message = "Esta conta não pode ser excluída pelo app. Fale com a liderança da igreja."
        accountRepository.deleteResult = Result.failure(
            AppError.Server(code = 409, errorCode = "ACCOUNT_DELETION_BLOCKED", userMessage = message)
        )

        viewModel.confirmDelete("segredo")
        advanceUntilIdle()

        assertEquals(message, viewModel.uiState.value.deleteDialog!!.error)
        assertTrue(authEventBus.emitted.isEmpty())
    }

    @Test
    fun `delete without password asks for a Google token and deletes with it`() = runTest {
        passwordAccount(hasPassword = false)
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.openDeleteDialog()

        viewModel.events.test {
            viewModel.confirmDelete("")
            advanceUntilIdle()
            assertEquals(ProfileUiEvent.RequestGoogleIdToken, awaitItem())
            assertTrue(viewModel.uiState.value.deleteDialog!!.isDeleting)

            viewModel.onGoogleIdToken("google-token")
            advanceUntilIdle()
            assertEquals(ProfileUiEvent.AccountDeleted, awaitItem())
        }
        assertEquals(
            listOf<AccountDeletionProof>(AccountDeletionProof.Google("google-token")),
            accountRepository.deletions,
        )
    }

    @Test
    fun `cancelling the Google picker leaves the dialog without a message`() = runTest {
        passwordAccount(hasPassword = false)
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.openDeleteDialog()
        viewModel.confirmDelete("")
        advanceUntilIdle()

        viewModel.onGoogleIdToken(null)

        val dialog = viewModel.uiState.value.deleteDialog!!
        assertFalse(dialog.isDeleting)
        assertNull(dialog.error)
        assertNull(dialog.passwordError)
        assertTrue(accountRepository.deletions.isEmpty())
    }

    @Test
    fun `another Google account shows the mismatch message`() = runTest {
        passwordAccount(hasPassword = false)
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.openDeleteDialog()
        accountRepository.deleteResult = Result.failure(
            AppError.Auth(code = 403, userMessage = "A conta Google não corresponde a esta conta.")
        )
        viewModel.confirmDelete("")
        advanceUntilIdle()

        viewModel.onGoogleIdToken("other-account")
        advanceUntilIdle()

        assertEquals("A conta Google não corresponde a esta conta.", viewModel.uiState.value.deleteDialog!!.error)
    }

    @Test
    fun `removing the photo refreshes the local photo and confirms`() = runTest {
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.requestRemovePhoto()
        assertTrue(viewModel.uiState.value.showRemovePhotoConfirm)

        viewModel.events.test {
            viewModel.confirmRemovePhoto()
            advanceUntilIdle()
            assertEquals(ProfileUiEvent.ShowMessage("Foto removida"), awaitItem())
        }
        val state = viewModel.uiState.value
        assertFalse(state.showRemovePhotoConfirm)
        assertFalse(state.isRemovingPhoto)
        assertNull(state.localPhotoPath)
        coVerify { profileRepository.refreshMeProfile() }
    }

    @Test
    fun `a failed photo removal shows the error`() = runTest {
        coEvery { profileRepository.deleteProfilePhoto() } returns Result.failure(AppError.Network())
        viewModel.initialize()
        advanceUntilIdle()

        viewModel.confirmRemovePhoto()
        advanceUntilIdle()

        assertEquals(
            "Sem conexão com a internet. Verifique sua rede e tente novamente.",
            viewModel.uiState.value.error,
        )
        assertFalse(viewModel.uiState.value.isRemovingPhoto)
    }

    // endregion

    // region access requests (spec 016)

    @Test
    fun `the access card follows the profile`() = runTest {
        val access = MemberAccess(false, AccessStatus.PENDING, requestsBlocked = false, correctionPending = false)
        every { fetchProfileUseCase.observe() } returns flowOf(
            SnapshotState.Data(fakeProfile.copy(memberAccessInfo = access))
        )

        viewModel.initialize()
        advanceUntilIdle()

        assertEquals(AccessStatus.PENDING, viewModel.uiState.value.access.status)
    }

    @Test
    fun `a linked account loads the roll data into memory`() = runTest {
        ownMemberResult = Result.success(
            OwnMember(
                role = "Diaconisa", ministries = listOf("Louvor", "Infantil"), birthDay = 14, birthMonth = 3,
                baptismDate = LocalDate.of(2001, 5, 20), gender = "F",
            )
        )
        every { fetchProfileUseCase.observe() } returns flowOf(SnapshotState.Data(fakeProfile.copy(memberId = 12)))

        viewModel.initialize()
        advanceUntilIdle()

        val section = viewModel.uiState.value.ownMember as OwnMemberSection.Loaded
        assertEquals(
            listOf(
                "Cargo" to "Diaconisa",
                "Ministérios" to "Louvor, Infantil",
                "Aniversário" to "14 de março",
                "Batismo" to "20/05/2001",
                "Gênero" to "Feminino",
            ),
            section.lines,
        )
    }

    @Test
    fun `an account without a link never asks for roll data`() = runTest {
        every { fetchProfileUseCase.observe() } returns flowOf(SnapshotState.Data(fakeProfile))

        viewModel.initialize()
        advanceUntilIdle()

        assertEquals(OwnMemberSection.Hidden, viewModel.uiState.value.ownMember)
        assertEquals(0, ownMemberRepository.calls)
    }

    @Test
    fun `a failed roll read shows the error and can be retried`() = runTest {
        ownMemberResult = Result.failure(AppError.Network())
        every { fetchProfileUseCase.observe() } returns flowOf(SnapshotState.Data(fakeProfile.copy(memberId = 12)))
        viewModel.initialize()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.ownMember is OwnMemberSection.Error)

        ownMemberResult = Result.success(null)
        viewModel.retryOwnMember()
        advanceUntilIdle()

        assertEquals(OwnMemberSection.Hidden, viewModel.uiState.value.ownMember)
    }

    @Test
    fun `a correction request is sent and confirmed`() = runTest {
        viewModel.events.test {
            viewModel.openCorrectionRequest()
            viewModel.sendRequest("Meu batismo foi em 2002.")
            advanceUntilIdle()

            assertEquals(ProfileUiEvent.ShowMessage("Correção solicitada"), awaitItem())
        }
        assertEquals(
            listOf(AccessRequestType.DATA_CORRECTION to "Meu batismo foi em 2002."),
            memberAccessRepository.requests,
        )
        assertNull(viewModel.uiState.value.requestDialog)
    }

    @Test
    fun `a refused request keeps the dialog with the server message`() = runTest {
        memberAccessRepository.requestResult =
            Result.failure(AppError.Server(code = 429, userMessage = "Tente novamente amanhã."))

        viewModel.openAccessRequest()
        viewModel.sendRequest("")
        advanceUntilIdle()

        assertEquals("Tente novamente amanhã.", viewModel.uiState.value.requestDialog?.error)
    }

    // endregion
}
