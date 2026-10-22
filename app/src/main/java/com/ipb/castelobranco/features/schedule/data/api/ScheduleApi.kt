package com.ipb.castelobranco.features.schedule.data.api

import com.ipb.castelobranco.features.schedule.data.dto.MonthScheduleDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface ScheduleApi {
    @GET(ScheduleEndpoints.CURRENT_SCHEDULE)
    suspend fun getMonthSchedule(
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<MonthScheduleDto>
}