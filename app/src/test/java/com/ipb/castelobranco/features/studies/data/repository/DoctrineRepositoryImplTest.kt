package com.ipb.castelobranco.features.studies.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.studies.DoctrineTestFixtures
import com.ipb.castelobranco.features.studies.data.dto.DoctrineDocumentDto
import com.ipb.castelobranco.features.studies.data.dto.DoctrineListDto
import com.ipb.castelobranco.features.studies.data.dto.DoctrineSummaryDto
import com.ipb.castelobranco.features.studies.domain.model.DoctrineDocument
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DoctrineRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val catechismDto = json.decodeFromString<DoctrineDocumentDto>(DoctrineTestFixtures.catechismJson)
    private val confessionDto = json.decodeFromString<DoctrineDocumentDto>(DoctrineTestFixtures.confessionJson)

    private val sources = FakeSources()
    private val repository = DoctrineRepositoryImpl(sources, Logger.Noop)

    @Test
    fun `load shows the saved copy when the server says not modified`() = runTest {
        sources.documentCaches.getValue(SHORTER).stored = catechismDto
        sources.documentFetchers.getValue(SHORTER).result = NetworkResult.NotModified

        val result = repository.loadDocument(SHORTER)

        assertEquals(RefreshResult.NotModified, result)
        val state = repository.observeDocument(SHORTER).first()
        assertTrue((state as SnapshotState.Data).value is DoctrineDocument.Catechism)
    }

    @Test
    fun `revalidation sends the saved ETag and replaces the copy when changed`() = runTest {
        val cache = sources.documentCaches.getValue(SHORTER)
        cache.stored = catechismDto.copy(title = "Antigo")
        cache.etag = "\"v1\""
        val fetcher = sources.documentFetchers.getValue(SHORTER)
        fetcher.result = NetworkResult.Success(catechismDto, "\"v2\"")

        assertEquals(RefreshResult.Updated, repository.loadDocument(SHORTER))

        assertEquals("\"v1\"", fetcher.lastEtag)
        assertEquals("\"v2\"", cache.etag)
        val state = repository.observeDocument(SHORTER).first() as SnapshotState.Data
        assertEquals("Breve Catecismo de Westminster", state.value.title)
    }

    @Test
    fun `network failure keeps the saved copy on screen`() = runTest {
        sources.documentCaches.getValue(SHORTER).stored = catechismDto
        sources.documentFetchers.getValue(SHORTER).result = NetworkResult.Failure(AppError.Network(cause = IOException()))

        assertEquals(RefreshResult.CacheUsed, repository.loadDocument(SHORTER))
        assertTrue(repository.observeDocument(SHORTER).first() is SnapshotState.Data)
    }

    @Test
    fun `network failure without a saved copy is an error`() = runTest {
        sources.documentFetchers.getValue(SHORTER).result = NetworkResult.Failure(AppError.Network(cause = IOException()))

        assertTrue(repository.loadDocument(SHORTER) is RefreshResult.Error)
        val state = repository.observeDocument(SHORTER).first()
        assertTrue((state as SnapshotState.Error).error is AppError.Network)
    }

    @Test
    fun `documents are cached separately per slug`() = runTest {
        sources.documentFetchers.getValue(SHORTER).result = NetworkResult.Success(catechismDto, "a")
        sources.documentFetchers.getValue(CONFESSION).result = NetworkResult.Success(confessionDto, "b")

        repository.loadDocument(SHORTER)
        repository.loadDocument(CONFESSION)

        assertTrue((repository.observeDocument(SHORTER).first() as SnapshotState.Data).value is DoctrineDocument.Catechism)
        assertTrue(
            (repository.observeDocument(CONFESSION).first() as SnapshotState.Data).value is DoctrineDocument.Confession,
        )
        assertEquals(catechismDto, sources.documentCaches.getValue(SHORTER).stored)
        assertEquals(confessionDto, sources.documentCaches.getValue(CONFESSION).stored)
    }

    @Test
    fun `unknown document kind becomes an error state`() = runTest {
        sources.documentFetchers.getValue(SHORTER).result =
            NetworkResult.Success(catechismDto.copy(kind = "devotional"), null)

        repository.loadDocument(SHORTER)

        assertTrue(repository.observeDocument(SHORTER).first() is SnapshotState.Error)
    }

    @Test
    fun `list loads and filters unknown kinds`() = runTest {
        sources.list.result = NetworkResult.Success(
            DoctrineListDto(
                listOf(
                    DoctrineSummaryDto(SHORTER, "Breve", "catechism", 107),
                    DoctrineSummaryDto("x", "X", "devotional", 1),
                ),
            ),
            "l1",
        )

        assertEquals(RefreshResult.Updated, repository.loadList())

        val state = repository.observeList().first() as SnapshotState.Data
        assertEquals(listOf(SHORTER), state.value.map { it.slug })
    }

    private class FakeCache<T> : SnapshotCache<T> {
        var stored: T? = null
        var etag: String? = null
        override suspend fun load(): T? = stored
        override suspend fun loadETag(): String? = etag
        override suspend fun save(dto: T, etag: String?) {
            stored = dto
            this.etag = etag
        }
        override suspend fun clear() {
            stored = null
            etag = null
        }
    }

    private class FakeFetcher<T> : SnapshotFetcher<T> {
        var result: NetworkResult<T> = NetworkResult.NotModified
        var lastEtag: String? = null
        override suspend fun fetch(etag: String?): NetworkResult<T> {
            lastEtag = etag
            return result
        }
    }

    private class FakeSources : DoctrineSnapshotSources {
        val listCache = FakeCache<DoctrineListDto>()
        val list = FakeFetcher<DoctrineListDto>()
        val documentCaches = mutableMapOf<String, FakeCache<DoctrineDocumentDto>>()
            .also { map -> SLUGS.forEach { map[it] = FakeCache() } }
        val documentFetchers = mutableMapOf<String, FakeFetcher<DoctrineDocumentDto>>()
            .also { map -> SLUGS.forEach { map[it] = FakeFetcher() } }

        override fun listCache(): SnapshotCache<DoctrineListDto> = listCache
        override fun listFetcher(): SnapshotFetcher<DoctrineListDto> = list
        override fun documentCache(slug: String): SnapshotCache<DoctrineDocumentDto> = documentCaches.getValue(slug)
        override fun documentFetcher(slug: String): SnapshotFetcher<DoctrineDocumentDto> =
            documentFetchers.getValue(slug)
    }

    private companion object {
        const val SHORTER = DoctrineTestFixtures.SHORTER
        const val CONFESSION = DoctrineTestFixtures.CONFESSION
        val SLUGS = listOf(SHORTER, CONFESSION)
    }
}
