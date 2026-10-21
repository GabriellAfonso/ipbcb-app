package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.features.profile.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Changes the password. The rules (length, confirmation) are the server's; its field errors come back on the
 * failure. On success the session already runs on the new pair.
 */
class ChangePasswordUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(current: String, new: String, confirm: String): Result<Unit> =
        repository.changePassword(current = current, new = new, confirm = confirm)
}
