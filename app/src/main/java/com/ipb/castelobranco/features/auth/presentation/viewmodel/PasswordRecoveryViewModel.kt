package com.ipb.castelobranco.features.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.auth.domain.usecase.ConfirmPasswordRecoveryUseCase
import com.ipb.castelobranco.features.auth.domain.usecase.RequestPasswordRecoveryUseCase
import com.ipb.castelobranco.features.auth.presentation.state.PasswordRecoveryState
import com.ipb.castelobranco.features.auth.presentation.state.RECOVERY_CODE_LENGTH
import com.ipb.castelobranco.features.auth.presentation.state.RecoveryStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** "Esqueci minha senha" (`specs/auth/spec.md` §5). Nothing here is logged: it holds an address and a new password. */
@HiltViewModel
class PasswordRecoveryViewModel @Inject constructor(
    private val requestRecovery: RequestPasswordRecoveryUseCase,
    private val confirmRecovery: ConfirmPasswordRecoveryUseCase,
) : ViewModel() {

    sealed interface Event {
        data object PasswordReset : Event
    }

    private val _state = MutableStateFlow(PasswordRecoveryState())
    val state: StateFlow<PasswordRecoveryState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private var countdown: Job? = null

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, serverEmailError = null, banner = null) }
    }

    fun onCodeChange(value: String) {
        val code = value.filter(Char::isDigit).take(RECOVERY_CODE_LENGTH)
        _state.update { it.copy(code = code, banner = null) }
    }

    fun onNewPasswordChange(value: String) {
        _state.update { it.copy(newPassword = value, serverPasswordError = null, banner = null) }
    }

    fun onNewPasswordConfirmChange(value: String) {
        _state.update { it.copy(newPasswordConfirm = value, serverConfirmError = null, banner = null) }
    }

    /** Step 1's button. A malformed address never reaches the backend. */
    fun sendCode() {
        val current = _state.value
        if (current.isBusy) return
        if (!current.isEmailValid) {
            _state.update { it.copy(submitted = true) }
            return
        }
        requestCode(onSent = { copy(step = RecoveryStep.Code, submitted = false) })
    }

    /** "Reenviar código": same request, and the step stays as it is. */
    fun resendCode() {
        if (!_state.value.canResend) return
        requestCode(onSent = { this })
    }

    private fun requestCode(onSent: PasswordRecoveryState.() -> PasswordRecoveryState) {
        _state.update { it.copy(isSending = true, serverEmailError = null, banner = null) }
        viewModelScope.launch {
            requestRecovery(_state.value.trimmedEmail)
                .onSuccess {
                    _state.update { it.onSent().copy(isSending = false) }
                    startCountdown()
                }
                .onFailure { throwable ->
                    val error = throwable.toAppError()
                    val emailError = error.fieldError(FIELD_EMAIL)
                    _state.update {
                        it.copy(
                            isSending = false,
                            serverEmailError = emailError,
                            banner = if (emailError == null) error.toUserMessage() else null,
                        )
                    }
                }
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        _state.update { it.copy(resendSecondsLeft = RESEND_COUNTDOWN_SECONDS) }
        countdown = viewModelScope.launch {
            while (_state.value.resendSecondsLeft > 0) {
                delay(ONE_SECOND_MS)
                _state.update { it.copy(resendSecondsLeft = it.resendSecondsLeft - 1) }
            }
        }
    }

    /** Step 2's button. A code or password that cannot pass stays on the screen and spends no attempt. */
    fun resetPassword() {
        val current = _state.value
        if (current.isBusy) return
        if (!current.isCodeStepValid) {
            _state.update { it.copy(submitted = true) }
            return
        }
        _state.update {
            it.copy(
                submitted = true,
                isResetting = true,
                serverPasswordError = null,
                serverConfirmError = null,
                banner = null,
            )
        }
        viewModelScope.launch {
            confirmRecovery(current.trimmedEmail, current.code, current.newPassword, current.newPasswordConfirm)
                .onSuccess {
                    _state.update { it.copy(isResetting = false) }
                    _events.emit(Event.PasswordReset)
                }
                .onFailure { throwable -> showResetError(throwable.toAppError()) }
        }
    }

    private fun showResetError(error: AppError) {
        val passwordError = error.fieldError(FIELD_NEW_PASSWORD)
        val confirmError = error.fieldError(FIELD_NEW_PASSWORD_CONFIRM)
        val onAField = passwordError != null || confirmError != null
        _state.update {
            it.copy(
                isResetting = false,
                serverPasswordError = passwordError,
                serverConfirmError = confirmError,
                banner = if (onAField) null else error.toUserMessage(),
            )
        }
    }

    /** Back on the code step returns to the e-mail step with the address; `false` means leave the screen. */
    fun goBackOneStep(): Boolean {
        val current = _state.value
        if (current.step != RecoveryStep.Code || current.isBusy) return false
        _state.update { it.copy(step = RecoveryStep.Email, submitted = false, banner = null) }
        return true
    }

    private fun AppError.fieldError(field: String): String? =
        (this as? AppError.Server)?.fieldErrors?.get(field)?.firstOrNull()

    private companion object {
        /** The server sends one request per minute per address, and answers the same past that limit. */
        const val RESEND_COUNTDOWN_SECONDS = 60
        const val ONE_SECOND_MS = 1_000L
        const val FIELD_EMAIL = "email"
        const val FIELD_NEW_PASSWORD = "new_password"
        const val FIELD_NEW_PASSWORD_CONFIRM = "new_password_confirm"
    }
}
