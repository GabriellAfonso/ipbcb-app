package com.ipb.castelobranco.features.worshiphub.lyrics.data.api

import com.ipb.castelobranco.features.worshiphub.lyrics.data.dto.LyricsDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface LyricsApi {

    @GET(LyricsEndpoint.LYRICS_PATH)
    suspend fun getLyrics(
        @Header("If-None-Match") ifNoneMatch: String? = null,
    ): Response<List<LyricsDto>>
}
