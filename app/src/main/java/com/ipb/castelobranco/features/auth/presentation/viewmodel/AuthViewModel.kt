package com.ipb.castelobranco.features.auth.presentation.viewmodel

import timber.log.Timber
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.features.auth.data.mapper.parseLoginError
import com.ipb.castelobranco.features.auth.data.mapper.parseRegisterError
import com.ipb.castelobranco.features.auth.domain.model.RegisterErrors
import com.ipb.castelobranco.features.auth.domain.model.RegisterRules
import com.ipb.castelobranco.features.auth.domain.usecase.LoginUseCase
import com.ipb.castelobranco.features.auth.domain.usecase.RegisterUseCase
import com.ipb.castelobranco.features.auth.presentation.state.RegisterFormState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {

    sealed class AuthEvent {
        data object RegisterSuccess : AuthEvent()
        data object LoginSuccess : AuthEvent()
    }

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoginLoading = MutableStateFlow(false)
    val isLoginLoading: StateFlow<Boolean> = _isLoginLoading.asStateFlow()

    private val _isGoogleLoading = MutableStateFlow(false)
    val isGoogleLoading: StateFlow<Boolean> = _isGoogleLoading.asStateFlow()

    private val _registerForm = MutableStateFlow(RegisterFormState())
    val registerForm: StateFlow<RegisterFormState> = _registerForm.asStateFlow()

    fun clearLoginError() {
        _loginError.value = null
    }

    fun onFirstNameChange(value: String) = editRegisterForm({ copy(firstName = null) }) { copy(firstName = value) }

    fun onLastNameChange(value: String) = editRegisterForm({ copy(lastName = null) }) { copy(lastName = value) }

    fun onUsernameChange(value: String) =
        editRegisterForm({ copy(username = null) }) { copy(username = RegisterRules.normalizeUsername(value)) }

    fun onPasswordChange(value: String) = editRegisterForm({ copy(password = null) }) { copy(password = value) }

    fun onPasswordConfirmChange(value: String) =
        editRegisterForm({ copy(passwordConfirm = null) }) { copy(passwordConfirm = value) }

    /** An edit drops that field's backend error and every banner: the person is acting on it. */
    private fun editRegisterForm(
        clearServerError: RegisterErrors.() -> RegisterErrors,
        edit: RegisterFormState.() -> RegisterFormState,
    ) {
        _loginError.value = null
        _registerForm.update { form ->
            form.edit().copy(
                serverErrors = form.serverErrors.clearServerError().copy(general = null),
                showFixBanner = false,
            )
        }
    }

    fun singIn(username: String, password: String) {
        if (_isLoginLoading.value || _isGoogleLoading.value) return
        if (username.isBlank() || password.isBlank()) {
            _loginError.value = EMPTY_CREDENTIALS_MESSAGE
            return
        }
        viewModelScope.launch {
            _isLoginLoading.value = true
            _loginError.value = null
            try {
                when (val result = loginUseCase.withCredentials(username, password)) {
                    LoginUseCase.Result.Success -> {
                        Timber.d("Login successful")
                        _events.tryEmit(AuthEvent.LoginSuccess)
                    }
                    is LoginUseCase.Result.Failure -> {
                        _loginError.value = parseLoginError(result.rawMessage)
                        Timber.e("Login failed")
                    }
                }
            } finally {
                _isLoginLoading.value = false
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        Timber.d("signInWithGoogle called")
        if (_isLoginLoading.value || _registerForm.value.isLoading) return
        viewModelScope.launch {
            _isGoogleLoading.value = true
            _loginError.value = null
            try {
                when (val result = loginUseCase.withGoogle(idToken)) {
                    LoginUseCase.Result.Success -> _events.tryEmit(AuthEvent.LoginSuccess)
                    is LoginUseCase.Result.Failure -> {
                        _loginError.value = result.rawMessage
                        Timber.e("Google sign-in failed")
                    }
                }
            } finally {
                _isGoogleLoading.value = false
            }
        }
    }

    /** Checks the form first (`specs/auth/spec.md` §4.1): with a field wrong, the backend is not called. */
    fun singUp() {
        val form = _registerForm.value
        if (form.isLoading || _isGoogleLoading.value) return
        _loginError.value = null
        if (!form.isValid) {
            _registerForm.value = form.copy(submitted = true, showFixBanner = true)
            return
        }
        _registerForm.value = form.copy(submitted = true, isLoading = true, serverErrors = RegisterErrors())
        viewModelScope.launch {
            try {
                val result = registerUseCase(
                    form.username, form.firstName.trim(), form.lastName.trim(), form.password, form.passwordConfirm,
                )
                when (result) {
                    RegisterUseCase.Result.Success -> {
                        Timber.d("Registration successful")
                        _events.tryEmit(AuthEvent.RegisterSuccess)
                    }
                    is RegisterUseCase.Result.Failure -> {
                        _registerForm.update { it.copy(serverErrors = parseRegisterError(result.rawMessage)) }
                        Timber.e("Registration failed")
                    }
                }
            } finally {
                _registerForm.update { it.copy(isLoading = false) }
            }
        }
    }

    fun signInWithGoogle() {
        // iniciar fluxo de login com Google
    }

    companion object {
        const val EMPTY_CREDENTIALS_MESSAGE = "Preencha usuário e senha."
    }
}
