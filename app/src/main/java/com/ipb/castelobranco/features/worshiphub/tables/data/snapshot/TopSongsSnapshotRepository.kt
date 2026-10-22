package com.ipb.castelobranco.features.worshiphub.tables.data.snapshot

import com.ipb.castelobranco.core.domain.snapshot.BaseSnapshotRepository
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.TopSongDto
import com.ipb.castelobranco.features.worshiphub.tables.data.mapper.toDomain
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopSong
import javax.inject.Inject

class TopSongsSnapshotRepository @Inject constructor(
    cache: SnapshotCache<List<TopSongDto>>,
    fetcher: SnapshotFetcher<List<TopSongDto>>,
    logger: Logger,
) : BaseSnapshotRepository<List<TopSongDto>, List<TopSong>>(
    cache = cache,
    fetcher = fetcher,
    mapper = { it.toDomain() },
    logger = logger,
    tag = "TopSongsSnapshot"
)
