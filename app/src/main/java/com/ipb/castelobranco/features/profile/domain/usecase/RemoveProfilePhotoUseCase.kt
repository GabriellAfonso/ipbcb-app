package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.features.profile.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * Removes the photo on the server (the data source also drops the local copy), then refreshes the profile so the
 * cached address goes away and nothing downloads the photo again.
 */
class RemoveProfilePhotoUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(): Result<Unit> =
        repository.deleteProfilePhoto().onSuccess {
            repository.refreshMeProfile()
        }
}
