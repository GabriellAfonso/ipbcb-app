package com.ipb.castelobranco.features.worshiphub.chordcharts.domain.repository

import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.model.ChordChart
import com.ipb.castelobranco.features.worshiphub.shared.domain.repository.SongContentRepository

interface ChordChartRepository : SongContentRepository<ChordChart> {
    suspend fun createChordChart(songId: Int, content: String, tone: String, instrument: String): Result<Unit>
    suspend fun updateChordChart(id: Int, content: String, tone: String, instrument: String): Result<Unit>
}
