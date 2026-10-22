package com.ipb.castelobranco.features.admin.profiles.domain.usecase

import com.ipb.castelobranco.features.admin.profiles.domain.model.ManagedAccount
import com.ipb.castelobranco.features.admin.profiles.domain.model.ProfilesTab
import com.ipb.castelobranco.features.admin.profiles.domain.repository.ProfilesAdminRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveManagedAccountsUseCase @Inject constructor(private val repository: ProfilesAdminRepository) {
    operator fun invoke(tab: ProfilesTab): Flow<List<ManagedAccount>?> = repository.observe(tab)
}

class RefreshManagedAccountsUseCase @Inject constructor(private val repository: ProfilesAdminRepository) {
    suspend operator fun invoke(tab: ProfilesTab): Result<Unit> = repository.refresh(tab)
}

class GetManagedAccountUseCase @Inject constructor(private val repository: ProfilesAdminRepository) {
    suspend operator fun invoke(userId: String): Result<ManagedAccount> = repository.detail(userId)
}

class ApproveAccessRequestUseCase @Inject constructor(private val repository: ProfilesAdminRepository) {
    suspend operator fun invoke(requestId: Long): Result<ManagedAccount> = repository.approve(requestId)
}

class DenyAccessRequestUseCase @Inject constructor(private val repository: ProfilesAdminRepository) {
    /** A blank reason is sent as absent. */
    suspend operator fun invoke(requestId: Long, reason: String?): Result<ManagedAccount> =
        repository.deny(requestId, reason?.trim()?.takeIf { it.isNotEmpty() })
}

class RevokeMemberAccessUseCase @Inject constructor(private val repository: ProfilesAdminRepository) {
    suspend operator fun invoke(userId: String): Result<ManagedAccount> = repository.revokeAccess(userId)
}

class SetAccessRequestsBlockedUseCase @Inject constructor(private val repository: ProfilesAdminRepository) {
    suspend operator fun invoke(userId: String, blocked: Boolean): Result<ManagedAccount> =
        repository.setRequestsBlocked(userId, blocked)
}

class LinkAccountToMemberUseCase @Inject constructor(private val repository: ProfilesAdminRepository) {
    suspend operator fun invoke(userId: String, memberId: Int?): Result<ManagedAccount> =
        repository.linkMember(userId, memberId)
}
