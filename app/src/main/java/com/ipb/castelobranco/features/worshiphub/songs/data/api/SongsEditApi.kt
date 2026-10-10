package com.ipb.castelobranco.features.worshiphub.songs.data.api

import com.ipb.castelobranco.features.worshiphub.tables.data.api.SongsTableEndpoint
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.AllSongDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.Path

interface SongsEditApi {

    @PATCH("${SongsTableEndpoint.ALL_SONGS_PATH}{id}/")
    suspend fun update(
        @Path("id") id: Int,
        @Body body: UpdateSongBody,
    ): Response<AllSongDto>

    @DELETE("${SongsTableEndpoint.ALL_SONGS_PATH}{id}/")
    suspend fun delete(@Path("id") id: Int): Response<Unit>
}

@Serializable
data class UpdateSongBody(
    @SerialName("title") val title: String,
    @SerialName("artist") val artist: String,
    @SerialName("youtube_link") val youtubeLink: String,
)
