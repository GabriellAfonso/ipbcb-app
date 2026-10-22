package com.ipb.castelobranco.features.worshiphub.lyrics.data.mapper

import com.ipb.castelobranco.features.worshiphub.lyrics.data.dto.LyricsDto
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.model.Lyrics

fun LyricsDto.toDomain(): Lyrics = Lyrics(
    id      = id,
    songId  = songId,
    content = content,
)

fun List<LyricsDto>.toDomain(): List<Lyrics> = map { it.toDomain() }
