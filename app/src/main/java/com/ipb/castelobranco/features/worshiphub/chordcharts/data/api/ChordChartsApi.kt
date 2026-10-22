package com.ipb.castelobranco.features.worshiphub.chordcharts.data.api

import com.ipb.castelobranco.features.worshiphub.chordcharts.data.dto.ChordChartDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface ChordChartsApi {

    @GET(ChordChartsEndpoint.CHORD_CHARTS_PATH)
    suspend fun getChordCharts(
        @Header("If-None-Match") ifNoneMatch: String? = null,
    ): Response<List<ChordChartDto>>
}
