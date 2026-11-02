package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.features.profile.domain.repository.AccountRepository
import javax.inject.Inject

/** Sends a confirmation code to the current recovery e-mail. */
class SendRecoveryEmailCodeUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.sendRecoveryEmailCode()
}
