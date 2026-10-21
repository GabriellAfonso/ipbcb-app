package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangePasswordUseCaseTest {

    private val repository = FakeAccountRepository()
    private val useCase = ChangePasswordUseCase(repository)

    @Test
    fun `passes the three passwords as typed`() = runTest {
        val result = useCase("atual", "nova123", "nova123")

        assertTrue(result.isSuccess)
        assertEquals(listOf(Triple("atual", "nova123", "nova123")), repository.passwordChanges)
    }

    @Test
    fun `a wrong current password is returned as is`() = runTest {
        val error = AppError.Server(code = 400, errorCode = "INVALID_PASSWORD", userMessage = "Senha incorreta.")
        repository.changePasswordResult = Result.failure(error)

        val result = useCase("errada", "nova123", "nova123")

        assertEquals(error, result.exceptionOrNull())
    }
}
