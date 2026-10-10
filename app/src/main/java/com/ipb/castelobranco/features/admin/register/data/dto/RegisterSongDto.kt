package com.ipb.castelobranco.features.admin.register.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterSongRequestDto(
    val title: String,
    val artist: String
)

@Serializable
data class RegisterSongResponseDto(
    val id: Int,
    val title: String,
    val artist: String,
    @SerialName("category") val categoryName: String = "",
    @SerialName("youtube_link") val youtubeLink: String? = null,
)
