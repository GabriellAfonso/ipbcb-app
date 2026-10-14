package com.ipb.castelobranco.features.worshiphub.tables.data.api

import com.ipb.castelobranco.features.worshiphub.tables.data.dto.AllSongDto
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.SongsBySundayDto
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.SuggestedSongDto
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.TopSongDto
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.TopToneDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SongsTableApi {

    @GET(SongsTableEndpoint.ALL_SONGS_PATH)
    suspend fun getAllSongs(
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<List<AllSongDto>>


    @GET(SongsTableEndpoint.SONGS_BY_SUNDAY_PATH)
    suspend fun getSongsBySunday(
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<List<SongsBySundayDto>>

    @GET(SongsTableEndpoint.TOP_SONGS_PATH)
    suspend fun getTopSongs(
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<List<TopSongDto>>

    @GET(SongsTableEndpoint.TOP_TONES_PATH)
    suspend fun getTopTones(
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<List<TopToneDto>>

    @GET(SongsTableEndpoint.SUGGESTED_SONGS_PATH)
    suspend fun getSuggestedSongs(
        @Header("If-None-Match") ifNoneMatch: String? = null,
        @Query("fixed") fixed: String? = null,
        @Query("exclude") exclude: String? = null,
    ): Response<List<SuggestedSongDto>>


}