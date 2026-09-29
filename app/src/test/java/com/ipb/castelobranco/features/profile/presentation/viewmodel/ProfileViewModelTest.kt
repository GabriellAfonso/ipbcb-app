package com.ipb.castelobranco.features.profile.presentation.viewmodel

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.profile.domain.model.MeProfile
import com.ipb.castelobranco.features.profile.domain.usecase.FetchProfileUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fetchProfileUseCase: FetchProfileUseCase
    private lateinit var uploadProfilePhotoUseCase: UploadProfilePhotoUseCase
    private lateinit var viewModel: ProfileViewModel

    private val fakeProfile = MeProfile(
        name = "João Silva",
        isMember = true,
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

        viewModel = ProfileViewModel(fetchProfileUseCase, uploadProfilePhotoUseCase)
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
    fun `observeProfile SnapshotState Data updates isMember`() = runTest {
        every { fetchProfileUseCase.observe() } returns flowOf(
            SnapshotState.Data(
                fakeProfile.copy(isMember = false)
            )
        )

        viewModel.initialize()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isMember)
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
}
