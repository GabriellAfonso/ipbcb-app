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

class RecoveryEmailUseCasesTest {

    private val account = FakeAccountRepository()
    private val profile = mockk<ProfileRepository> {
        coEvery { refreshMeProfile() } returns RefreshResult.Updated
    }
    private val refused = Result.failure<Unit>(AppError.Server(code = 400))

    @Test
    fun `changing trims the address and refreshes the profile`() = runTest {
        val result = ChangeRecoveryEmailUseCase(account, profile)(" a@b.com ", " pass ")

        assertTrue(result.isSuccess)
        assertEquals("a@b.com" to " pass ", account.recoveryEmailChanges.single())
        coVerify(exactly = 1) { profile.refreshMeProfile() }
    }

    @Test
    fun `a refused change does not refresh`() = runTest {
        account.changeRecoveryEmailResult = refused

        assertTrue(ChangeRecoveryEmailUseCase(account, profile)("a@b.com", "pass").isFailure)
        coVerify(exactly = 0) { profile.refreshMeProfile() }
    }

    @Test
    fun `sending passes the result through`() = runTest {
        assertTrue(SendRecoveryEmailCodeUseCase(account)().isSuccess)

        account.sendCodeResult = refused
        assertTrue(SendRecoveryEmailCodeUseCase(account)().isFailure)
    }

    @Test
    fun `confirming refreshes the profile`() = runTest {
        assertTrue(ConfirmRecoveryEmailUseCase(account, profile)("123456").isSuccess)

        coVerify(exactly = 1) { profile.refreshMeProfile() }
    }

    @Test
    fun `a refused code does not refresh`() = runTest {
        account.confirmEmailResult = refused

        assertTrue(ConfirmRecoveryEmailUseCase(account, profile)("000000").isFailure)
        coVerify(exactly = 0) { profile.refreshMeProfile() }
    }
}
