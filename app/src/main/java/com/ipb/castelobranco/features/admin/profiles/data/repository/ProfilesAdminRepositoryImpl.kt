package com.ipb.castelobranco.features.admin.profiles.data.repository

import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.mapError
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.admin.profiles.data.api.ProfilesAdminApi
import com.ipb.castelobranco.features.admin.profiles.data.api.ProfilesAdminEndpoints
import com.ipb.castelobranco.features.admin.profiles.data.dto.AccessRequestDto
import com.ipb.castelobranco.features.admin.profiles.data.dto.DenyRequestBody
import com.ipb.castelobranco.features.admin.profiles.data.dto.ManagedAccountDto
import com.ipb.castelobranco.features.admin.profiles.domain.model.AccessRequest
import com.ipb.castelobranco.features.admin.profiles.domain.model.LinkedMember
import com.ipb.castelobranco.features.admin.profiles.domain.model.ManagedAccount
import com.ipb.castelobranco.features.admin.profiles.domain.model.ProfilesTab
import com.ipb.castelobranco.features.admin.profiles.domain.model.RequestStatus
import com.ipb.castelobranco.features.admin.profiles.domain.repository.ProfilesAdminRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import retrofit2.Response
import timber.log.Timber
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accounts held per tab, in memory only, each with its `ETag`. Logs carry ids only — never a name or a message.
 */
@Singleton
class ProfilesAdminRepositoryImpl @Inject constructor(
    private val api: ProfilesAdminApi,
) : ProfilesAdminRepository {

    private val lists = ProfilesTab.entries.associateWith { MutableStateFlow<List<ManagedAccount>?>(null) }
    private val etags = mutableMapOf<ProfilesTab, String?>()

    override fun observe(tab: ProfilesTab): Flow<List<ManagedAccount>?> = lists.getValue(tab).asStateFlow()

    override suspend fun refresh(tab: ProfilesTab): Result<Unit> = runCatching {
        val held = lists.getValue(tab).value
        val etag = etags[tab].takeIf { held != null }
        val response = api.getAccounts(status = tab.wireKey, ifNoneMatch = etag)
        if (response.code() == HTTP_NOT_MODIFIED && etag != null) return@runCatching
        if (!response.isSuccessful) throw response.toAppError()
        val body = response.body() ?: throw AppError.Unknown(message = EMPTY_BODY)

        etags[tab] = response.headers()[ProfilesAdminEndpoints.ETAG]
        lists.getValue(tab).value = body.accounts.map { it.toDomain() }
    }.mapError()

    override suspend fun detail(userId: String): Result<ManagedAccount> =
        account { api.getAccount(userId) }

    override suspend fun approve(requestId: Long): Result<ManagedAccount> =
        account { api.approveRequest(requestId) }.onSuccess { Timber.i("Access request %d approved", requestId) }

    override suspend fun deny(requestId: Long, reason: String?): Result<ManagedAccount> =
        account { api.denyRequest(requestId, DenyRequestBody(reason = reason)) }
            .onSuccess { Timber.i("Access request %d denied", requestId) }

    override suspend fun grantAccess(userId: String): Result<ManagedAccount> =
        account { api.grantAccess(userId) }

    override suspend fun revokeAccess(userId: String): Result<ManagedAccount> =
        account { api.revokeAccess(userId) }

    override suspend fun setRequestsBlocked(userId: String, blocked: Boolean): Result<ManagedAccount> =
        account {
            api.updateAccount(userId, buildJsonObject { put(FIELD_BLOCKED, JsonPrimitive(blocked)) })
        }

    override suspend fun linkMember(userId: String, memberId: Int?): Result<ManagedAccount> =
        account {
            api.updateAccount(
                userId,
                buildJsonObject { put(FIELD_MEMBER_ID, memberId?.let(::JsonPrimitive) ?: JsonNull) },
            )
        }

    override fun clear() {
        lists.values.forEach { it.value = null }
        etags.clear()
    }

    /** Reads an account answer and puts it in the lists, so they never wait for a reload to show it. */
    private suspend fun account(call: suspend () -> Response<ManagedAccountDto>): Result<ManagedAccount> =
        runCatching {
            val response = call()
            if (!response.isSuccessful) throw response.toAppError()
            val account = (response.body() ?: throw AppError.Unknown(message = EMPTY_BODY)).toDomain()
            patchLists(account)
            account
        }.mapError()

    private fun patchLists(account: ManagedAccount) {
        val row = account.copy(requests = emptyList())
        lists.getValue(ProfilesTab.ALL).update { list -> list?.map { if (it.id == row.id) row else it } }
        lists.getValue(ProfilesTab.PENDING).update { list ->
            list?.mapNotNull { if (it.id != row.id) it else row.takeIf { r -> r.pendingRequests.isNotEmpty() } }
        }
    }

    private companion object {
        const val HTTP_NOT_MODIFIED = 304
        const val EMPTY_BODY = "Resposta vazia"
        const val FIELD_BLOCKED = "access_requests_blocked"
        const val FIELD_MEMBER_ID = "member_id"
    }
}

internal fun ManagedAccountDto.toDomain() = ManagedAccount(
    id = id,
    name = name,
    username = username?.takeIf { it.isNotBlank() },
    photoUrl = photoUrl?.takeIf { it.isNotBlank() },
    memberAccess = memberAccess,
    accessViaRole = accessViaRole,
    accessGrantedBy = accessGrantedBy?.name?.takeIf { it.isNotBlank() },
    accessGrantedAt = accessGrantedAt.toDateTimeOrNull(),
    requestsBlocked = accessRequestsBlocked,
    member = member?.let { LinkedMember(it.id, it.name) },
    dateJoined = dateJoined?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    pendingRequests = pendingRequests.mapNotNull { it.toDomain() },
    requests = requests.mapNotNull { it.toDomain() },
)

/** A request of an unknown type or status is left out rather than shown wrong. */
internal fun AccessRequestDto.toDomain(): AccessRequest? {
    val type = AccessRequestType.fromWire(type) ?: return null
    val status = RequestStatus.fromWire(status) ?: return null
    return AccessRequest(
        id = id,
        type = type,
        message = message?.takeIf { it.isNotBlank() },
        status = status,
        createdAt = createdAt.toDateTimeOrNull(),
        decidedBy = decidedBy?.name,
        decidedAt = decidedAt.toDateTimeOrNull(),
        denialReason = denialReason?.takeIf { it.isNotBlank() },
    )
}

private fun String?.toDateTimeOrNull(): OffsetDateTime? =
    this?.let {
        try {
            OffsetDateTime.parse(it)
        } catch (_: DateTimeParseException) {
            null
        }
    }
