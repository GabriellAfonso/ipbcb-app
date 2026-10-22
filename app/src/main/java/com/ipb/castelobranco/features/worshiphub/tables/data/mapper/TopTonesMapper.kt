package com.ipb.castelobranco.features.worshiphub.tables.data.mapper

import com.ipb.castelobranco.features.worshiphub.tables.data.dto.TopToneDto
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopTone

fun TopToneDto.toDomain(): TopTone = TopTone(tone = tone, count = count)

fun List<TopToneDto>.toDomain(): List<TopTone> = map { it.toDomain() }
