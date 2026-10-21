package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.features.profile.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoveProfilePhotoUseCaseTest {

    private val repository = mockk<ProfileRepository> {
        coEvery { refreshMeProfile() } returns RefreshResult.Updated
    }
    private val useCase = RemoveProfilePhotoUseCase(repository)

    @Test
    fun `removes the photo and refreshes the profile`() = runTest {
        coEvery { repository.deleteProfilePhoto() } returns Result.success(Unit)

        assertTrue(useCase().isSuccess)
        coVerify(exactly = 1) { repository.refreshMeProfile() }
    }

    @Test
    fun `a failed removal keeps the profile as it was`() = runTest {
        coEvery { repository.deleteProfilePhoto() } returns Result.failure(AppError.Network())

        assertTrue(useCase().exceptionOrNull() is AppError.Network)
        coVerify(exactly = 0) { repository.refreshMeProfile() }
    }
}
