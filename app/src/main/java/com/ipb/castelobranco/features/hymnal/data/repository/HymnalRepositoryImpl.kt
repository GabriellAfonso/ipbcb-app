package com.ipb.castelobranco.features.hymnal.data.repository

import com.ipb.castelobranco.core.domain.model.HymnCatalogEntry
import com.ipb.castelobranco.core.domain.snapshot.BaseSnapshotRepository
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.hymnal.data.dto.HymnDto
import com.ipb.castelobranco.features.hymnal.data.mapper.toDomain
import com.ipb.castelobranco.features.hymnal.domain.model.Hymn
import com.ipb.castelobranco.features.hymnal.domain.repository.HymnalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HymnalRepositoryImpl @Inject constructor(
    cache: SnapshotCache<List<HymnDto>>,
    fetcher: SnapshotFetcher<List<HymnDto>>,
    logger: Logger,
) : BaseSnapshotRepository<List<HymnDto>, List<Hymn>>(
    cache = cache,
    fetcher = fetcher,
    mapper = { it.toDomain() },
    logger = logger,
    tag = "HymnalSnapshot"
), HymnalRepository {

    override fun observeHymnal(): Flow<SnapshotState<List<Hymn>>> = observe()

    override suspend fun refreshHymnal(): RefreshResult = refresh()

    /**
     * The same snapshot, narrowed to number and title for callers outside this feature. No second
     * cache and no second fetch — it maps whatever [observeHymnal] is already emitting.
     */
    override fun observeHymnCatalog(): Flow<SnapshotState<List<HymnCatalogEntry>>> =
        observe().map { state ->
            val mapped: SnapshotState<List<HymnCatalogEntry>> = when (state) {
                is SnapshotState.Data -> SnapshotState.Data(
                    state.value.map { HymnCatalogEntry(number = it.number, title = it.title) }
                )

                is SnapshotState.Error -> SnapshotState.Error(state.error)
                SnapshotState.Loading -> SnapshotState.Loading
            }
            mapped
        }

    // preload() is inherited from BaseSnapshotRepository
}
