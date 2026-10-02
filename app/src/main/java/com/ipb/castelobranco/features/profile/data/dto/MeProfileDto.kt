package com.ipb.castelobranco.features.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `is_admin` left the contract with backend 012; a cached profile that still carries it decodes
 * because unknown keys are ignored. `roles` and `permissions` default to empty so a profile cached before backend 012 still decodes,
 * as "no role". Their values stay raw strings: an unknown value must reach the mapper, which denies
 * it, instead of failing deserialization.
 */
@Serializable
data class MeProfileDto(
    @SerialName("name") val name: String,
    @SerialName("is_member") val isMember: Boolean,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("roles") val roles: List<RoleDto> = emptyList(),
    @SerialName("permissions") val permissions: Map<String, String?> = emptyMap(),
    /** The member record an admin linked to the user; `null` when not linked or cached before backend 015. */
    @SerialName("member_id") val memberId: Long? = null,
    /** Linked member belongs to the "Louvor" ministry; `false` when absent (cached before backend 017). */
    @SerialName("is_worship_member") val isWorshipMember: Boolean = false,
    /** Worship member with `manage` on `songs`; `false` when absent (cached before backend 017). */
    @SerialName("can_save_setlist") val canSaveSetlist: Boolean = false,
)

@Serializable
data class RoleDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
)
