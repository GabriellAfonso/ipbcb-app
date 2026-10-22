package com.ipb.castelobranco.features.auth.presentation.viewmodel

import timber.log.Timber
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.features.auth.data.mapper.parseLoginError
import com.ipb.castelobranco.features.auth.data.mapper.parseRegisterError
import com.ipb.castelobranco.features.auth.domain.model.RegisterErrors
import com.ipb.castelobranco.features.auth.domain.usecase.LoginUseCase
import com.ipb.castelobranco.features.auth.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _isGoogleLoading = MutableStateFlow(false)
    val isGoogleLoading: StateFlow<Boolean> = _isGoogleLoading.asStateFlow()

    private val _registerErrors = MutableStateFlow(RegisterErrors())
    val registerErrors: StateFlow<RegisterErrors> = _registerErrors.asStateFlow()

    fun clearLoginError() {
        _loginError.value = null
    }

    fun clearRegisterErrors() {
        _registerErrors.value = RegisterErrors()
    }

    fun singIn(username: String, password: String) {
        viewModelScope.launch {
            _loginError.value = null
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
        }
    }

    fun signInWithGoogle(idToken: String) {
        Timber.d("signInWithGoogle called")
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

    fun singUp(
        username: String,
        firstName: String,
        lastName: String,
        password: String,
        passwordConfirm: String,
    ) {
        viewModelScope.launch {
            _registerErrors.value = RegisterErrors()
            when (val result = registerUseCase(username, firstName, lastName, password, passwordConfirm)) {
                RegisterUseCase.Result.Success -> {
                    Timber.d("Registration successful")
                    _events.tryEmit(AuthEvent.RegisterSuccess)
                }
                is RegisterUseCase.Result.Failure -> {
                    _registerErrors.value = parseRegisterError(result.rawMessage)
                    Timber.e("Registration failed")
                }
            }
        }
    }

    fun signInWithGoogle() {
        // iniciar fluxo de login com Google
    }
}
