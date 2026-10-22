package com.ipb.castelobranco.features.hymnal.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.hymnal.domain.model.Hymn
import com.ipb.castelobranco.features.hymnal.domain.repository.HymnalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveHymnsUseCase @Inject constructor(
    private val repository: HymnalRepository,
) {
    operator fun invoke(): Flow<SnapshotState<List<Hymn>>> = repository.observeHymnal()
    suspend fun refresh(): RefreshResult = repository.refreshHymnal()
}
