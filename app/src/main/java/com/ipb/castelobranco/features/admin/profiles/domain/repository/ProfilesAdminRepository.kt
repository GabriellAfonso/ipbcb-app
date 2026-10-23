package com.ipb.castelobranco.features.admin.profiles.domain.repository

import com.ipb.castelobranco.features.admin.profiles.domain.model.ManagedAccount
import com.ipb.castelobranco.features.admin.profiles.domain.model.ProfilesTab
import kotlinx.coroutines.flow.Flow

/**
 * Accounts for "Perfis", held in memory only. Every action answers with the account detail, which also updates
 * (or, for Pendentes, drops) that account's row in the lists.
 */
interface ProfilesAdminRepository {

    /** `null` until the tab was loaded once. */
    fun observe(tab: ProfilesTab): Flow<List<ManagedAccount>?>

    suspend fun refresh(tab: ProfilesTab): Result<Unit>

    suspend fun detail(userId: String): Result<ManagedAccount>

    suspend fun approve(requestId: Long): Result<ManagedAccount>

    suspend fun deny(requestId: Long, reason: String?): Result<ManagedAccount>

    /** Sets the stored flag without a request; a pending access request is approved with it. */
    suspend fun grantAccess(userId: String): Result<ManagedAccount>

    suspend fun revokeAccess(userId: String): Result<ManagedAccount>

    suspend fun setRequestsBlocked(userId: String, blocked: Boolean): Result<ManagedAccount>

    /** `null` removes the link. */
    suspend fun linkMember(userId: String, memberId: Int?): Result<ManagedAccount>

    /** Sign-out. */
    fun clear()
}
