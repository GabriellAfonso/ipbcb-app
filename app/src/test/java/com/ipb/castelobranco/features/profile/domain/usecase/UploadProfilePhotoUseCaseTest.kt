package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.profile.domain.model.MeProfile
import com.ipb.castelobranco.features.profile.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class UploadProfilePhotoUseCaseTest {

    private lateinit var repository: ProfileRepository
    private lateinit var useCase: UploadProfilePhotoUseCase

    private val bytes = byteArrayOf(1, 2, 3)
    private val fileName = "photo.jpg"

    private val profileWithPhoto = MeProfile(
        name = "João", isMember = true,
        photoUrl = "https://example.com/photo.jpg"
    )
    private val profileNoPhoto = MeProfile(
        name = "João", isMember = true,
        photoUrl = null
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = UploadProfilePhotoUseCase(repository)
    }

    // region success

    @Test
    fun `invoke success with photo URL uploads and downloads photo`() = runTest {
        coEvery { repository.uploadProfilePhoto(bytes, fileName) } returns Result.success(null)
        coEvery { repository.refreshMeProfile() } returns RefreshResult.Updated
        every { repository.observeMeProfile() } returns
            MutableStateFlow(SnapshotState.Data(profileWithPhoto))
        coEvery { repository.downloadAndPersistProfilePhoto(any()) } returns Result.success(File("photo.jpg"))

        val result = useCase(bytes, fileName)

        assertTrue(result is UploadProfilePhotoUseCase.Result.Success)
        coVerify { repository.downloadAndPersistProfilePhoto("https://example.com/photo.jpg") }
    }

    @Test
    fun `invoke success without photo URL skips download`() = runTest {
        coEvery { repository.uploadProfilePhoto(bytes, fileName) } returns Result.success(null)
        coEvery { repository.refreshMeProfile() } returns RefreshResult.Updated
        every { repository.observeMeProfile() } returns
            MutableStateFlow(SnapshotState.Data(profileNoPhoto))

        val result = useCase(bytes, fileName)

        assertTrue(result is UploadProfilePhotoUseCase.Result.Success)
        coVerify(exactly = 0) { repository.downloadAndPersistProfilePhoto(any()) }
    }

    // endregion

    // region upload failure

    @Test
    fun `invoke returns Failure when upload throws`() = runTest {
        coEvery { repository.uploadProfilePhoto(bytes, fileName) } returns
            Result.failure(AppError.Server(code = 400, message = "raw body", userMessage = "upload failed"))

        val result = useCase(bytes, fileName)

        assertTrue(result is UploadProfilePhotoUseCase.Result.Failure)
        assertEquals("upload failed", (result as UploadProfilePhotoUseCase.Result.Failure).message)
    }

    // endregion

    // region refresh failure

    @Test
    fun `invoke returns Failure when refresh returns Error`() = runTest {
        coEvery { repository.uploadProfilePhoto(bytes, fileName) } returns Result.success(null)
        coEvery { repository.refreshMeProfile() } returns RefreshResult.Error(RuntimeException("refresh fail"))

        val result = useCase(bytes, fileName)

        assertTrue(result is UploadProfilePhotoUseCase.Result.Failure)
        assertEquals("Falha ao atualizar perfil", (result as UploadProfilePhotoUseCase.Result.Failure).message)
    }

    // endregion

    // region download failure

    @Test
    fun `invoke returns Failure when downloadAndPersist throws`() = runTest {
        coEvery { repository.uploadProfilePhoto(bytes, fileName) } returns Result.success(null)
        coEvery { repository.refreshMeProfile() } returns RefreshResult.Updated
        every { repository.observeMeProfile() } returns
            MutableStateFlow(SnapshotState.Data(profileWithPhoto))
        coEvery { repository.downloadAndPersistProfilePhoto(any()) } returns
            Result.failure(AppError.Server(code = 400, message = "raw body", userMessage = "download fail"))

        val result = useCase(bytes, fileName)

        assertTrue(result is UploadProfilePhotoUseCase.Result.Failure)
        assertEquals("download fail", (result as UploadProfilePhotoUseCase.Result.Failure).message)
    }

    // endregion

    // region blank photoUrl

    @Test
    fun `invoke with blank photoUrl skips download`() = runTest {
        val profileBlank = profileWithPhoto.copy(photoUrl = "   ")
        coEvery { repository.uploadProfilePhoto(bytes, fileName) } returns Result.success(null)
        coEvery { repository.refreshMeProfile() } returns RefreshResult.Updated
        every { repository.observeMeProfile() } returns
            MutableStateFlow(SnapshotState.Data(profileBlank))

        val result = useCase(bytes, fileName)

        assertTrue(result is UploadProfilePhotoUseCase.Result.Success)
        coVerify(exactly = 0) { repository.downloadAndPersistProfilePhoto(any()) }
    }

    // endregion
}
