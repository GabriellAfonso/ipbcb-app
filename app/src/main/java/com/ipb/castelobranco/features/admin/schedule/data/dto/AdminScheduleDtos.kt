package com.ipb.castelobranco.features.admin.schedule.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Generate request ──────────────────────────────────────────────────────────

@Serializable
data class GenerateScheduleRequestDto(
    val year: Int,
    val month: Int,
    val fixed: List<FixedItemDto> = emptyList()
)

@Serializable
data class FixedItemDto(
    @SerialName("schedule_type_id") val scheduleTypeId: Int,
    val date: String,
    @SerialName("member_id") val memberId: Int
)

// ── Generate response ─────────────────────────────────────────────────────────

@Serializable
data class GenerateScheduleResponseDto(
    val year: Int,
    val month: Int,
    val items: List<GeneratedItemDto>
)

@Serializable
data class GeneratedItemDto(
    val date: String,
    val day: Int,
    @SerialName("schedule_type") val scheduleType: GeneratedScheduleTypeDto,
    val member: MemberItemDto,
    val fixed: Boolean = false
)

@Serializable
data class GeneratedScheduleTypeDto(
    val id: Int,
    val name: String,
    val time: String
)

// ── Save request ──────────────────────────────────────────────────────────────

@Serializable
data class SaveScheduleRequestDto(
    val year: Int,
    val month: Int,
    val items: List<SaveScheduleItemDto>
)

@Serializable
data class SaveScheduleItemDto(
    val date: String,
    @SerialName("schedule_type_id") val scheduleTypeId: Int,
    @SerialName("member_id") val memberId: Int
)