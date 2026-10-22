package com.ipb.castelobranco.features.worshiphub.chordcharts.data.repository

import com.ipb.castelobranco.core.domain.snapshot.BaseSnapshotRepository
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.network.CreateChordChartRequest
import com.ipb.castelobranco.core.network.UpdateContentRequest
import com.ipb.castelobranco.features.worshiphub.chordcharts.data.api.ChordChartsEditApi
import com.ipb.castelobranco.features.worshiphub.chordcharts.data.dto.ChordChartDto
import com.ipb.castelobranco.features.worshiphub.chordcharts.data.mapper.toDomain
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.model.ChordChart
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.repository.ChordChartRepository
import com.ipb.castelobranco.core.network.error.toAppError
import javax.inject.Inject

class ChordChartRepositoryImpl @Inject constructor(
    cache: SnapshotCache<List<ChordChartDto>>,
    fetcher: SnapshotFetcher<List<ChordChartDto>>,
    logger: Logger,
    private val editApi: ChordChartsEditApi,
) : ChordChartRepository,
    BaseSnapshotRepository<List<ChordChartDto>, List<ChordChart>>(
        cache = cache,
        fetcher = fetcher,
        mapper = { dtos -> dtos.toDomain() },
        logger = logger,
        tag = "ChordChartsSnapshot",
    ) {

    override suspend fun createChordChart(
        songId: Int,
        content: String,
        tone: String,
        instrument: String,
    ): Result<Unit> = runCatching {
        val response = editApi.createChordChart(CreateChordChartRequest(songId, content, tone, instrument))
        if (!response.isSuccessful) throw response.toAppError()
        refresh()
    }

    override suspend fun updateContent(id: Int, content: String): Result<Unit> = runCatching {
        val response = editApi.updateContent(id, UpdateContentRequest(content))
        if (!response.isSuccessful) throw response.toAppError()
        refresh()
    }
}
