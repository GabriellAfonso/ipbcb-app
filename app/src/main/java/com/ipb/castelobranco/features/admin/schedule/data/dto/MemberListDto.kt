package com.ipb.castelobranco.features.admin.schedule.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberListDto(
    val members: List<MemberItemDto>
)

@Serializable
data class MemberItemDto(
    val id: Int,
    val name: String
)
