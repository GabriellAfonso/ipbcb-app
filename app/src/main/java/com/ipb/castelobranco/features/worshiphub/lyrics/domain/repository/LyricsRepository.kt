package com.ipb.castelobranco.features.worshiphub.lyrics.domain.repository

import com.ipb.castelobranco.features.worshiphub.lyrics.domain.model.Lyrics
import com.ipb.castelobranco.features.worshiphub.shared.domain.repository.SongContentRepository

interface LyricsRepository : SongContentRepository<Lyrics> {
    suspend fun createLyrics(songId: Int, content: String): Result<Unit>
    suspend fun updateContent(id: Int, content: String): Result<Unit>
}
