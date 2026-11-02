package com.ipb.castelobranco.features.auth.domain.usecase

import com.ipb.castelobranco.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

/** Asks for recovery codes; success never says whether the address has an account. */
class RequestPasswordRecoveryUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Result<Unit> = repository.requestPasswordRecovery(email)
}
