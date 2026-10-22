package com.ipb.castelobranco.features.hymnal.data.api

import com.ipb.castelobranco.features.hymnal.data.dto.HymnDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface HymnalApi {
    @GET(HymnalEndpoints.HYMNAL_PATH)
    suspend fun getHymnal(
        @Header("If-None-Match") ifNoneMatch: String?
    ): Response<List<HymnDto>>
}