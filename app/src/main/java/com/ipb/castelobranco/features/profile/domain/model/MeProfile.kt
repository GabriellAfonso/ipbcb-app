package com.ipb.castelobranco.features.profile.domain.model

import com.ipb.castelobranco.core.domain.access.Access

data class MeProfile(
    val name: String,
    val isMember: Boolean,
    val photoUrl: String?,
    val access: Access = Access.NONE,
    /** The member record linked to the user, `null` when not linked. */
    val memberId: Long? = null,
    val isWorshipMember: Boolean = false,
    val canSaveSetlist: Boolean = false,
)
