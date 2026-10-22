package com.ipb.castelobranco.features.worshiphub.tables.data.snapshot

import com.ipb.castelobranco.core.domain.snapshot.BaseSnapshotRepository
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.TopToneDto
import com.ipb.castelobranco.features.worshiphub.tables.data.mapper.toDomain
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopTone
import javax.inject.Inject

class TopTonesSnapshotRepository @Inject constructor(
    cache: SnapshotCache<List<TopToneDto>>,
    fetcher: SnapshotFetcher<List<TopToneDto>>,
    logger: Logger,
) : BaseSnapshotRepository<List<TopToneDto>, List<TopTone>>(
    cache = cache,
    fetcher = fetcher,
    mapper = { it.toDomain() },
    logger = logger,
    tag = "TopTonesSnapshot"
)
