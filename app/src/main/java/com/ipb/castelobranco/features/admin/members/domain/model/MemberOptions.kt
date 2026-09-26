package com.ipb.castelobranco.features.admin.members.domain.model

data class MemberOptions(
    val statuses: List<NamedRef>,
    val roles: List<NamedRef>,
    val ministries: List<NamedRef>,
)
