// app/src/main/java/com/gabrielafonso/ipb/castelobranco/data/api/AllSongsDto.kt
package com.ipb.castelobranco.features.worshiphub.tables.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AllSongDto(
    val id: Int,
    val title: String,
    val artist: String,
    @SerialName("category") val categoryName: String = "",
    @SerialName("youtube_link") val youtubeLink: String? = null,
)
