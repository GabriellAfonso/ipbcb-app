package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.core.domain.auth.AuthEventBus
import com.ipb.castelobranco.features.profile.domain.model.AccountDeletionProof
import com.ipb.castelobranco.features.profile.domain.repository.AccountRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class FakeAccountRepository : AccountRepository {
    var updateNameResult: Result<Unit> = Result.success(Unit)
    var changePasswordResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)

    val names = mutableListOf<String>()
    val passwordChanges = mutableListOf<Triple<String, String, String>>()
    val deletions = mutableListOf<AccountDeletionProof>()

    override suspend fun updateName(name: String): Result<Unit> {
        names += name
        return updateNameResult
    }

    override suspend fun changePassword(current: String, new: String, confirm: String): Result<Unit> {
        passwordChanges += Triple(current, new, confirm)
        return changePasswordResult
    }

    override suspend fun deleteAccount(proof: AccountDeletionProof): Result<Unit> {
        deletions += proof
        return deleteResult
    }
}

/** Records what is emitted; nothing collects in a unit test. */
class RecordingAuthEventBus : AuthEventBus {
    val emitted = mutableListOf<AuthEventBus.Event>()
    override val events: SharedFlow<AuthEventBus.Event> = MutableSharedFlow()

    override fun emit(event: AuthEventBus.Event) {
        emitted += event
    }
}
