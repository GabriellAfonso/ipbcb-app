package com.ipb.castelobranco.features.studies.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.studies.domain.model.ContentError
import com.ipb.castelobranco.features.studies.domain.model.EditorLocation
import com.ipb.castelobranco.features.studies.domain.model.Lesson
import com.ipb.castelobranco.features.studies.domain.model.LessonBlock
import com.ipb.castelobranco.features.studies.domain.model.LessonContent
import com.ipb.castelobranco.features.studies.domain.model.LessonSaveResult
import com.ipb.castelobranco.features.studies.domain.model.LessonSection
import com.ipb.castelobranco.features.studies.domain.model.LessonStatus
import com.ipb.castelobranco.features.studies.domain.model.StandardSections
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolYear
import com.ipb.castelobranco.features.studies.domain.usecase.GetSundaySchoolLessonUseCase
import com.ipb.castelobranco.features.studies.domain.usecase.SaveSundaySchoolLessonUseCase
import com.ipb.castelobranco.features.studies.domain.usecase.ValidateLessonDraftUseCase
import com.ipb.castelobranco.features.studies.presentation.navigation.StudiesRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SundaySchoolLessonFormViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val admin = FakeSundaySchoolAdminRepository()

    private val table = LessonBlock.Table(listOf("a"), listOf(listOf("b")))
    private val lesson = Lesson(
        id = 11,
        title = "Panorama de Daniel",
        date = null,
        teacher = "",
        position = 10,
        status = LessonStatus.PUBLISHED,
        year = SundaySchoolYear(2026, "O Drama das Escrituras"),
        content = LessonContent(
            listOf(LessonSection("Contexto", "hourglass", listOf(LessonBlock.Paragraph("a"), table))),
        ),
        books = emptyMap(),
    )

    private fun viewModel(lessonId: Int, year: Int = 2026) = SundaySchoolLessonFormViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(StudiesRoutes.ARG_LESSON_ID to lessonId, StudiesRoutes.ARG_YEAR to year),
        ),
        getLesson = GetSundaySchoolLessonUseCase(admin),
        saveLesson = SaveSundaySchoolLessonUseCase(admin),
        validate = ValidateLessonDraftUseCase(),
        catalog = FakeBibleBookCatalog(),
    )

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a new lesson starts as a draft from the standard sections`() = runTest(dispatcher) {
        val vm = viewModel(StudiesRoutes.NEW_LESSON, year = 2027)
        advanceUntilIdle()

        val draft = vm.uiState.value.draft!!
        assertEquals(2027, draft.year)
        assertEquals(LessonStatus.DRAFT, draft.status)
        assertEquals(StandardSections.template(), draft.content)
        assertFalse(vm.uiState.value.hasChanges)
    }

    @Test
    fun `blocks the editor does not edit are kept and sent back`() = runTest(dispatcher) {
        admin.lessonResult = Result.success(lesson)
        admin.saveResult = LessonSaveResult.Saved(lesson)
        val vm = viewModel(11)
        advanceUntilIdle()

        vm.updateBlock(0, 1, LessonBlock.Paragraph("trocado"))
        vm.updateBlock(0, 0, LessonBlock.Paragraph("novo texto"))
        vm.save()
        advanceUntilIdle()

        val (id, sent) = admin.saved.single()
        assertEquals(11, id)
        assertEquals(listOf(LessonBlock.Paragraph("novo texto"), table), sent.content.sections.single().blocks)
    }

    @Test
    fun `server content errors land next to their block`() = runTest(dispatcher) {
        admin.lessonResult = Result.success(lesson)
        admin.saveResult = LessonSaveResult.Invalid(
            contentErrors = listOf(ContentError("sections.0.blocks.0.paragraph.proof_texts.0.reference", "Dn 13")),
            fieldErrors = emptyMap(),
            message = "Conteúdo inválido",
        )
        val vm = viewModel(11)
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        val errors = vm.uiState.value.locationErrors
        assertEquals(listOf("Dn 13"), errors[EditorLocation.Block(0, 0)])
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `an invalid draft is not sent`() = runTest(dispatcher) {
        val vm = viewModel(StudiesRoutes.NEW_LESSON)
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        assertTrue(admin.saved.isEmpty())
        assertTrue(vm.uiState.value.fieldErrors.containsKey(SundaySchoolLessonFormViewModel.FIELD_TITLE))
    }

    @Test
    fun `a failed load shows the error`() = runTest(dispatcher) {
        admin.lessonResult = Result.failure(AppError.Network())
        val vm = viewModel(11)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.loadError != null)
    }

    @Test
    fun `back with changes asks to discard`() = runTest(dispatcher) {
        val vm = viewModel(StudiesRoutes.NEW_LESSON)
        advanceUntilIdle()

        vm.setTitle("Gênesis")
        vm.onBack()

        assertTrue(vm.uiState.value.confirmDiscard)
    }
}
