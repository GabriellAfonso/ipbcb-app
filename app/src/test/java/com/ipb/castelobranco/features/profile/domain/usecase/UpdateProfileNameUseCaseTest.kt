package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.features.profile.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateProfileNameUseCaseTest {

    private val accounts = FakeAccountRepository()
    private val profile = mockk<ProfileRepository> {
        coEvery { refreshMeProfile() } returns RefreshResult.Updated
    }
    private val useCase = UpdateProfileNameUseCase(accounts, profile)

    @Test
    fun `saves the trimmed name and refreshes the profile`() = runTest {
        val result = useCase("  Ana Maria  ")

        assertTrue(result.isSuccess)
        assertEquals(listOf("Ana Maria"), accounts.names)
        coVerify(exactly = 1) { profile.refreshMeProfile() }
    }

    @Test
    fun `a blank name never reaches the server`() = runTest {
        val result = useCase("   ")

        assertTrue(result.isFailure)
        assertTrue(accounts.names.isEmpty())
        coVerify(exactly = 0) { profile.refreshMeProfile() }
    }

    @Test
    fun `a refused name does not refresh`() = runTest {
        accounts.updateNameResult = Result.failure(AppError.Server(code = 400, errorCode = "VALIDATION_ERROR"))

        val result = useCase("Ana")

        assertTrue(result.exceptionOrNull() is AppError.Server)
        coVerify(exactly = 0) { profile.refreshMeProfile() }
    }
}
