package com.ipb.castelobranco.features.studies.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.BaseSnapshotRepository
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.studies.data.api.SundaySchoolEndpoints
import com.ipb.castelobranco.features.studies.data.mapper.toDomain
import com.ipb.castelobranco.features.studies.domain.model.Lesson
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolIndex
import com.ipb.castelobranco.features.studies.domain.repository.SundaySchoolRepository
import com.ipb.castelobranco.features.studies.domain.repository.SundaySchoolRepository.Companion.LESSON_GONE
import com.ipb.castelobranco.features.studies.domain.repository.SundaySchoolRepository.Companion.YEAR_GONE
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SundaySchoolRepositoryImpl @Inject constructor(
    private val sources: SundaySchoolSnapshotSources,
    private val logger: Logger,
) : SundaySchoolRepository {

    private val indexes = mutableMapOf<Int?, Snapshot<*, SundaySchoolIndex>>()
    private val lessons = mutableMapOf<Int, Snapshot<*, Lesson>>()

    override fun observeIndex(year: Int?): Flow<SnapshotState<SundaySchoolIndex>> = index(year).observe()

    override suspend fun loadIndex(year: Int?): RefreshResult = index(year).load()

    override suspend fun refreshIndex(year: Int?): RefreshResult = index(year).revalidate()

    override fun observeLesson(id: Int): Flow<SnapshotState<Lesson>> = lesson(id).observe()

    override suspend fun loadLesson(id: Int): RefreshResult = lesson(id).load()

    override suspend fun refreshLesson(id: Int): RefreshResult = lesson(id).revalidate()

    private fun index(year: Int?): Snapshot<*, SundaySchoolIndex> = synchronized(indexes) {
        indexes.getOrPut(year) {
            Snapshot(
                cache = sources.indexCache(year),
                fetcher = sources.indexFetcher(year),
                mapper = { it.toDomain() },
                logger = logger,
                tag = "SundaySchoolIndexSnapshot",
                goneCode = SundaySchoolEndpoints.ERROR_YEAR_NOT_FOUND,
                goneMessage = YEAR_GONE,
            )
        }
    }

    private fun lesson(id: Int): Snapshot<*, Lesson> = synchronized(lessons) {
        lessons.getOrPut(id) {
            Snapshot(
                cache = sources.lessonCache(id),
                fetcher = sources.lessonFetcher(id),
                mapper = { it.toDomain() },
                logger = logger,
                tag = "SundaySchoolLessonSnapshot",
                goneCode = SundaySchoolEndpoints.ERROR_LESSON_NOT_FOUND,
                goneMessage = LESSON_GONE,
            )
        }
    }

    /**
     * A snapshot that, unlike the base class, drops its saved copy when the server says the year or lesson is no
     * longer published ([goneCode]): showing an unpublished lesson forever would be wrong (research R-03).
     */
    private class Snapshot<Dto, Domain>(
        cache: SnapshotCache<Dto>,
        fetcher: SnapshotFetcher<Dto>,
        mapper: (Dto) -> Domain,
        logger: Logger,
        tag: String,
        goneCode: String,
        private val goneMessage: String,
        private val watcher: GoneWatcher<Dto> = GoneWatcher(fetcher, goneCode),
    ) : BaseSnapshotRepository<Dto, Domain>(cache, watcher, mapper, logger, tag) {

        /** Saved copy on screen first, then the ETag check. */
        suspend fun load(): RefreshResult {
            preload()
            return revalidate()
        }

        /** [refresh] plus dropping the copy when the server no longer publishes it. */
        suspend fun revalidate(): RefreshResult {
            watcher.gone = null
            val result = refresh()
            val gone = watcher.gone ?: return result
            clearCache()
            val error = AppError.Server(
                code = gone.code,
                message = gone.message,
                cause = gone,
                errorCode = gone.errorCode,
                userMessage = goneMessage,
            )
            emitError(error)
            return RefreshResult.Error(error)
        }
    }

    /** Remembers the last fetch failure that means "not published any more". */
    private class GoneWatcher<Dto>(
        private val delegate: SnapshotFetcher<Dto>,
        private val goneCode: String,
    ) : SnapshotFetcher<Dto> {

        @Volatile
        var gone: AppError.Server? = null

        override suspend fun fetch(etag: String?): NetworkResult<Dto> {
            val result = delegate.fetch(etag)
            val error = (result as? NetworkResult.Failure)?.throwable as? AppError.Server
            if (error != null && error.code == HTTP_NOT_FOUND && error.errorCode == goneCode) gone = error
            return result
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
