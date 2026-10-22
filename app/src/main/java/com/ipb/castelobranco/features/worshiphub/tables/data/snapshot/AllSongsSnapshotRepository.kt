package com.ipb.castelobranco.features.worshiphub.tables.data.snapshot

import com.ipb.castelobranco.core.domain.snapshot.BaseSnapshotRepository
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.AllSongDto
import com.ipb.castelobranco.features.worshiphub.tables.data.mapper.toDomain
import com.ipb.castelobranco.core.domain.model.Song
import javax.inject.Inject

class AllSongsSnapshotRepository @Inject constructor(
    cache: SnapshotCache<List<AllSongDto>>,
    fetcher: SnapshotFetcher<List<AllSongDto>>,
    logger: Logger,
) : BaseSnapshotRepository<List<AllSongDto>, List<Song>>(
    cache = cache,
    fetcher = fetcher,
    mapper = { it.toDomain() },
    logger = logger,
    tag = "AllSongsSnapshot"
)
