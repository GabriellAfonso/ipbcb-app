package com.ipb.castelobranco.features.worshiphub.lyrics.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LyricsDto(
    val id: Int,
    @SerialName("song_id") val songId: Int,
    val content: String,
    @SerialName("updated_at") val updatedAt: String,
)
