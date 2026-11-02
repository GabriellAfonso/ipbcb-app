package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.features.profile.domain.repository.AccountRepository
import com.ipb.castelobranco.features.profile.domain.repository.ProfileRepository
import javax.inject.Inject

/** Confirms the recovery e-mail with the received code, then refreshes the cached profile. */
class ConfirmRecoveryEmailUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(code: String): Result<Unit> =
        accountRepository.confirmRecoveryEmail(code).onSuccess {
            profileRepository.refreshMeProfile()
        }
}
