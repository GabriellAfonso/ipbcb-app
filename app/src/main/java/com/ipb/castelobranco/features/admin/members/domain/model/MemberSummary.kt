package com.ipb.castelobranco.features.admin.members.domain.model

/** What a list card needs. [isValid] is the wire `is_active` ("perfil válido"). */
data class MemberSummary(
    val id: Int,
    val name: String,
    val photoUrl: String?,
    val status: NamedRef?,
    val isValid: Boolean,
)
