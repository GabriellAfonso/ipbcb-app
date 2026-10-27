package com.ipb.castelobranco.features.studies.domain.repository

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.studies.domain.model.Lesson
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolIndex
import kotlinx.coroutines.flow.Flow

/**
 * Published Sunday school lessons, kept on the device after the first load and revalidated by ETag. A `null` year
 * is the server's default year, which also carries the list of years. `load*` shows the saved copy first, then asks
 * the server whether it changed; a year or lesson the server no longer publishes is dropped from the device.
 */
interface SundaySchoolRepository {
    fun observeIndex(year: Int?): Flow<SnapshotState<SundaySchoolIndex>>
    suspend fun loadIndex(year: Int?): RefreshResult
    suspend fun refreshIndex(year: Int?): RefreshResult

    fun observeLesson(id: Int): Flow<SnapshotState<Lesson>>
    suspend fun loadLesson(id: Int): RefreshResult
    suspend fun refreshLesson(id: Int): RefreshResult

    companion object {
        const val LESSON_GONE = "Esta aula não está mais disponível."
        const val YEAR_GONE = "Este ano não tem mais aulas publicadas."
    }
}
