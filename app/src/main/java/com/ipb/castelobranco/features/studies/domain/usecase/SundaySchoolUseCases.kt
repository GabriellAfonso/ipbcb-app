package com.ipb.castelobranco.features.studies.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.studies.domain.model.Lesson
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolIndex
import com.ipb.castelobranco.features.studies.domain.repository.SundaySchoolRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** A year of published lessons; `null` = the server's default year, which also lists the years. */
class ObserveSundaySchoolIndexUseCase @Inject constructor(
    private val repository: SundaySchoolRepository,
) {
    operator fun invoke(year: Int?): Flow<SnapshotState<SundaySchoolIndex>> = repository.observeIndex(year)

    suspend fun load(year: Int?): RefreshResult = repository.loadIndex(year)

    suspend fun refresh(year: Int?): RefreshResult = repository.refreshIndex(year)
}

class ObserveSundaySchoolLessonUseCase @Inject constructor(
    private val repository: SundaySchoolRepository,
) {
    operator fun invoke(id: Int): Flow<SnapshotState<Lesson>> = repository.observeLesson(id)

    suspend fun load(id: Int): RefreshResult = repository.loadLesson(id)

    suspend fun refresh(id: Int): RefreshResult = repository.refreshLesson(id)
}
