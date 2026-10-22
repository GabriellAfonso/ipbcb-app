package com.ipb.castelobranco.features.worshiphub.tables.data.mapper

import com.ipb.castelobranco.features.worshiphub.tables.data.dto.TopSongDto
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopSong

fun TopSongDto.toDomain(): TopSong = TopSong(title = title, songId = songId, playCount = playCount)

fun List<TopSongDto>.toDomain(): List<TopSong> = map { it.toDomain() }
