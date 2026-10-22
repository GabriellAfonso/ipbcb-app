package com.ipb.castelobranco.features.admin.profiles

import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.features.admin.profiles.domain.model.AccessRequest
import com.ipb.castelobranco.features.admin.profiles.domain.model.ManagedAccount
import com.ipb.castelobranco.features.admin.profiles.domain.model.ProfilesTab
import com.ipb.castelobranco.features.admin.profiles.domain.model.RequestStatus
import com.ipb.castelobranco.features.admin.profiles.domain.repository.ProfilesAdminRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Lists you set by hand; every action is recorded and answered with [actionResult]. */
class FakeProfilesAdminRepository : ProfilesAdminRepository {

    val lists = ProfilesTab.entries.associateWith { MutableStateFlow<List<ManagedAccount>?>(null) }
    var refreshResult: Result<Unit> = Result.success(Unit)
    val refreshed = mutableListOf<ProfilesTab>()

    var detailResult: Result<ManagedAccount> = Result.success(account())
    var detailCalls = 0
    var actionResult: Result<ManagedAccount> = Result.success(account())
    val actions = mutableListOf<String>()

    override fun observe(tab: ProfilesTab): Flow<List<ManagedAccount>?> = lists.getValue(tab)

    override suspend fun refresh(tab: ProfilesTab): Result<Unit> {
        refreshed += tab
        return refreshResult
    }

    override suspend fun detail(userId: String): Result<ManagedAccount> {
        detailCalls++
        return detailResult
    }

    override suspend fun approve(requestId: Long) = record("approve $requestId")
    override suspend fun deny(requestId: Long, reason: String?) = record("deny $requestId $reason")
    override suspend fun revokeAccess(userId: String) = record("revoke $userId")
    override suspend fun setRequestsBlocked(userId: String, blocked: Boolean) = record("block $userId $blocked")
    override suspend fun linkMember(userId: String, memberId: Int?) = record("link $userId $memberId")

    override fun clear() {
        lists.values.forEach { it.value = null }
    }

    private fun record(action: String): Result<ManagedAccount> {
        actions += action
        return actionResult
    }
}

fun pendingRequest(id: Long = 41, type: AccessRequestType = AccessRequestType.ACCESS) = AccessRequest(
    id = id,
    type = type,
    message = "Sou membro.",
    status = RequestStatus.PENDING,
    createdAt = null,
    decidedBy = null,
    decidedAt = null,
    denialReason = null,
)

fun account(
    id: String = "u-1",
    name: String = "Ana Souza",
    memberAccess: Boolean = false,
    pending: List<AccessRequest> = listOf(pendingRequest()),
    blocked: Boolean = false,
) = ManagedAccount(
    id = id,
    name = name,
    username = "ana",
    photoUrl = null,
    memberAccess = memberAccess,
    accessViaRole = false,
    requestsBlocked = blocked,
    member = null,
    dateJoined = null,
    pendingRequests = pending,
    requests = emptyList(),
)
