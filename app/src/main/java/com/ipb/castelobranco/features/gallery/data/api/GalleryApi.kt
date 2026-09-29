package com.ipb.castelobranco.features.gallery.data.api

import com.ipb.castelobranco.features.gallery.data.dto.GalleryChangesDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface GalleryApi {

    /** Without [since], every live album and photo; with it, what changed since shortly before it. */
    @GET(GalleryEndpoints.CHANGES)
    suspend fun getChanges(
        @Query(GalleryEndpoints.SINCE) since: String?,
    ): Response<GalleryChangesDto>

    @Streaming
    @GET
    suspend fun downloadFile(
        @Url absoluteUrl: String
    ): Response<ResponseBody>
}
