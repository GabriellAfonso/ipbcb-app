package com.ipb.castelobranco.features.bible.data.api

import com.ipb.castelobranco.features.bible.data.dto.BibleProgressDto
import com.ipb.castelobranco.features.bible.data.dto.ProgressSyncRequestDto
import com.ipb.castelobranco.features.bible.data.dto.ProgressSyncResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** The signed-in user's reading and watched progress: authed client. */
interface BibleProgressApi {
    @GET(BibleEndpoints.PROGRESS)
    suspend fun getProgress(): Response<BibleProgressDto>

    @POST(BibleEndpoints.PROGRESS_SYNC)
    suspend fun sync(@Body body: ProgressSyncRequestDto): Response<ProgressSyncResponseDto>
}
