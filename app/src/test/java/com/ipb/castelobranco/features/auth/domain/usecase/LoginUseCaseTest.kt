package com.ipb.castelobranco.features.auth.domain.usecase

import com.ipb.castelobranco.core.domain.auth.AuthEventBus
import com.ipb.castelobranco.features.auth.domain.model.AuthTokens
import com.ipb.castelobranco.features.auth.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LoginUseCaseTest {

    private lateinit var repository: AuthRepository
    private lateinit var authEventBus: AuthEventBus
    private lateinit var useCase: LoginUseCase

    private val fakeTokens = AuthTokens(access = "access_token", refresh = "refresh_token")

    @Before
    fun setup() {
        repository = mockk()
        authEventBus = mockk(relaxed = true)
        useCase = LoginUseCase(repository, authEventBus)
    }

    // region withCredentials

    @Test
    fun `withCredentials success returns Success`() = runTest {
        coEvery { repository.signIn(any(), any()) } returns Result.success(fakeTokens)

        val result = useCase.withCredentials("user", "pass")

        assertEquals(LoginUseCase.Result.Success, result)
    }

    @Test
    fun `withCredentials success emits LoginSuccess on bus`() = runTest {
        coEvery { repository.signIn(any(), any()) } returns Result.success(fakeTokens)

        useCase.withCredentials("user", "pass")

        verify { authEventBus.emit(AuthEventBus.Event.LoginSuccess) }
    }

    @Test
    fun `withCredentials failure with message returns Failure with correct message`() = runTest {
        coEvery { repository.signIn(any(), any()) } returns
            Result.failure(Exception("Invalid credentials"))

        val result = useCase.withCredentials("user", "pass")

        assertEquals(LoginUseCase.Result.Failure("Invalid credentials"), result)
    }

    @Test
    fun `withCredentials failure without message returns Failure with default message`() = runTest {
        coEvery { repository.signIn(any(), any()) } returns
            Result.failure(Exception())

        val result = useCase.withCredentials("user", "pass")

        assertEquals(LoginUseCase.Result.Failure("Erro ao fazer login"), result)
    }

    @Test
    fun `withCredentials failure does not emit event on bus`() = runTest {
        coEvery { repository.signIn(any(), any()) } returns
            Result.failure(Exception("error"))

        useCase.withCredentials("user", "pass")

        verify(exactly = 0) { authEventBus.emit(any()) }
    }

    // endregion

    // region withGoogle

    @Test
    fun `withGoogle success returns Success`() = runTest {
        coEvery { repository.signInWithGoogle(any()) } returns Result.success(fakeTokens)

        val result = useCase.withGoogle("token123")

        assertEquals(LoginUseCase.Result.Success, result)
    }

    @Test
    fun `withGoogle success emits LoginSuccess on bus`() = runTest {
        coEvery { repository.signInWithGoogle(any()) } returns Result.success(fakeTokens)

        useCase.withGoogle("token123")

        verify { authEventBus.emit(AuthEventBus.Event.LoginSuccess) }
    }

    @Test
    fun `withGoogle failure with message returns Failure with correct message`() = runTest {
        coEvery { repository.signInWithGoogle(any()) } returns
            Result.failure(Exception("Invalid token"))

        val result = useCase.withGoogle("token123")

        assertEquals(LoginUseCase.Result.Failure("Invalid token"), result)
    }

    @Test
    fun `withGoogle failure without message returns Failure with default message`() = runTest {
        coEvery { repository.signInWithGoogle(any()) } returns
            Result.failure(Exception())

        val result = useCase.withGoogle("token123")

        assertEquals(LoginUseCase.Result.Failure("Erro ao entrar com Google"), result)
    }

    @Test
    fun `withGoogle failure does not emit event on bus`() = runTest {
        coEvery { repository.signInWithGoogle(any()) } returns
            Result.failure(Exception("error"))

        useCase.withGoogle("token123")

        verify(exactly = 0) { authEventBus.emit(any()) }
    }

    // endregion
}