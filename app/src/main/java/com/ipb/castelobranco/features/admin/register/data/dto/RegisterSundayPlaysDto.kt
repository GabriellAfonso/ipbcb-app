package com.ipb.castelobranco.features.admin.register.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterSundayPlaysRequestDto(
    val date: String,
    val plays: List<RegisterSundayPlayItemDto>
)

@Serializable
data class RegisterSundayPlayItemDto(
    @SerialName("song_id") val songId: Int,
    val position: Int,
    val tone: String
)

@Serializable
data class RegisterSundayPlaysResponseDto(
    val created: Int
)
