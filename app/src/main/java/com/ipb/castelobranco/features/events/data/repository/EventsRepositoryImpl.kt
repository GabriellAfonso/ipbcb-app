package com.ipb.castelobranco.features.events.data.repository

import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.domain.snapshot.BaseSnapshotRepository
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.events.data.dto.EventPublicDto
import com.ipb.castelobranco.features.events.data.mapper.toDomain
import com.ipb.castelobranco.features.events.domain.model.Event
import com.ipb.castelobranco.features.events.domain.repository.EventsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The home asks for the list on boot, on every resume and on every login change, so two of those can
 * overlap: the second one is skipped instead of fetching the same list twice.
 */
@Singleton
class EventsRepositoryImpl @Inject constructor(
    cache: SnapshotCache<List<EventPublicDto>>,
    fetcher: SnapshotFetcher<List<EventPublicDto>>,
    logger: Logger,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseSnapshotRepository<List<EventPublicDto>, List<Event>>(
    cache = cache,
    fetcher = fetcher,
    mapper = { it.toDomain() },
    logger = logger,
    tag = "EventsSnapshot",
    ioDispatcher = ioDispatcher,
), EventsRepository {

    private val refreshing = Mutex()

    override fun observeEvents(): Flow<SnapshotState<List<Event>>> = observe()

    // preload() is inherited from BaseSnapshotRepository

    override suspend fun refreshEvents(): RefreshResult {
        if (!refreshing.tryLock()) return RefreshResult.NotModified
        return try {
            refresh()
        } finally {
            refreshing.unlock()
        }
    }

    override suspend fun clear() = clearCache()
}
