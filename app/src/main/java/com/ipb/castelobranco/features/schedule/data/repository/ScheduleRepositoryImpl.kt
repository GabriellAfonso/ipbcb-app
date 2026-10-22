package com.ipb.castelobranco.features.schedule.data.repository

import com.ipb.castelobranco.core.domain.snapshot.BaseSnapshotRepository
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.schedule.data.dto.MonthScheduleDto
import com.ipb.castelobranco.features.schedule.data.mapper.toDomain
import com.ipb.castelobranco.features.schedule.domain.model.MonthSchedule
import com.ipb.castelobranco.features.schedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    cache: SnapshotCache<MonthScheduleDto>,
    fetcher: SnapshotFetcher<MonthScheduleDto>,
    logger: Logger,
) : BaseSnapshotRepository<MonthScheduleDto, MonthSchedule>(
    cache = cache,
    fetcher = fetcher,
    mapper = { it.toDomain() },
    logger = logger,
    tag = "ScheduleSnapshot"
), ScheduleRepository {

    override fun observeMonthSchedule(): Flow<SnapshotState<MonthSchedule>> = observe()

    override fun getCurrentSnapshot(): SnapshotState<MonthSchedule> = getCurrentState()

    // preload() is inherited from BaseSnapshotRepository

    override suspend fun refreshMonthSchedule(): RefreshResult = refresh()

    override suspend fun clearScheduleCache() {
        clearCache()
        emitError(AppError.Auth(code = 401, userMessage = "Faça login para ver a escala"))
    }
}
