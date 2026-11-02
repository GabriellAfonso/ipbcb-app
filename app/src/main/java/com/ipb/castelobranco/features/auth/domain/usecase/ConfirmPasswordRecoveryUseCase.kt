package com.ipb.castelobranco.features.auth.domain.usecase

import com.ipb.castelobranco.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

/** Sets a new password with a code. Signing in stays with the person, so no session starts here. */
class ConfirmPasswordRecoveryUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        code: String,
        newPassword: String,
        newPasswordConfirm: String,
    ): Result<Unit> = repository.confirmPasswordRecovery(email, code, newPassword, newPasswordConfirm)
}
