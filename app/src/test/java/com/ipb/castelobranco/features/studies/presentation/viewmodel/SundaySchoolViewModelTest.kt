package com.ipb.castelobranco.features.studies.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.studies.domain.model.LessonSummary
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolIndex
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolYear
import com.ipb.castelobranco.features.studies.domain.usecase.ObserveSundaySchoolIndexUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SundaySchoolViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeSundaySchoolRepository()

    private val y2027 = SundaySchoolYear(2027, "")
    private val y2026 = SundaySchoolYear(2026, "O Drama das Escrituras")

    private fun index(year: SundaySchoolYear, vararg titles: String) = SundaySchoolIndex(
        years = listOf(y2027, y2026),
        defaultYear = 2027,
        year = year,
        lessons = titles.mapIndexed { i, title -> LessonSummary(i, title, null, "", i) },
    )

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `opens the server's default year with its tabs`() = runTest(dispatcher) {
        val vm = SundaySchoolViewModel(ObserveSundaySchoolIndexUseCase(repository))
        repository.index(null).value = SnapshotState.Data(index(y2027, "Gênesis"))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf(null), repository.indexLoads)
        assertEquals(listOf(2027, 2026), state.years.map { it.year })
        assertEquals(2027, state.selectedYear)
        assertEquals("2027", state.yearLabel)
        assertEquals("", state.yearTheme)
        assertEquals("1 aula", state.lessonCountLabel)
        assertEquals("Gênesis", state.lessons.single().title)
    }

    @Test
    fun `another tab reads its own year`() = runTest(dispatcher) {
        val vm = SundaySchoolViewModel(ObserveSundaySchoolIndexUseCase(repository))
        repository.index(null).value = SnapshotState.Data(index(y2027, "Gênesis"))
        repository.index(2026).value = SnapshotState.Data(index(y2026, "Josué", "Rute"))
        advanceUntilIdle()

        vm.selectYear(2026)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf(null, 2026), repository.indexLoads)
        assertEquals("2026 — O Drama das Escrituras", state.yearLabel)
        assertEquals("O Drama das Escrituras", state.yearTheme)
        assertEquals("2 aulas", state.lessonCountLabel)
        assertEquals(listOf("Josué", "Rute"), state.lessons.map { it.title })
        assertEquals(listOf(2027, 2026), state.years.map { it.year })
    }

    @Test
    fun `no published lesson is the empty state`() = runTest(dispatcher) {
        val vm = SundaySchoolViewModel(ObserveSundaySchoolIndexUseCase(repository))
        repository.index(null).value = SnapshotState.Data(SundaySchoolIndex(emptyList(), null, null, emptyList()))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun `error without a copy shows a message`() = runTest(dispatcher) {
        val vm = SundaySchoolViewModel(ObserveSundaySchoolIndexUseCase(repository))
        repository.index(null).value = SnapshotState.Error(AppError.Network())
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
    }
}
