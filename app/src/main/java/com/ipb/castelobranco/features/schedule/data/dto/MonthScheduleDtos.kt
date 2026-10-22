package com.ipb.castelobranco.features.schedule.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MonthScheduleDto(
    val year: Int,
    val month: Int,
    val schedule: Map<String, ScheduleEntryDto>
)

@Serializable
data class ScheduleEntryDto(
    val time: String,
    val items: List<ScheduleItemDto>
)

@Serializable
data class ScheduleItemDto(
    val date: String? = null,
    val day: Int,
    val member: MemberDto,
    @SerialName("schedule_type")
    val scheduleType: ScheduleTypeDto? = null
)

@Serializable
data class MemberDto(
    val id: Int,
    val name: String
)

@Serializable
data class ScheduleTypeDto(
    val id: Int,
    val name: String
)
