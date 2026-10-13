package com.ipb.castelobranco.core.data.repository

import com.ipb.castelobranco.core.data.dto.BirthdaysResponseDto
import com.ipb.castelobranco.core.data.mapper.toDomain
import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.domain.repository.MembersRepository
import com.ipb.castelobranco.core.domain.snapshot.BaseSnapshotRepository
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MembersRepositoryImpl @Inject constructor(
    cache: SnapshotCache<BirthdaysResponseDto>,
    fetcher: SnapshotFetcher<BirthdaysResponseDto>,
    logger: Logger,
) : BaseSnapshotRepository<BirthdaysResponseDto, List<Birthday>>(
    cache = cache,
    fetcher = fetcher,
    mapper = { it.toDomain() },
    logger = logger,
    tag = "BirthdaySnapshot"
), MembersRepository {

    override fun observeBirthdays(): Flow<SnapshotState<List<Birthday>>> = observe()
    override fun getCurrentSnapshot(): SnapshotState<List<Birthday>> = getCurrentState()
    override suspend fun loadCurrentSnapshot(): SnapshotState<List<Birthday>> = loadCurrentState()
    override suspend fun refreshBirthdays(): RefreshResult = refresh()
    override suspend fun clearBirthdaysCache() {
        clearCache()
    }
}
