package com.ipb.castelobranco.features.studies.presentation.viewmodel

import com.ipb.castelobranco.core.domain.bible.BibleBookCatalog
import com.ipb.castelobranco.core.domain.bible.BibleBookInfo
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.studies.domain.model.Lesson
import com.ipb.castelobranco.features.studies.domain.model.LessonDraft
import com.ipb.castelobranco.features.studies.domain.model.LessonSaveResult
import com.ipb.castelobranco.features.studies.domain.model.LessonSummary
import com.ipb.castelobranco.features.studies.domain.model.ReorderResult
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolIndex
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolYearAdmin
import com.ipb.castelobranco.features.studies.domain.repository.SundaySchoolAdminRepository
import com.ipb.castelobranco.features.studies.domain.repository.SundaySchoolRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSundaySchoolRepository : SundaySchoolRepository {
    val indexes = mutableMapOf<Int?, MutableStateFlow<SnapshotState<SundaySchoolIndex>>>()
    val lessons = mutableMapOf<Int, MutableStateFlow<SnapshotState<Lesson>>>()
    val indexLoads = mutableListOf<Int?>()

    fun index(year: Int?) = indexes.getOrPut(year) { MutableStateFlow(SnapshotState.Loading) }
    fun lesson(id: Int) = lessons.getOrPut(id) { MutableStateFlow(SnapshotState.Loading) }

    override fun observeIndex(year: Int?): Flow<SnapshotState<SundaySchoolIndex>> = index(year)
    override suspend fun loadIndex(year: Int?): RefreshResult = RefreshResult.NotModified.also { indexLoads += year }
    override suspend fun refreshIndex(year: Int?): RefreshResult = RefreshResult.NotModified
    override fun observeLesson(id: Int): Flow<SnapshotState<Lesson>> = lesson(id)
    override suspend fun loadLesson(id: Int): RefreshResult = RefreshResult.NotModified
    override suspend fun refreshLesson(id: Int): RefreshResult = RefreshResult.NotModified
}

class FakeSundaySchoolAdminRepository : SundaySchoolAdminRepository {
    var lessonResult: Result<Lesson> = Result.failure(IllegalStateException())
    var saveResult: LessonSaveResult? = null
    val saved = mutableListOf<Pair<Int?, LessonDraft>>()

    override suspend fun years(): Result<List<SundaySchoolYearAdmin>> = Result.success(emptyList())
    override suspend fun lessons(year: Int): Result<List<LessonSummary>> = Result.success(emptyList())
    override suspend fun lesson(id: Int): Result<Lesson> = lessonResult
    override suspend fun save(id: Int?, draft: LessonDraft): LessonSaveResult {
        saved += id to draft
        return checkNotNull(saveResult)
    }
    override suspend fun delete(id: Int): Result<Unit> = Result.success(Unit)
    override suspend fun reorder(year: Int, lessonIds: List<Int>): ReorderResult = ReorderResult.Saved
    override suspend fun setTheme(year: Int, theme: String): Result<SundaySchoolYearAdmin> =
        Result.success(SundaySchoolYearAdmin(year, theme, 0, 0))
}

class FakeBibleBookCatalog(books: List<BibleBookInfo> = emptyList()) : BibleBookCatalog {
    override val books = MutableStateFlow(books)
    override suspend fun load() = Unit
}
