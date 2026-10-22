package com.ipb.castelobranco.features.auth.domain.usecase

import com.ipb.castelobranco.core.domain.auth.AuthEventBus
import com.ipb.castelobranco.features.auth.domain.model.AuthTokens
import com.ipb.castelobranco.features.auth.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RegisterUseCaseTest {

    private lateinit var repository: AuthRepository
    private lateinit var authEventBus: AuthEventBus
    private lateinit var useCase: RegisterUseCase

    private val fakeTokens = AuthTokens(access = "access_token", refresh = "refresh_token")

    @Before
    fun setup() {
        repository = mockk()
        authEventBus = mockk(relaxed = true)
        useCase = RegisterUseCase(repository, authEventBus)
    }

    @Test
    fun `success returns Success`() = runTest {
        coEvery { repository.signUp(any(), any(), any(), any(), any()) } returns Result.success(fakeTokens)

        val result = useCase("user", "João", "Silva", "pass123", "pass123")

        assertEquals(RegisterUseCase.Result.Success, result)
    }

    @Test
    fun `success emits LoginSuccess on bus`() = runTest {
        coEvery { repository.signUp(any(), any(), any(), any(), any()) } returns Result.success(fakeTokens)

        useCase("user", "João", "Silva", "pass123", "pass123")

        verify { authEventBus.emit(AuthEventBus.Event.LoginSuccess) }
    }

    @Test
    fun `failure with message returns Failure with that message`() = runTest {
        coEvery { repository.signUp(any(), any(), any(), any(), any()) } returns
            Result.failure(Exception("Nome de usuário já existe"))

        val result = useCase("user", "João", "Silva", "pass123", "pass123")

        assertEquals(RegisterUseCase.Result.Failure("Nome de usuário já existe"), result)
    }

    @Test
    fun `failure without message returns Failure with fallback message`() = runTest {
        coEvery { repository.signUp(any(), any(), any(), any(), any()) } returns
            Result.failure(Exception())

        val result = useCase("user", "João", "Silva", "pass123", "pass123")

        assertEquals(RegisterUseCase.Result.Failure("Erro ao registrar"), result)
    }

    @Test
    fun `failure does not emit on bus`() = runTest {
        coEvery { repository.signUp(any(), any(), any(), any(), any()) } returns
            Result.failure(Exception("Erro"))

        useCase("user", "João", "Silva", "pass123", "pass123")

        verify(exactly = 0) { authEventBus.emit(any()) }
    }
}
