package com.ipb.castelobranco.features.worshiphub.chordcharts.data.mapper

import com.ipb.castelobranco.features.worshiphub.chordcharts.data.dto.ChordChartDto
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.model.ChordChart

fun ChordChartDto.toDomain(): ChordChart = ChordChart(
    id = id,
    songId = songId,
    content = content,
    tone = tone,
    instrument = instrument,
)

fun List<ChordChartDto>.toDomain(): List<ChordChart> = map { it.toDomain() }
