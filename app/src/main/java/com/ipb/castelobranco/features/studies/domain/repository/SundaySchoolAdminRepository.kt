package com.ipb.castelobranco.features.studies.domain.repository

import com.ipb.castelobranco.features.studies.domain.model.Lesson
import com.ipb.castelobranco.features.studies.domain.model.LessonDraft
import com.ipb.castelobranco.features.studies.domain.model.LessonSaveResult
import com.ipb.castelobranco.features.studies.domain.model.LessonSummary
import com.ipb.castelobranco.features.studies.domain.model.ReorderResult
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolYearAdmin

/** Panel side of the Sunday school (scope `sunday_school`). Nothing is stored on the device. */
interface SundaySchoolAdminRepository {
    suspend fun years(): Result<List<SundaySchoolYearAdmin>>
    suspend fun lessons(year: Int): Result<List<LessonSummary>>
    suspend fun lesson(id: Int): Result<Lesson>

    /** Creates when [id] is null. */
    suspend fun save(id: Int?, draft: LessonDraft): LessonSaveResult

    suspend fun delete(id: Int): Result<Unit>
    suspend fun reorder(year: Int, lessonIds: List<Int>): ReorderResult
    suspend fun setTheme(year: Int, theme: String): Result<SundaySchoolYearAdmin>

    companion object {
        const val LESSON_DELETED = "Esta aula foi apagada."
    }
}
