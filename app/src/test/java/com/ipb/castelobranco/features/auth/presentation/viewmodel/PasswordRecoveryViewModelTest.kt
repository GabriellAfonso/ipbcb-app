package com.ipb.castelobranco.features.auth.presentation.viewmodel

import app.cash.turbine.test
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.auth.domain.usecase.ConfirmPasswordRecoveryUseCase
import com.ipb.castelobranco.features.auth.domain.usecase.RequestPasswordRecoveryUseCase
import com.ipb.castelobranco.features.auth.domain.repository.AuthRepository
import com.ipb.castelobranco.features.auth.presentation.state.EMAIL_RULE_MESSAGE
import com.ipb.castelobranco.features.auth.presentation.state.RECOVERY_CODE_MESSAGE
import com.ipb.castelobranco.features.auth.presentation.state.RecoveryStep
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordRecoveryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<AuthRepository>()
    private lateinit var viewModel: PasswordRecoveryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.requestPasswordRecovery(any()) } returns Result.success(Unit)
        coEvery { repository.confirmPasswordRecovery(any(), any(), any(), any()) } returns Result.success(Unit)
        viewModel = PasswordRecoveryViewModel(
            RequestPasswordRecoveryUseCase(repository),
            ConfirmPasswordRecoveryUseCase(repository),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun invalidCode() = Result.failure<Unit>(
        AppError.Server(code = 400, errorCode = "INVALID_EMAIL_CODE", userMessage = "Código inválido ou expirado.")
    )

    private fun fillCodeStep() {
        viewModel.onCodeChange("123456")
        viewModel.onNewPasswordChange("nova123")
        viewModel.onNewPasswordConfirmChange("nova123")
    }

    @Test
    fun `a malformed address never reaches the backend`() = runTest(dispatcher) {
        viewModel.onEmailChange("ana@")
        viewModel.sendCode()
        runCurrent()

        assertEquals(EMAIL_RULE_MESSAGE, viewModel.state.value.emailHelp?.text)
        assertEquals(RecoveryStep.Email, viewModel.state.value.step)
        coVerify(exactly = 0) { repository.requestPasswordRecovery(any()) }
    }

    @Test
    fun `a good address goes to the code step with the trimmed address and a countdown`() = runTest(dispatcher) {
        viewModel.onEmailChange(" ana@example.com ")
        viewModel.sendCode()
        runCurrent()

        assertEquals(RecoveryStep.Code, viewModel.state.value.step)
        assertEquals(60, viewModel.state.value.resendSecondsLeft)
        coVerify { repository.requestPasswordRecovery("ana@example.com") }
        testScheduler.advanceTimeBy(60_001)
    }

    @Test
    fun `the server refusing the address shows under the field and stays on the step`() = runTest(dispatcher) {
        coEvery { repository.requestPasswordRecovery(any()) } returns Result.failure(
            AppError.Server(code = 400, fieldErrors = mapOf("email" to listOf("Informe um e-mail válido.")))
        )
        viewModel.onEmailChange("ana@example.com")
        viewModel.sendCode()
        runCurrent()

        assertEquals("Informe um e-mail válido.", viewModel.state.value.emailHelp?.text)
        assertEquals(RecoveryStep.Email, viewModel.state.value.step)
    }

    @Test
    fun `no connection keeps the address and allows a retry`() = runTest(dispatcher) {
        coEvery { repository.requestPasswordRecovery(any()) } returns Result.failure(IOException("offline"))
        viewModel.onEmailChange("ana@example.com")
        viewModel.sendCode()
        runCurrent()

        val state = viewModel.state.value
        assertEquals(RecoveryStep.Email, state.step)
        assertEquals("ana@example.com", state.email)
        assertTrue(state.banner!!.contains("internet"))
        assertFalse(state.isBusy)
    }

    @Test
    fun `resend waits for the countdown, then asks again`() = runTest(dispatcher) {
        viewModel.onEmailChange("ana@example.com")
        viewModel.sendCode()
        runCurrent()

        viewModel.resendCode()
        runCurrent()
        coVerify(exactly = 1) { repository.requestPasswordRecovery(any()) }

        testScheduler.advanceTimeBy(60_001)
        runCurrent()
        assertTrue(viewModel.state.value.canResend)

        viewModel.resendCode()
        runCurrent()
        coVerify(exactly = 2) { repository.requestPasswordRecovery(any()) }
        assertEquals(RecoveryStep.Code, viewModel.state.value.step)
        testScheduler.advanceTimeBy(60_001)
    }

    @Test
    fun `the code keeps only digits and at most six`() {
        viewModel.onCodeChange("12a34 5678")

        assertEquals("123456", viewModel.state.value.code)
    }

    @Test
    fun `an incomplete code stays local`() = runTest(dispatcher) {
        fillCodeStep()
        viewModel.onCodeChange("123")
        viewModel.resetPassword()
        runCurrent()

        assertEquals(RECOVERY_CODE_MESSAGE, viewModel.state.value.codeHelp?.text)
        coVerify(exactly = 0) { repository.confirmPasswordRecovery(any(), any(), any(), any()) }
    }

    @Test
    fun `a good code emits the reset event`() = runTest(dispatcher) {
        viewModel.onEmailChange("ana@example.com")
        fillCodeStep()

        viewModel.events.test {
            viewModel.resetPassword()
            runCurrent()

            assertEquals(PasswordRecoveryViewModel.Event.PasswordReset, awaitItem())
        }
        coVerify { repository.confirmPasswordRecovery("ana@example.com", "123456", "nova123", "nova123") }
    }

    @Test
    fun `a wrong code shows one message and keeps what was typed`() = runTest(dispatcher) {
        coEvery { repository.confirmPasswordRecovery(any(), any(), any(), any()) } returns invalidCode()
        viewModel.onEmailChange("ana@example.com")
        fillCodeStep()

        viewModel.resetPassword()
        runCurrent()

        val state = viewModel.state.value
        assertEquals("Código inválido ou expirado.", state.banner)
        assertEquals("123456", state.code)
        assertEquals("nova123", state.newPassword)
        assertFalse(state.isBusy)
    }

    @Test
    fun `a refused password shows under its field without a banner`() = runTest(dispatcher) {
        coEvery { repository.confirmPasswordRecovery(any(), any(), any(), any()) } returns Result.failure(
            AppError.Server(code = 400, fieldErrors = mapOf("new_password" to listOf("Senha muito comum.")))
        )
        viewModel.onEmailChange("ana@example.com")
        fillCodeStep()

        viewModel.resetPassword()
        runCurrent()

        assertEquals("Senha muito comum.", viewModel.state.value.newPasswordHelp.text)
        assertNull(viewModel.state.value.banner)
    }

    @Test
    fun `back from the code step keeps the address, and from the first step leaves`() = runTest(dispatcher) {
        viewModel.onEmailChange("ana@example.com")
        viewModel.sendCode()
        runCurrent()

        assertTrue(viewModel.goBackOneStep())
        assertEquals(RecoveryStep.Email, viewModel.state.value.step)
        assertEquals("ana@example.com", viewModel.state.value.email)
        assertFalse(viewModel.goBackOneStep())
        testScheduler.advanceTimeBy(60_001)
    }
}
