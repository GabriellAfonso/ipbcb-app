package com.ipb.castelobranco.features.admin.members.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NamedRefDto(
    val id: Int,
    val name: String,
)

@Serializable
data class MemberSummaryDto(
    val id: Int,
    val name: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    val status: NamedRefDto? = null,
    @SerialName("is_active") val isActive: Boolean,
)

@Serializable
data class MemberListDto(
    val members: List<MemberSummaryDto>,
)

@Serializable
data class MemberRecordDto(
    val id: Int,
    val name: String,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    @SerialName("birth_day") val birthDay: Int? = null,
    @SerialName("birth_month") val birthMonth: Int? = null,
    @SerialName("birth_year") val birthYear: Int? = null,
    val gender: String? = null,
    val status: NamedRefDto? = null,
    val role: NamedRefDto? = null,
    val ministries: List<NamedRefDto> = emptyList(),
    @SerialName("baptism_date") val baptismDate: String? = null,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class MemberOptionsDto(
    val statuses: List<NamedRefDto> = emptyList(),
    val roles: List<NamedRefDto> = emptyList(),
    val ministries: List<NamedRefDto> = emptyList(),
)

@Serializable
data class PhotoUrlDto(
    @SerialName("photo_url") val photoUrl: String,
)

@Serializable
data class HistoryEditorDto(
    val id: String,
    val name: String,
)

@Serializable
data class HistoryEntryDto(
    val id: Int,
    val editor: HistoryEditorDto? = null,
    val field: String,
    @SerialName("old_value") val oldValue: String? = null,
    @SerialName("new_value") val newValue: String? = null,
    @SerialName("changed_at") val changedAt: String,
)

@Serializable
data class HistoryDto(
    val history: List<HistoryEntryDto>,
)
