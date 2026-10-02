package com.ipb.castelobranco.core.data.setlist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The server's Setlist object (`backend/specs/017-sunday-setlist-push/contracts/setlist-api.md`). */
@Serializable
data class SetlistDto(
    @SerialName("date") val date: String,
    @SerialName("items") val items: List<SetlistItemDto> = emptyList(),
    @SerialName("saved_by_name") val savedByName: String? = null,
    @SerialName("saved_at") val savedAt: String = "",
)

@Serializable
data class SetlistItemDto(
    @SerialName("position") val position: Int,
    @SerialName("song_id") val songId: Int,
    @SerialName("title") val title: String = "",
    @SerialName("artist") val artist: String = "",
    @SerialName("tone") val tone: String = "",
)

/** `GET api/setlists/current/` — `setlist` is `null` when no setlist is dated today or later. */
@Serializable
data class CurrentSetlistDto(
    @SerialName("setlist") val setlist: SetlistDto? = null,
)
