package com.ipb.castelobranco.features.auth.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.auth.domain.model.EmailRules
import com.ipb.castelobranco.features.auth.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordRecoveryUseCasesTest {

    private val repository = mockk<AuthRepository>()
    private val refused = Result.failure<Unit>(AppError.Server(code = 400))

    @Test
    fun `request passes success and failure through`() = runTest {
        val useCase = RequestPasswordRecoveryUseCase(repository)

        coEvery { repository.requestPasswordRecovery("a@b.com") } returns Result.success(Unit)
        assertTrue(useCase("a@b.com").isSuccess)

        coEvery { repository.requestPasswordRecovery("a@b.com") } returns refused
        assertTrue(useCase("a@b.com").isFailure)
    }

    @Test
    fun `confirm passes success and failure through`() = runTest {
        val useCase = ConfirmPasswordRecoveryUseCase(repository)

        coEvery { repository.confirmPasswordRecovery("a@b.com", "123456", "nova", "nova") } returns Result.success(Unit)
        assertTrue(useCase("a@b.com", "123456", "nova", "nova").isSuccess)

        coEvery { repository.confirmPasswordRecovery("a@b.com", "123456", "nova", "nova") } returns refused
        assertTrue(useCase("a@b.com", "123456", "nova", "nova").isFailure)
    }

    @Test
    fun `e-mail rule catches obvious mistakes only`() {
        listOf("a@b.com", "ana.paula+x@mail.example.com.br").forEach { assertTrue(it, EmailRules.isValid(it)) }
        listOf("", "a", "a@", "@b.com", "a@b", "a b@c.com", "a@b@c.com", "a@b.", "a@.com").forEach {
            assertFalse(it, EmailRules.isValid(it))
        }
    }
}
