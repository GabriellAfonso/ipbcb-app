package com.ipb.castelobranco.features.worshiphub.tables.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepertoireDraftDto(
    @SerialName("rows") val rows: List<DraftRowDto>,
)

@Serializable
data class DraftRowDto(
    @SerialName("position") val position: Int,
    @SerialName("song_id") val songId: Int? = null,
    @SerialName("tone") val tone: String = "",
    @SerialName("fixed") val isFixed: Boolean = false,
)
