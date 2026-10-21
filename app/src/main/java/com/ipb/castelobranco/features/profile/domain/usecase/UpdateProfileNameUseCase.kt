package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.profile.domain.repository.AccountRepository
import com.ipb.castelobranco.features.profile.domain.repository.ProfileRepository
import javax.inject.Inject

/** Saves a trimmed name, then refreshes the cached profile so every screen shows it. */
class UpdateProfileNameUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(name: String): Result<Unit> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(AppError.Unknown(message = "Blank name", userMessage = BLANK_NAME_MESSAGE))
        }
        return accountRepository.updateName(trimmed).onSuccess {
            profileRepository.refreshMeProfile()
        }
    }

    companion object {
        private const val BLANK_NAME_MESSAGE = "Informe um nome."
    }
}
