package com.ipb.castelobranco.features.studies.presentation.viewmodel

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.studies.domain.model.DoctrineDocument
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSummary
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository
import com.ipb.castelobranco.features.studies.domain.repository.DoctrineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDoctrineRepository : DoctrineRepository {
    val list = MutableStateFlow<SnapshotState<List<DoctrineSummary>>>(SnapshotState.Loading)
    val documents = mutableMapOf<String, MutableStateFlow<SnapshotState<DoctrineDocument>>>()
    var listLoads = 0
    var listRefreshes = 0
    var documentLoads = 0

    fun document(slug: String) = documents.getOrPut(slug) { MutableStateFlow(SnapshotState.Loading) }

    override fun observeList(): Flow<SnapshotState<List<DoctrineSummary>>> = list
    override suspend fun loadList(): RefreshResult = RefreshResult.NotModified.also { listLoads++ }
    override suspend fun refreshList(): RefreshResult = RefreshResult.NotModified.also { listRefreshes++ }

    override fun observeDocument(slug: String): Flow<SnapshotState<DoctrineDocument>> = document(slug)
    override suspend fun loadDocument(slug: String): RefreshResult = RefreshResult.NotModified.also { documentLoads++ }
    override suspend fun refreshDocument(slug: String): RefreshResult = RefreshResult.NotModified
}

class FakeDoctrinePreferences(initial: Float = DoctrinePreferencesRepository.DEFAULT_FONT_SIZE) :
    DoctrinePreferencesRepository {
    override val fontSizeFlow = MutableStateFlow(initial)
    override suspend fun setFontSize(size: Float) {
        fontSizeFlow.value = size
    }
}
