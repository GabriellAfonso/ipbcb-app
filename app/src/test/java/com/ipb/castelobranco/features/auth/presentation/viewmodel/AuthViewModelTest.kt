package com.ipb.castelobranco.features.auth.presentation.viewmodel

import com.ipb.castelobranco.core.domain.auth.AuthEventBusImpl
import com.ipb.castelobranco.features.auth.domain.model.AuthTokens
import com.ipb.castelobranco.features.auth.domain.repository.AuthRepository
import com.ipb.castelobranco.features.auth.domain.usecase.LoginUseCase
import com.ipb.castelobranco.features.auth.domain.usecase.RegisterUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import com.ipb.castelobranco.features.auth.presentation.state.FIX_FIELDS_MESSAGE
import com.ipb.castelobranco.features.auth.presentation.state.USERNAME_HINT

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAuthRepository : AuthRepository {
        var signInCalls = 0
        var gate: CompletableDeferred<Result<AuthTokens>>? = null
        var signInResult: Result<AuthTokens> = Result.success(AuthTokens(access = "a", refresh = "r"))

        override suspend fun signIn(username: String, password: String): Result<AuthTokens> {
            signInCalls++
            return gate?.await() ?: signInResult
        }

        var signUpCalls = 0
        var signUpUsername: String? = null
        var signUpGate: CompletableDeferred<Result<AuthTokens>>? = null
        var signUpResult: Result<AuthTokens> = Result.success(AuthTokens(access = "a", refresh = "r"))

        override suspend fun signInWithGoogle(idToken: String): Result<AuthTokens> = signInResult
        override suspend fun signUp(
            username: String,
            firstName: String,
            lastName: String,
            password: String,
            passwordConfirm: String,
        ): Result<AuthTokens> {
            signUpCalls++
            signUpUsername = username
            return signUpGate?.await() ?: signUpResult
        }

        override suspend fun getAuthToken(): String? = null
        override suspend fun signOut() = Unit
    }

    private fun buildViewModel(repository: AuthRepository): AuthViewModel {
        val bus = AuthEventBusImpl()
        return AuthViewModel(LoginUseCase(repository, bus), RegisterUseCase(repository, bus))
    }

    @Test
    fun `blank credentials show an error without calling the backend`() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = buildViewModel(repository)

        viewModel.singIn("joao.silva", " ")

        assertEquals(AuthViewModel.EMPTY_CREDENTIALS_MESSAGE, viewModel.loginError.value)
        assertEquals(0, repository.signInCalls)
        assertFalse(viewModel.isLoginLoading.value)
    }

    @Test
    fun `sign in is loading while the backend answers and ignores a second tap`() = runTest(testDispatcher) {
        val repository = FakeAuthRepository().apply { gate = CompletableDeferred() }
        val viewModel = buildViewModel(repository)

        viewModel.singIn("joao.silva", "senha")
        assertTrue(viewModel.isLoginLoading.value)

        viewModel.singIn("joao.silva", "senha")
        assertEquals(1, repository.signInCalls)

        repository.gate!!.complete(Result.success(AuthTokens(access = "a", refresh = "r")))
        assertFalse(viewModel.isLoginLoading.value)
        assertNull(viewModel.loginError.value)
    }

    @Test
    fun `backend failure shows its message and stops loading`() = runTest(testDispatcher) {
        val repository = FakeAuthRepository().apply {
            signInResult = Result.failure(Exception("""{"detail": "Usuário ou senha inválidos."}"""))
        }
        val viewModel = buildViewModel(repository)

        viewModel.singIn("joao.silva", "errada")

        assertEquals("Usuário ou senha inválidos.", viewModel.loginError.value)
        assertFalse(viewModel.isLoginLoading.value)
    }

    private fun AuthViewModel.fillValidForm() {
        onFirstNameChange("Maria")
        onLastNameChange("Oliveira")
        onUsernameChange("Maria.Oliveira")
        onPasswordChange("abc123")
        onPasswordConfirmChange("abc123")
    }

    @Test
    fun `invalid register form is refused without calling the backend`() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = buildViewModel(repository)

        viewModel.onPasswordChange("abc")
        viewModel.singUp()

        val form = viewModel.registerForm.value
        assertEquals(0, repository.signUpCalls)
        assertTrue(form.submitted)
        assertEquals(FIX_FIELDS_MESSAGE, form.bannerMessage)
    }

    @Test
    fun `register is loading while the backend answers, ignores a second tap and sends the lowercased username`() =
        runTest(testDispatcher) {
            val repository = FakeAuthRepository().apply { signUpGate = CompletableDeferred() }
            val viewModel = buildViewModel(repository)
            viewModel.fillValidForm()

            viewModel.singUp()
            assertTrue(viewModel.registerForm.value.isLoading)
            viewModel.singUp()
            assertEquals(1, repository.signUpCalls)
            assertEquals("maria.oliveira", repository.signUpUsername)

            repository.signUpGate!!.complete(Result.success(AuthTokens(access = "a", refresh = "r")))
            assertFalse(viewModel.registerForm.value.isLoading)
        }

    @Test
    fun `backend field error stays until that field is edited`() = runTest(testDispatcher) {
        val repository = FakeAuthRepository().apply {
            signUpResult = Result.failure(
                Exception("""{"error_code": "VALIDATION_ERROR", "detail": "Erro", "field_errors": {"username": ["Este nome de usuário já está em uso."]}}"""),
            )
        }
        val viewModel = buildViewModel(repository)
        viewModel.fillValidForm()

        viewModel.singUp()
        assertEquals("Este nome de usuário já está em uso.", viewModel.registerForm.value.usernameHelp.text)

        viewModel.onPasswordChange("abc1234")
        assertEquals("Este nome de usuário já está em uso.", viewModel.registerForm.value.usernameHelp.text)

        viewModel.onUsernameChange("maria.o")
        assertEquals(USERNAME_HINT, viewModel.registerForm.value.usernameHelp.text)
    }
}
