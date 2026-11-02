package com.ipb.castelobranco.features.worshiphub.chordcharts.data.api

import com.ipb.castelobranco.core.network.CreateChordChartRequest
import com.ipb.castelobranco.core.network.UpdateChordChartRequest
import com.ipb.castelobranco.features.worshiphub.chordcharts.data.dto.ChordChartDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ChordChartsEditApi {

    @POST(ChordChartsEndpoint.CHORD_CHARTS_PATH)
    suspend fun createChordChart(@Body body: CreateChordChartRequest): Response<ChordChartDto>

    @PATCH("${ChordChartsEndpoint.CHORD_CHARTS_PATH}{id}/")
    suspend fun updateChordChart(
        @Path("id") id: Int,
        @Body body: UpdateChordChartRequest,
    ): Response<ChordChartDto>
}
