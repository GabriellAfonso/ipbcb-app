package com.ipb.castelobranco.features.studies.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.studies.data.api.SundaySchoolEndpoints
import com.ipb.castelobranco.features.studies.data.dto.LessonDto
import com.ipb.castelobranco.features.studies.data.dto.LessonSummaryDto
import com.ipb.castelobranco.features.studies.data.dto.SundaySchoolIndexDto
import com.ipb.castelobranco.features.studies.data.dto.SundaySchoolYearDto
import com.ipb.castelobranco.features.studies.domain.repository.SundaySchoolRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SundaySchoolRepositoryImplTest {

    private val sources = FakeSources()
    private val repository = SundaySchoolRepositoryImpl(sources, Logger.Noop)

    private val year = SundaySchoolYearDto(2026, "O Drama das Escrituras")
    private val index = SundaySchoolIndexDto(
        years = listOf(year),
        defaultYear = 2026,
        year = year,
        lessons = listOf(LessonSummaryDto(id = 11, title = "Panorama de Daniel", date = "2026-08-16")),
    )
    private val lesson = LessonDto(id = 11, title = "Panorama de Daniel", status = "published", year = year)

    @Test
    fun `default index is cached apart from other years`() = runTest {
        sources.indexFetcher(null).result = NetworkResult.Success(index, "\"a\"")

        assertEquals(RefreshResult.Updated, repository.loadIndex(null))

        val state = repository.observeIndex(null).first() as SnapshotState.Data
        assertEquals("2026 — O Drama das Escrituras", state.value.year?.label)
        assertEquals(index, sources.indexCache(null).stored)
        assertNull(sources.indexCache(2026).stored)
    }

    @Test
    fun `offline the saved lesson stays on screen`() = runTest {
        sources.lessonCache(11).stored = lesson
        sources.lessonFetcher(11).result = NetworkResult.Failure(AppError.Network(cause = IOException()))

        assertEquals(RefreshResult.CacheUsed, repository.loadLesson(11))
        assertTrue(repository.observeLesson(11).first() is SnapshotState.Data)
    }

    @Test
    fun `a lesson no longer published is dropped with app copy`() = runTest {
        sources.lessonCache(11).stored = lesson
        sources.lessonFetcher(11).result = NetworkResult.Failure(
            AppError.Server(code = 404, errorCode = SundaySchoolEndpoints.ERROR_LESSON_NOT_FOUND),
        )

        assertTrue(repository.loadLesson(11) is RefreshResult.Error)

        assertNull(sources.lessonCache(11).stored)
        val state = repository.observeLesson(11).first() as SnapshotState.Error
        assertEquals(SundaySchoolRepository.LESSON_GONE, state.error.userMessage)
    }

    @Test
    fun `a year no longer published is dropped`() = runTest {
        sources.indexCache(2025).stored = index
        sources.indexFetcher(2025).result = NetworkResult.Failure(
            AppError.Server(code = 404, errorCode = SundaySchoolEndpoints.ERROR_YEAR_NOT_FOUND),
        )

        repository.loadIndex(2025)

        assertNull(sources.indexCache(2025).stored)
        val state = repository.observeIndex(2025).first() as SnapshotState.Error
        assertEquals(SundaySchoolRepository.YEAR_GONE, state.error.userMessage)
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
        override suspend fun fetch(etag: String?): NetworkResult<T> = result
    }

    private class FakeSources : SundaySchoolSnapshotSources {
        private val indexCaches = mutableMapOf<Int?, FakeCache<SundaySchoolIndexDto>>()
        private val indexFetchers = mutableMapOf<Int?, FakeFetcher<SundaySchoolIndexDto>>()
        private val lessonCaches = mutableMapOf<Int, FakeCache<LessonDto>>()
        private val lessonFetchers = mutableMapOf<Int, FakeFetcher<LessonDto>>()

        override fun indexCache(year: Int?) = indexCaches.getOrPut(year) { FakeCache() }
        override fun indexFetcher(year: Int?) = indexFetchers.getOrPut(year) { FakeFetcher() }
        override fun lessonCache(id: Int) = lessonCaches.getOrPut(id) { FakeCache() }
        override fun lessonFetcher(id: Int) = lessonFetchers.getOrPut(id) { FakeFetcher() }
    }
}
