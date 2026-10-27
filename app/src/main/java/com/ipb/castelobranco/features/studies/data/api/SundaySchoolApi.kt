package com.ipb.castelobranco.features.studies.data.api

import com.ipb.castelobranco.features.studies.data.dto.LessonDto
import com.ipb.castelobranco.features.studies.data.dto.SundaySchoolIndexDto
import com.ipb.castelobranco.features.studies.data.dto.SundaySchoolYearAdminDto
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Public, read-only; both endpoints answer 304 to a matching `If-None-Match`. */
interface SundaySchoolApi {
    @GET(SundaySchoolEndpoints.INDEX_PATH)
    suspend fun getIndex(
        @Query(SundaySchoolEndpoints.QUERY_YEAR) year: Int?,
        @Header("If-None-Match") ifNoneMatch: String?,
    ): Response<SundaySchoolIndexDto>

    @GET(SundaySchoolEndpoints.LESSON_PATH)
    suspend fun getLesson(
        @Path("id") id: Int,
        @Header("If-None-Match") ifNoneMatch: String?,
    ): Response<LessonDto>
}

/** Panel, scope `sunday_school`. Bodies are built as JSON so a cleared `date` is sent as `null`. */
interface SundaySchoolAdminApi {
    @GET(SundaySchoolEndpoints.MANAGE_LESSONS_PATH)
    suspend fun getLessons(@Query(SundaySchoolEndpoints.QUERY_YEAR) year: Int): Response<List<LessonDto>>

    @GET(SundaySchoolEndpoints.MANAGE_LESSON_PATH)
    suspend fun getLesson(@Path("id") id: Int): Response<LessonDto>

    @POST(SundaySchoolEndpoints.MANAGE_LESSONS_PATH)
    suspend fun createLesson(@Body body: JsonObject): Response<LessonDto>

    @PATCH(SundaySchoolEndpoints.MANAGE_LESSON_PATH)
    suspend fun updateLesson(@Path("id") id: Int, @Body body: JsonObject): Response<LessonDto>

    @DELETE(SundaySchoolEndpoints.MANAGE_LESSON_PATH)
    suspend fun deleteLesson(@Path("id") id: Int): Response<Unit>

    @PUT(SundaySchoolEndpoints.MANAGE_ORDER_PATH)
    suspend fun reorder(@Body body: JsonObject): Response<Unit>

    @GET(SundaySchoolEndpoints.MANAGE_YEARS_PATH)
    suspend fun getYears(): Response<List<SundaySchoolYearAdminDto>>

    @PATCH(SundaySchoolEndpoints.MANAGE_YEAR_PATH)
    suspend fun updateYear(@Path("year") year: Int, @Body body: JsonObject): Response<SundaySchoolYearAdminDto>
}
