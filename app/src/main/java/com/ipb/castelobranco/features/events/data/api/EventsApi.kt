package com.ipb.castelobranco.features.events.data.api

import com.ipb.castelobranco.features.events.data.dto.EventPublicDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * The public list works signed out, yet the interface comes from `@AuthedRetrofit`: a member's token
 * must travel when there is one, so the answer includes the members-only events.
 */
interface EventsApi {
    @GET(EventsEndpoints.EVENTS)
    suspend fun getEvents(
        @Header(EventsEndpoints.IF_NONE_MATCH) ifNoneMatch: String? = null,
    ): Response<List<EventPublicDto>>
}
