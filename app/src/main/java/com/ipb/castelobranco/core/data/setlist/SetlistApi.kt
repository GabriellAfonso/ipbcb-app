package com.ipb.castelobranco.core.data.setlist

import com.ipb.castelobranco.core.network.ApiConstants
import retrofit2.Response
import retrofit2.http.GET

object SetlistEndpoints {
    const val SETLISTS_PATH = "${ApiConstants.BASE_PATH}setlists/"
    const val CURRENT_PATH = "${SETLISTS_PATH}current/"
}

interface SetlistApi {

    @GET(SetlistEndpoints.CURRENT_PATH)
    suspend fun getCurrent(): Response<CurrentSetlistDto>
}
