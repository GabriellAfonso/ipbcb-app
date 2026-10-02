package com.ipb.castelobranco.features.admin.register.data.api

import com.ipb.castelobranco.core.data.setlist.SetlistDto
import com.ipb.castelobranco.core.data.setlist.SetlistEndpoints
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/** Setlist reads for whoever registers the played songs (`manage` on `songs`). */
interface SetlistAdminApi {

    @GET("${SetlistEndpoints.SETLISTS_PATH}{date}/")
    suspend fun byDate(@Path("date") date: String): Response<SetlistDto>

    /** Declared before `{date}/` on the server; newest first. */
    @GET("${SetlistEndpoints.SETLISTS_PATH}pending-confirmation/")
    suspend fun pending(): Response<List<SetlistDto>>
}
