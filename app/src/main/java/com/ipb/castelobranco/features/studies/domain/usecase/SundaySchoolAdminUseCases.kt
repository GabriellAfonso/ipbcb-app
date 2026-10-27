package com.ipb.castelobranco.features.studies.domain.usecase

import com.ipb.castelobranco.features.studies.domain.model.Lesson
import com.ipb.castelobranco.features.studies.domain.model.LessonDraft
import com.ipb.castelobranco.features.studies.domain.model.LessonSaveResult
import com.ipb.castelobranco.features.studies.domain.model.LessonSummary
import com.ipb.castelobranco.features.studies.domain.model.ReorderResult
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolYearAdmin
import com.ipb.castelobranco.features.studies.domain.repository.SundaySchoolAdminRepository
import javax.inject.Inject

class ListSundaySchoolYearsUseCase @Inject constructor(private val repository: SundaySchoolAdminRepository) {
    suspend operator fun invoke(): Result<List<SundaySchoolYearAdmin>> = repository.years()
}

class ListSundaySchoolLessonsUseCase @Inject constructor(private val repository: SundaySchoolAdminRepository) {
    suspend operator fun invoke(year: Int): Result<List<LessonSummary>> = repository.lessons(year)
}

class GetSundaySchoolLessonUseCase @Inject constructor(private val repository: SundaySchoolAdminRepository) {
    suspend operator fun invoke(id: Int): Result<Lesson> = repository.lesson(id)
}

/** Creates when `id` is null. The draft is checked by [ValidateLessonDraftUseCase] first, in the ViewModel. */
class SaveSundaySchoolLessonUseCase @Inject constructor(private val repository: SundaySchoolAdminRepository) {
    suspend operator fun invoke(id: Int?, draft: LessonDraft): LessonSaveResult = repository.save(id, draft)
}

class DeleteSundaySchoolLessonUseCase @Inject constructor(private val repository: SundaySchoolAdminRepository) {
    suspend operator fun invoke(id: Int): Result<Unit> = repository.delete(id)
}

class ReorderSundaySchoolLessonsUseCase @Inject constructor(private val repository: SundaySchoolAdminRepository) {
    suspend operator fun invoke(year: Int, lessonIds: List<Int>): ReorderResult = repository.reorder(year, lessonIds)
}

class SetSundaySchoolThemeUseCase @Inject constructor(private val repository: SundaySchoolAdminRepository) {
    suspend operator fun invoke(year: Int, theme: String): Result<SundaySchoolYearAdmin> =
        repository.setTheme(year, theme)
}
