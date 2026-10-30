package com.ipb.castelobranco.features.bible.data.api

import com.ipb.castelobranco.features.bible.data.dto.BibleVideoDto
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Public list: read with the authless client, so signed-out users see the videos too. */
interface BibleVideosPublicApi {
    @GET(BibleEndpoints.VIDEOS)
    suspend fun getVideos(@Header("If-None-Match") ifNoneMatch: String?): Response<List<BibleVideoDto>>
}

/** Writes need the `bible_videos` scope: authed client. */
interface BibleVideosManageApi {
    @POST(BibleEndpoints.VIDEOS)
    suspend fun addVideo(@Body body: JsonObject): Response<BibleVideoDto>

    @PUT(BibleEndpoints.VIDEO_PLACES)
    suspend fun setPlaces(@Path(BibleEndpoints.ID) id: Long, @Body body: JsonObject): Response<Unit>

    @DELETE(BibleEndpoints.VIDEO_PLACES)
    suspend fun removePlace(
        @Path(BibleEndpoints.ID) id: Long,
        @Query(BibleEndpoints.BOOK) book: String,
        @Query(BibleEndpoints.CHAPTER) chapter: Int?,
    ): Response<Unit>

    @DELETE(BibleEndpoints.VIDEO)
    suspend fun deleteVideo(@Path(BibleEndpoints.ID) id: Long): Response<Unit>

    @PUT(BibleEndpoints.VIDEOS_ORDER)
    suspend fun setOrder(@Body body: JsonObject): Response<Unit>
}
