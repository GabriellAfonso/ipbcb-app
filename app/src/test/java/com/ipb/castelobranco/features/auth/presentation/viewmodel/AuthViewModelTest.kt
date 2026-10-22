package com.ipb.castelobranco.features.auth.presentation.viewmodel

import app.cash.turbine.test
import com.ipb.castelobranco.features.auth.domain.model.RegisterErrors
import com.ipb.castelobranco.features.auth.domain.usecase.LoginUseCase
import com.ipb.castelobranco.features.auth.domain.usecase.RegisterUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var loginUseCase: LoginUseCase
    private lateinit var registerUseCase: RegisterUseCase
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        loginUseCase = mockk()
        registerUseCase = mockk()
        viewModel = AuthViewModel(loginUseCase, registerUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region singIn (credentials)

    @Test
    fun `singIn success emits LoginSuccess event`() = runTest {
        coEvery { loginUseCase.withCredentials(any(), any()) } returns LoginUseCase.Result.Success

        viewModel.events.test {
            viewModel.singIn("user", "pass")
            advanceUntilIdle()
            assertEquals(AuthViewModel.AuthEvent.LoginSuccess, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `singIn success leaves loginError null`() = runTest {
        coEvery { loginUseCase.withCredentials(any(), any()) } returns LoginUseCase.Result.Success

        viewModel.singIn("user", "pass")
        advanceUntilIdle()

        assertNull(viewModel.loginError.value)
    }

    @Test
    fun `singIn failure sets loginError via parseLoginError`() = runTest {
        coEvery { loginUseCase.withCredentials(any(), any()) } returns
            LoginUseCase.Result.Failure("{\"detail\": \"Credenciais inválidas\"}")

        viewModel.singIn("user", "pass")
        advanceUntilIdle()

        assertEquals("Credenciais inválidas", viewModel.loginError.value)
    }

    @Test
    fun `singIn failure does not emit event`() = runTest {
        coEvery { loginUseCase.withCredentials(any(), any()) } returns
            LoginUseCase.Result.Failure("error")

        viewModel.events.test {
            viewModel.singIn("user", "pass")
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region signInWithGoogle

    @Test
    fun `signInWithGoogle success emits LoginSuccess event`() = runTest {
        coEvery { loginUseCase.withGoogle(any()) } returns LoginUseCase.Result.Success

        viewModel.events.test {
            viewModel.signInWithGoogle("id_token")
            advanceUntilIdle()
            assertEquals(AuthViewModel.AuthEvent.LoginSuccess, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `signInWithGoogle failure sets loginError`() = runTest {
        coEvery { loginUseCase.withGoogle(any()) } returns
            LoginUseCase.Result.Failure("Token inválido")

        viewModel.signInWithGoogle("bad_token")
        advanceUntilIdle()

        assertEquals("Token inválido", viewModel.loginError.value)
    }

    @Test
    fun `signInWithGoogle resets isGoogleLoading to false after success`() = runTest {
        coEvery { loginUseCase.withGoogle(any()) } returns LoginUseCase.Result.Success

        viewModel.signInWithGoogle("id_token")
        advanceUntilIdle()

        assertFalse(viewModel.isGoogleLoading.value)
    }

    @Test
    fun `signInWithGoogle resets isGoogleLoading to false after failure`() = runTest {
        coEvery { loginUseCase.withGoogle(any()) } returns
            LoginUseCase.Result.Failure("error")

        viewModel.signInWithGoogle("bad_token")
        advanceUntilIdle()

        assertFalse(viewModel.isGoogleLoading.value)
    }

    // endregion

    // region singUp

    @Test
    fun `singUp success emits RegisterSuccess event`() = runTest {
        coEvery { registerUseCase(any(), any(), any(), any(), any()) } returns RegisterUseCase.Result.Success

        viewModel.events.test {
            viewModel.singUp("user", "First", "Last", "pass", "pass")
            advanceUntilIdle()
            assertEquals(AuthViewModel.AuthEvent.RegisterSuccess, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `singUp failure sets registerErrors via parseRegisterError`() = runTest {
        coEvery { registerUseCase(any(), any(), any(), any(), any()) } returns
            RegisterUseCase.Result.Failure("{\"username\": [\"Usuário já existe\"]}")

        viewModel.singUp("user", "First", "Last", "pass", "pass")
        advanceUntilIdle()

        assertEquals("Usuário já existe", viewModel.registerErrors.value.username)
    }

    @Test
    fun `singUp failure does not emit event`() = runTest {
        coEvery { registerUseCase(any(), any(), any(), any(), any()) } returns
            RegisterUseCase.Result.Failure("error")

        viewModel.events.test {
            viewModel.singUp("user", "First", "Last", "pass", "pass")
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `singUp success leaves registerErrors empty`() = runTest {
        coEvery { registerUseCase(any(), any(), any(), any(), any()) } returns RegisterUseCase.Result.Success

        viewModel.singUp("user", "First", "Last", "pass", "pass")
        advanceUntilIdle()

        assertEquals(RegisterErrors(), viewModel.registerErrors.value)
    }

    // endregion

    // region clearLoginError / clearRegisterErrors

    @Test
    fun `clearLoginError sets loginError to null`() = runTest {
        coEvery { loginUseCase.withCredentials(any(), any()) } returns
            LoginUseCase.Result.Failure("{\"detail\": \"Erro\"}")
        viewModel.singIn("user", "pass")
        advanceUntilIdle()

        viewModel.clearLoginError()

        assertNull(viewModel.loginError.value)
    }

    @Test
    fun `clearRegisterErrors resets registerErrors to empty`() = runTest {
        coEvery { registerUseCase(any(), any(), any(), any(), any()) } returns
            RegisterUseCase.Result.Failure("{\"username\": [\"taken\"]}")
        viewModel.singUp("user", "First", "Last", "pass", "pass")
        advanceUntilIdle()

        viewModel.clearRegisterErrors()

        assertEquals(RegisterErrors(), viewModel.registerErrors.value)
    }

    // endregion
}
