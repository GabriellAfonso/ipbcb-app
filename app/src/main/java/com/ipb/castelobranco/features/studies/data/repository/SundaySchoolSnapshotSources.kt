package com.ipb.castelobranco.features.studies.data.repository

import com.ipb.castelobranco.core.data.snapshot.RetrofitSnapshotFetcher
import com.ipb.castelobranco.core.data.snapshot.SnapshotCacheFactory
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.studies.data.api.SundaySchoolApi
import com.ipb.castelobranco.features.studies.data.dto.LessonDto
import com.ipb.castelobranco.features.studies.data.dto.SundaySchoolIndexDto
import javax.inject.Inject

/**
 * Where each Sunday school snapshot is stored and fetched. Years and lesson ids come from the server, so caches and
 * fetchers are built on demand. A `null` year is the request without `year`: the server's default year.
 */
interface SundaySchoolSnapshotSources {
    fun indexCache(year: Int?): SnapshotCache<SundaySchoolIndexDto>
    fun indexFetcher(year: Int?): SnapshotFetcher<SundaySchoolIndexDto>
    fun lessonCache(id: Int): SnapshotCache<LessonDto>
    fun lessonFetcher(id: Int): SnapshotFetcher<LessonDto>
}

class DefaultSundaySchoolSnapshotSources @Inject constructor(
    private val factory: SnapshotCacheFactory,
    private val api: SundaySchoolApi,
) : SundaySchoolSnapshotSources {

    override fun indexCache(year: Int?): SnapshotCache<SundaySchoolIndexDto> =
        factory.create(key = indexKey(year), serializer = SundaySchoolIndexDto.serializer())

    override fun indexFetcher(year: Int?): SnapshotFetcher<SundaySchoolIndexDto> =
        RetrofitSnapshotFetcher { etag -> api.getIndex(year, etag) }

    override fun lessonCache(id: Int): SnapshotCache<LessonDto> =
        factory.create(key = lessonKey(id), serializer = LessonDto.serializer())

    override fun lessonFetcher(id: Int): SnapshotFetcher<LessonDto> =
        RetrofitSnapshotFetcher { etag -> api.getLesson(id, etag) }

    companion object {
        const val INDEX_KEY = "sunday_school_index"
        private const val LESSON_KEY_PREFIX = "sunday_school_lesson_"

        fun indexKey(year: Int?): String = if (year == null) INDEX_KEY else "${INDEX_KEY}_$year"

        fun lessonKey(id: Int): String = LESSON_KEY_PREFIX + id
    }
}
