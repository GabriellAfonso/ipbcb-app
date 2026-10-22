package com.ipb.castelobranco.features.worshiphub.lyrics.data.repository

import com.ipb.castelobranco.core.domain.snapshot.BaseSnapshotRepository
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.network.CreateLyricsRequest
import com.ipb.castelobranco.core.network.UpdateContentRequest
import com.ipb.castelobranco.features.worshiphub.lyrics.data.api.LyricsEditApi
import com.ipb.castelobranco.features.worshiphub.lyrics.data.dto.LyricsDto
import com.ipb.castelobranco.features.worshiphub.lyrics.data.mapper.toDomain
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.model.Lyrics
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.repository.LyricsRepository
import com.ipb.castelobranco.core.network.error.toAppError
import javax.inject.Inject

class LyricsRepositoryImpl @Inject constructor(
    cache: SnapshotCache<List<LyricsDto>>,
    fetcher: SnapshotFetcher<List<LyricsDto>>,
    logger: Logger,
    private val editApi: LyricsEditApi,
) : LyricsRepository,
    BaseSnapshotRepository<List<LyricsDto>, List<Lyrics>>(
        cache   = cache,
        fetcher = fetcher,
        mapper  = { dtos -> dtos.toDomain() },
        logger  = logger,
        tag     = "LyricsSnapshot",
    ) {

    override suspend fun createLyrics(songId: Int, content: String): Result<Unit> = runCatching {
        val response = editApi.createLyrics(CreateLyricsRequest(songId, content))
        if (!response.isSuccessful) throw response.toAppError()
        refresh()
    }

    override suspend fun updateContent(id: Int, content: String): Result<Unit> = runCatching {
        val response = editApi.updateContent(id, UpdateContentRequest(content))
        if (!response.isSuccessful) throw response.toAppError()
        refresh()
    }
}
