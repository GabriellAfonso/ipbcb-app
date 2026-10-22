package com.ipb.castelobranco.features.worshiphub.chordcharts.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChordChartDto(
    val id: Int,
    @SerialName("song_id") val songId: Int,
    val content: String,
    val tone: String,
    val instrument: String,
    @SerialName("updated_at") val updatedAt: String,
)
