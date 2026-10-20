package com.ipb.castelobranco.features.studies.domain.repository

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.studies.domain.model.DoctrineDocument
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSummary
import kotlinx.coroutines.flow.Flow

/**
 * Westminster documents, kept on the device after the first load and revalidated by ETag.
 * `load*` shows the saved copy first, then asks the server whether it changed.
 */
interface DoctrineRepository {
    fun observeList(): Flow<SnapshotState<List<DoctrineSummary>>>
    suspend fun loadList(): RefreshResult
    suspend fun refreshList(): RefreshResult

    fun observeDocument(slug: String): Flow<SnapshotState<DoctrineDocument>>
    suspend fun loadDocument(slug: String): RefreshResult
    suspend fun refreshDocument(slug: String): RefreshResult
}
