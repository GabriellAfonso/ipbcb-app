package com.ipb.castelobranco.features.studies.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.studies.DoctrineTestFixtures
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSearchResult
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.MAX_FONT_SIZE
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.MIN_FONT_SIZE
import com.ipb.castelobranco.features.studies.domain.usecase.ObserveDoctrineDocumentUseCase
import com.ipb.castelobranco.features.studies.domain.usecase.SearchDoctrineUseCase
import com.ipb.castelobranco.features.studies.presentation.navigation.StudiesRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatechismViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeDoctrineRepository()
    private val preferences = FakeDoctrinePreferences()

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.loadedViewModel(): CatechismViewModel {
        val vm = CatechismViewModel(
            savedStateHandle = SavedStateHandle(mapOf(StudiesRoutes.ARG_SLUG to DoctrineTestFixtures.SHORTER)),
            observeDocument = ObserveDoctrineDocumentUseCase(repository),
            search = SearchDoctrineUseCase(),
            preferences = preferences,
            defaultDispatcher = dispatcher,
        )
        repository.document(DoctrineTestFixtures.SHORTER).value = SnapshotState.Data(DoctrineTestFixtures.catechism)
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `shows the short title and the questions`() = runTest(dispatcher) {
        val vm = loadedViewModel()

        assertEquals(1, repository.documentLoads)
        assertEquals("Breve Catecismo", vm.uiState.value.title)
        assertEquals(listOf(1, 33, 34), vm.uiState.value.questions.map { it.number })
    }

    @Test
    fun `go to a valid number scrolls to its index`() = runTest(dispatcher) {
        val vm = loadedViewModel()

        vm.events.test {
            vm.goTo("34")
            assertEquals(CatechismEvent.ScrollTo(2), awaitItem())
        }
    }

    @Test
    fun `go to out of range or not a number explains the range`() = runTest(dispatcher) {
        val vm = loadedViewModel()

        vm.events.test {
            vm.goTo("200")
            assertEquals(CatechismEvent.ShowMessage("Digite um número de 1 a 34"), awaitItem())
            vm.goTo("abc")
            assertEquals(CatechismEvent.ShowMessage("Digite um número de 1 a 34"), awaitItem())
        }
    }

    @Test
    fun `search shows results only for a non-blank term and a result scrolls and closes search`() =
        runTest(dispatcher) {
            val vm = loadedViewModel()
            vm.openSearch()
            advanceUntilIdle()
            assertNull(vm.uiState.value.results)

            vm.onQueryChange("adocao")
            advanceUntilIdle()
            val result = vm.uiState.value.results!!.single() as DoctrineSearchResult.Question
            assertEquals(34, result.number)

            vm.events.test {
                vm.onResultClick(result)
                assertEquals(CatechismEvent.ScrollTo(2), awaitItem())
            }
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isSearching)
        }

    @Test
    fun `font size steps by two and stops at the limits`() = runTest(dispatcher) {
        val vm = loadedViewModel()

        vm.increaseFont()
        advanceUntilIdle()
        assertEquals(20f, vm.uiState.value.fontSize.size)

        preferences.fontSizeFlow.value = MAX_FONT_SIZE
        advanceUntilIdle()
        assertFalse(vm.uiState.value.fontSize.canIncrease)
        vm.increaseFont()
        advanceUntilIdle()
        assertEquals(MAX_FONT_SIZE, preferences.fontSizeFlow.value)

        preferences.fontSizeFlow.value = MIN_FONT_SIZE
        advanceUntilIdle()
        assertFalse(vm.uiState.value.fontSize.canDecrease)
        assertTrue(vm.uiState.value.fontSize.canIncrease)
    }
}
