package com.ipb.castelobranco.core.data.api

import com.ipb.castelobranco.core.data.dto.BirthdaysResponseDto
import com.ipb.castelobranco.core.network.ApiConstants
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MembersApi {

    /** [month] is a single month (`7`) or an inclusive range (`1-12`). */
    @GET(MembersEndpoints.BIRTHDAYS)
    suspend fun getBirthdays(
        @Query("month") month: String,
        @Header("If-None-Match") ifNoneMatch: String? = null,
    ): Response<BirthdaysResponseDto>
}

object MembersEndpoints {
    const val BIRTHDAYS = "${ApiConstants.BASE_PATH}members/birthdays/"
    const val WHOLE_YEAR = "1-12"
}
