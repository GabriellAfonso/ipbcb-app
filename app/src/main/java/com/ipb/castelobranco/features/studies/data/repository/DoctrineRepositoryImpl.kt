package com.ipb.castelobranco.features.studies.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.BaseSnapshotRepository
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.studies.data.dto.DoctrineDocumentDto
import com.ipb.castelobranco.features.studies.data.mapper.toDomain
import com.ipb.castelobranco.features.studies.domain.model.DoctrineDocument
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSummary
import com.ipb.castelobranco.features.studies.domain.repository.DoctrineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoctrineRepositoryImpl @Inject constructor(
    private val sources: DoctrineSnapshotSources,
    private val logger: Logger,
) : DoctrineRepository {

    private val list = Snapshot(
        cache = sources.listCache(),
        fetcher = sources.listFetcher(),
        mapper = { it.toDomain() },
        logger = logger,
        tag = "DoctrineListSnapshot",
    )

    private val documents = mutableMapOf<String, Snapshot<DoctrineDocumentDto, DoctrineDocument?>>()

    override fun observeList(): Flow<SnapshotState<List<DoctrineSummary>>> = list.observe()

    override suspend fun loadList(): RefreshResult = list.load()

    override suspend fun refreshList(): RefreshResult = list.refresh()

    override fun observeDocument(slug: String): Flow<SnapshotState<DoctrineDocument>> =
        document(slug).observe().map { state ->
            when (state) {
                is SnapshotState.Data -> state.value?.let { SnapshotState.Data(it) }
                    ?: SnapshotState.Error(AppError.Unknown(message = "Unknown doctrine kind for '$slug'"))
                is SnapshotState.Error -> state
                SnapshotState.Loading -> SnapshotState.Loading
            }
        }

    override suspend fun loadDocument(slug: String): RefreshResult = document(slug).load()

    override suspend fun refreshDocument(slug: String): RefreshResult = document(slug).refresh()

    private fun document(slug: String): Snapshot<DoctrineDocumentDto, DoctrineDocument?> =
        synchronized(documents) {
            documents.getOrPut(slug) {
                Snapshot(
                    cache = sources.documentCache(slug),
                    fetcher = sources.documentFetcher(slug),
                    mapper = { it.toDomain() },
                    logger = logger,
                    tag = "DoctrineDocumentSnapshot",
                )
            }
        }

    private class Snapshot<Dto, Domain>(
        cache: SnapshotCache<Dto>,
        fetcher: SnapshotFetcher<Dto>,
        mapper: (Dto) -> Domain,
        logger: Logger,
        tag: String,
    ) : BaseSnapshotRepository<Dto, Domain>(cache, fetcher, mapper, logger, tag) {

        /** Saved copy on screen first, then the ETag check (spec FR-021, FR-022). */
        suspend fun load(): RefreshResult {
            preload()
            return refresh()
        }
    }
}
