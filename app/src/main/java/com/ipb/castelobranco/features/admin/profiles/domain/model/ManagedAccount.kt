package com.ipb.castelobranco.features.admin.profiles.domain.model

import com.ipb.castelobranco.core.domain.access.AccessRequestType
import java.time.LocalDate
import java.time.OffsetDateTime

enum class RequestStatus(val wireKey: String) {
    PENDING("pending"),
    APPROVED("approved"),
    DENIED("denied");

    companion object {
        fun fromWire(value: String): RequestStatus? = entries.firstOrNull { it.wireKey == value }
    }
}

data class AccessRequest(
    val id: Long,
    val type: AccessRequestType,
    val message: String?,
    val status: RequestStatus,
    val createdAt: OffsetDateTime?,
    val decidedBy: String?,
    val decidedAt: OffsetDateTime?,
    val denialReason: String?,
)

data class LinkedMember(val id: Int, val name: String)

/** An app account as leaders see it in "Perfis". There is no e-mail in it on purpose. */
data class ManagedAccount(
    val id: String,
    val name: String,
    val username: String?,
    val photoUrl: String?,
    /** The stored flag, not the effective access. */
    val memberAccess: Boolean,
    /** Admin/Liderança grants access anyway: revoking the flag changes nothing for them. */
    val accessViaRole: Boolean,
    /** Who set the stored flag (name), when the server knows; `null` for flags set in the Django admin. */
    val accessGrantedBy: String?,
    val accessGrantedAt: OffsetDateTime?,
    val requestsBlocked: Boolean,
    val member: LinkedMember?,
    val dateJoined: LocalDate?,
    val pendingRequests: List<AccessRequest>,
    /** The full history, newest first; empty in the lists. */
    val requests: List<AccessRequest>,
)

enum class ProfilesTab(val wireKey: String) {
    PENDING("pending"),
    ALL("all"),
}

/** The server refuses a longer denial reason. */
const val DENIAL_REASON_MAX_LENGTH = 500
