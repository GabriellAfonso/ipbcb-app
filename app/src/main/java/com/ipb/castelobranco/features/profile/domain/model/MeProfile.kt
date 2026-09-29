package com.ipb.castelobranco.features.profile.domain.model

import com.ipb.castelobranco.core.domain.access.Access

data class MeProfile(
    val name: String,
    val isMember: Boolean,
    val photoUrl: String?,
    val access: Access = Access.NONE,
)
