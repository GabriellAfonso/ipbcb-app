package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.features.profile.domain.repository.AccountRepository
import com.ipb.castelobranco.features.profile.domain.repository.ProfileRepository
import javax.inject.Inject

/** Sets or changes the recovery e-mail with the current password, then refreshes the cached profile. */
class ChangeRecoveryEmailUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(email: String, currentPassword: String): Result<Unit> =
        accountRepository.changeRecoveryEmail(email.trim(), currentPassword).onSuccess {
            profileRepository.refreshMeProfile()
        }
}
