package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.core.domain.auth.AuthEventBus
import com.ipb.castelobranco.features.profile.domain.model.AccountDeletionProof
import com.ipb.castelobranco.features.profile.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Deletes the account. On success it announces [AuthEventBus.Event.AccountDeleted], and the app-wide session owner
 * ends the session the way a logout does, so the cleanup never depends on the profile screen still being open.
 */
class DeleteAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
    private val authEventBus: AuthEventBus,
) {
    suspend operator fun invoke(proof: AccountDeletionProof): Result<Unit> =
        repository.deleteAccount(proof).onSuccess {
            authEventBus.emit(AuthEventBus.Event.AccountDeleted)
        }
}
