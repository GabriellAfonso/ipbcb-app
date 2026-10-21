package com.ipb.castelobranco.features.events.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `EventPublic` of `backend/specs/020-church-events/contracts/events-api.md`. */
@Serializable
data class EventPublicDto(
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
    val visibility: String = VISIBILITY_PUBLIC,
) {
    companion object {
        const val VISIBILITY_PUBLIC = "public"
        const val VISIBILITY_MEMBERS = "members"
    }
}
