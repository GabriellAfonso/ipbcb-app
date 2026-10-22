package com.ipb.castelobranco.features.worshiphub.chordcharts.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.model.ChordChart
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.repository.ChordChartRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChordChartsUseCase @Inject constructor(
    private val repository: ChordChartRepository,
) {
    fun observe(): Flow<SnapshotState<List<ChordChart>>> = repository.observe()
    suspend fun refresh(): RefreshResult = repository.refresh()
}
