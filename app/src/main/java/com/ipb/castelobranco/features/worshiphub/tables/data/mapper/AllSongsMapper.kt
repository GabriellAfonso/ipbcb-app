package com.ipb.castelobranco.features.worshiphub.tables.data.mapper

import com.ipb.castelobranco.features.worshiphub.tables.data.dto.AllSongDto
import com.ipb.castelobranco.core.domain.model.Song

fun AllSongDto.toDomain(): Song =
    Song(
        id = id,
        title = title,
        artist = artist,
        categoryName = categoryName,
        youtubeLink = youtubeLink,
    )

fun List<AllSongDto>.toDomain(): List<Song> = map { it.toDomain() }
