package com.ipb.castelobranco.features.profile.domain.model

import com.ipb.castelobranco.core.domain.access.Access
import java.time.LocalDate

/** The server refuses a longer name. */
const val PROFILE_NAME_MAX_LENGTH = 100

data class MeProfile(
    val name: String,
    val isMember: Boolean,
    val photoUrl: String?,
    val access: Access = Access.NONE,
    /** The member record linked to the user, `null` when not linked. */
    val memberId: Long? = null,
    val isWorshipMember: Boolean = false,
    val canSaveSetlist: Boolean = false,
    /** Login name, `null` when unknown. */
    val username: String? = null,
    /** Whether the account has a password, `null` when unknown. */
    val hasPassword: Boolean? = null,
    /** When the account was created, `null` when unknown. */
    val dateJoined: LocalDate? = null,
)
