package com.ipb.castelobranco.features.admin.events.data.api

import com.ipb.castelobranco.features.admin.events.data.dto.EventListDto
import com.ipb.castelobranco.features.admin.events.data.dto.EventRecordDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Every call needs a level on `events`, so the interface comes from `@AuthedRetrofit`. Writes are
 * multipart only — the server answers 415 to a JSON body — with the text fields as parts.
 */
interface EventsAdminApi {

    @GET(EventsAdminEndpoints.EVENTS_PATH)
    suspend fun getEvents(@Query(EventsAdminEndpoints.STATUS) status: String): Response<EventListDto>

    @GET(EventsAdminEndpoints.EVENT_PATH)
    suspend fun getEvent(@Path(EventsAdminEndpoints.ID) id: Int): Response<EventRecordDto>

    @Multipart
    @POST(EventsAdminEndpoints.EVENTS_PATH)
    suspend fun createEvent(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part,
    ): Response<EventRecordDto>

    @Multipart
    @PATCH(EventsAdminEndpoints.EVENT_PATH)
    suspend fun updateEvent(
        @Path(EventsAdminEndpoints.ID) id: Int,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?,
    ): Response<EventRecordDto>

    @DELETE(EventsAdminEndpoints.EVENT_PATH)
    suspend fun deleteEvent(@Path(EventsAdminEndpoints.ID) id: Int): Response<Unit>
}
