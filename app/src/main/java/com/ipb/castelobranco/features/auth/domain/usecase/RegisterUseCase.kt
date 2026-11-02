package com.ipb.castelobranco.features.auth.domain.usecase

import com.ipb.castelobranco.core.domain.auth.AuthEventBus
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val authEventBus: AuthEventBus,
) {
    sealed interface Result {
        data object Success : Result
        data class Failure(
            val rawMessage: String,
            /** The backend's per-field messages, keyed by its field names. */
            val fieldErrors: Map<String, List<String>>? = null,
        ) : Result
    }

    suspend operator fun invoke(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        passwordConfirm: String,
    ): Result =
        repository.signUp(username, firstName, lastName, email, password, passwordConfirm).fold(
            onSuccess = {
                authEventBus.emit(AuthEventBus.Event.LoginSuccess)
                Result.Success
            },
            onFailure = {
                Result.Failure(
                    rawMessage = it.message ?: "Erro ao registrar",
                    fieldErrors = (it as? AppError.Server)?.fieldErrors,
                )
            }
        )
}
