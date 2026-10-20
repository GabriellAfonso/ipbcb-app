package com.ipb.castelobranco.features.studies.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSummary
import com.ipb.castelobranco.features.studies.domain.repository.DoctrineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDoctrineListUseCase @Inject constructor(
    private val repository: DoctrineRepository,
) {
    operator fun invoke(): Flow<SnapshotState<List<DoctrineSummary>>> = repository.observeList()

    suspend fun load(): RefreshResult = repository.loadList()

    suspend fun refresh(): RefreshResult = repository.refreshList()
}
