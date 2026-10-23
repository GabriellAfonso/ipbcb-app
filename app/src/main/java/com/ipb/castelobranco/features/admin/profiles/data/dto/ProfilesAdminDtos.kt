package com.ipb.castelobranco.features.admin.profiles.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ManagedAccountListDto(
    @SerialName("accounts") val accounts: List<ManagedAccountDto> = emptyList(),
)

/**
 * `member_access` here is the stored flag; [accessViaRole] says a role grants access anyway. [accessGrantedBy] and
 * [accessGrantedAt] say who set the flag and when; both absent for flags set in the Django admin.
 */
@Serializable
data class ManagedAccountDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("username") val username: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("member_access") val memberAccess: Boolean = false,
    @SerialName("access_via_role") val accessViaRole: Boolean = false,
    @SerialName("access_requests_blocked") val accessRequestsBlocked: Boolean = false,
    @SerialName("access_granted_by") val accessGrantedBy: PersonRefDto? = null,
    @SerialName("access_granted_at") val accessGrantedAt: String? = null,
    @SerialName("member") val member: LinkedMemberDto? = null,
    @SerialName("date_joined") val dateJoined: String? = null,
    @SerialName("pending_requests") val pendingRequests: List<AccessRequestDto> = emptyList(),
    /** Detail only: the full history, newest first. */
    @SerialName("requests") val requests: List<AccessRequestDto> = emptyList(),
)

@Serializable
data class LinkedMemberDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
)

@Serializable
data class AccessRequestDto(
    @SerialName("id") val id: Long,
    @SerialName("type") val type: String,
    @SerialName("message") val message: String? = null,
    @SerialName("status") val status: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("decided_by") val decidedBy: PersonRefDto? = null,
    @SerialName("decided_at") val decidedAt: String? = null,
    @SerialName("denial_reason") val denialReason: String? = null,
)

/** A leader as "Perfis" names them: who decided a request or granted access. */
@Serializable
data class PersonRefDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String,
)

@Serializable
data class DenyRequestBody(
    @SerialName("reason") val reason: String? = null,
)
