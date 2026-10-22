package com.ipb.castelobranco.features.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateAccessRequestBody(
    @SerialName("type") val type: String,
    @SerialName("message") val message: String? = null,
)

/** The person's own roll record (`GET me/member/`). Kept in memory only, never in a snapshot. */
@Serializable
data class OwnMemberDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("role") val role: String? = null,
    @SerialName("ministries") val ministries: List<String> = emptyList(),
    @SerialName("birth_day") val birthDay: Int? = null,
    @SerialName("birth_month") val birthMonth: Int? = null,
    @SerialName("birth_year") val birthYear: Int? = null,
    @SerialName("baptism_date") val baptismDate: String? = null,
    @SerialName("gender") val gender: String? = null,
)
