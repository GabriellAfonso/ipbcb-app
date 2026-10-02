package com.ipb.castelobranco.features.worshiphub.tables.data.api

import com.ipb.castelobranco.core.data.setlist.SetlistDto
import com.ipb.castelobranco.core.data.setlist.SetlistEndpoints
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

interface SetlistSaveApi {

    /** Creates or replaces the setlist of [date] (`YYYY-MM-DD`, a Sunday). */
    @PUT("${SetlistEndpoints.SETLISTS_PATH}{date}/")
    suspend fun save(
        @Path("date") date: String,
        @Body body: SaveSetlistBody,
    ): Response<SetlistDto>
}

@Serializable
data class SaveSetlistBody(
    @SerialName("items") val items: List<SaveSetlistItemBody>,
)

@Serializable
data class SaveSetlistItemBody(
    @SerialName("song_id") val songId: Int,
    @SerialName("position") val position: Int,
    @SerialName("tone") val tone: String,
)
