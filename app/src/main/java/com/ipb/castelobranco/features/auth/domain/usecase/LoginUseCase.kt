package com.ipb.castelobranco.features.auth.domain.usecase

import com.ipb.castelobranco.core.domain.auth.AuthEventBus
import com.ipb.castelobranco.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val authEventBus: AuthEventBus,
) {
    sealed interface Result {
        data object Success : Result
        data class Failure(val rawMessage: String) : Result
    }

    suspend fun withCredentials(username: String, password: String): Result =
        repository.signIn(username, password).fold(
            onSuccess = {
                authEventBus.emit(AuthEventBus.Event.LoginSuccess)
                Result.Success
            },
            onFailure = { Result.Failure(it.message ?: "Erro ao fazer login") }
        )

    suspend fun withGoogle(idToken: String): Result =
        repository.signInWithGoogle(idToken).fold(
            onSuccess = {
                authEventBus.emit(AuthEventBus.Event.LoginSuccess)
                Result.Success
            },
            onFailure = { Result.Failure(it.message ?: "Erro ao entrar com Google") }
        )
}
