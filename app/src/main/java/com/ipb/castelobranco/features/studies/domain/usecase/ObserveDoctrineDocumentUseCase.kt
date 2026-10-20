package com.ipb.castelobranco.features.studies.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.studies.domain.model.DoctrineDocument
import com.ipb.castelobranco.features.studies.domain.repository.DoctrineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDoctrineDocumentUseCase @Inject constructor(
    private val repository: DoctrineRepository,
) {
    operator fun invoke(slug: String): Flow<SnapshotState<DoctrineDocument>> = repository.observeDocument(slug)

    suspend fun load(slug: String): RefreshResult = repository.loadDocument(slug)

    suspend fun refresh(slug: String): RefreshResult = repository.refreshDocument(slug)
}
