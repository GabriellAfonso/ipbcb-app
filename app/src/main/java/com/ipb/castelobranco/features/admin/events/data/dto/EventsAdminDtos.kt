package com.ipb.castelobranco.features.admin.events.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `EventRecord` of `backend/specs/020-church-events/contracts/events-api.md`. */
@Serializable
data class EventRecordDto(
    val id: Int,
    val title: String,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String? = null,
    @SerialName("start_time")
    val startTime: String? = null,
    val location: String? = null,
    val description: String? = null,
    @SerialName("image_url")
    val imageUrl: String,
    val visibility: String,
    @SerialName("created_by")
    val createdBy: CreatedByDto? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class CreatedByDto(
    val id: String,
    val name: String = "",
)

@Serializable
data class EventListDto(
    val events: List<EventRecordDto>,
)
