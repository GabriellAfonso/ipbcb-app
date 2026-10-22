package com.ipb.castelobranco.features.hymnal.presentation.viewmodel

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.hymnal.domain.model.Hymn
import com.ipb.castelobranco.features.hymnal.domain.usecase.ObserveHymnsUseCase
import com.ipb.castelobranco.features.hymnal.domain.usecase.SearchHymnsUseCase
import com.ipb.castelobranco.features.hymnal.presentation.screens.HymnalUiState
import com.ipb.castelobranco.features.settings.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
class HymnalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var observeHymnsUseCase: ObserveHymnsUseCase
    private lateinit var searchHymnsUseCase: SearchHymnsUseCase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: HymnalViewModel

    private val fakeHymns = listOf(
        Hymn(id = null, number = "1", title = "Quão Grande És Tu", lyrics = emptyList()),
        Hymn(id = null, number = "2", title = "Grande É o Senhor", lyrics = emptyList()),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        observeHymnsUseCase = mockk()
        searchHymnsUseCase = mockk()
        settingsRepository = mockk()

        every { observeHymnsUseCase() } returns flowOf(SnapshotState.Loading)
        every { settingsRepository.hymnalFontSizeFlow } returns flowOf(16f)
        every { searchHymnsUseCase(any(), any()) } returns emptyList()
        coEvery { observeHymnsUseCase.refresh() } returns RefreshResult.Updated

        viewModel = HymnalViewModel(observeHymnsUseCase, searchHymnsUseCase, settingsRepository, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Helper: subscribe and advance so WhileSubscribed flows populate their state
    private fun TestScope.subscribeAndAdvance() {
        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        job.cancel()
    }

    // region uiState

    @Test
    fun `uiState emits loading state when observe emits Loading`() = runTest {
        subscribeAndAdvance()
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `uiState populates hymns when observe emits Data`() = runTest {
        every { observeHymnsUseCase() } returns flowOf(SnapshotState.Data(fakeHymns))
        every { searchHymnsUseCase(fakeHymns, "") } returns fakeHymns
        viewModel = HymnalViewModel(observeHymnsUseCase, searchHymnsUseCase, settingsRepository, testDispatcher)

        subscribeAndAdvance()

        assertEquals(fakeHymns, viewModel.uiState.value.hymns)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `uiState sets generic error message when observe emits Error`() = runTest {
        every { observeHymnsUseCase() } returns
            flowOf(SnapshotState.Error(AppError.Server(code = 500, message = "fetch failed")))
        viewModel = HymnalViewModel(observeHymnsUseCase, searchHymnsUseCase, settingsRepository, testDispatcher)

        subscribeAndAdvance()

        assertEquals(
            "Não foi possível completar a operação. Tente novamente mais tarde.",
            viewModel.uiState.value.error,
        )
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `uiState uses the authored userMessage when the error carries one`() = runTest {
        every { observeHymnsUseCase() } returns flowOf(
            SnapshotState.Error(
                AppError.Server(code = 500, message = "raw body", userMessage = "Erro ao carregar hinário")
            )
        )
        viewModel = HymnalViewModel(observeHymnsUseCase, searchHymnsUseCase, settingsRepository, testDispatcher)

        subscribeAndAdvance()

        assertEquals("Erro ao carregar hinário", viewModel.uiState.value.error)
    }

    @Test
    fun `uiState emits data without waiting for the debounce when the query is empty`() = runTest {
        every { observeHymnsUseCase() } returns flowOf(SnapshotState.Data(fakeHymns))
        every { searchHymnsUseCase(fakeHymns, "") } returns fakeHymns
        viewModel = HymnalViewModel(observeHymnsUseCase, searchHymnsUseCase, settingsRepository, testDispatcher)

        val job = launch { viewModel.uiState.collect { } }
        runCurrent() // nao avanca o tempo virtual
        job.cancel()

        assertEquals(fakeHymns, viewModel.uiState.value.filteredHymns)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // endregion

    // region onQueryChange

    @Test
    fun `onQueryChange debounces a non-empty query`() = runTest {
        val hymnsFlow = MutableStateFlow<SnapshotState<List<Hymn>>>(SnapshotState.Data(fakeHymns))
        every { observeHymnsUseCase() } returns hymnsFlow
        every { searchHymnsUseCase(fakeHymns, "") } returns fakeHymns
        every { searchHymnsUseCase(fakeHymns, "Quão") } returns listOf(fakeHymns[0])
        viewModel = HymnalViewModel(observeHymnsUseCase, searchHymnsUseCase, settingsRepository, testDispatcher)
        val job = launch { viewModel.uiState.collect { } }
        runCurrent()

        viewModel.onQueryChange("Quão")
        advanceTimeBy(199)
        runCurrent()
        verify(exactly = 0) { searchHymnsUseCase(fakeHymns, "Quão") }

        advanceTimeBy(2)
        runCurrent()
        verify { searchHymnsUseCase(fakeHymns, "Quão") }

        job.cancel()
    }

    @Test
    fun `onQueryChange updates query in uiState`() = runTest {
        val hymnsFlow = MutableStateFlow<SnapshotState<List<Hymn>>>(SnapshotState.Data(fakeHymns))
        every { observeHymnsUseCase() } returns hymnsFlow
        every { searchHymnsUseCase(fakeHymns, any()) } answers { emptyList() }
        viewModel = HymnalViewModel(observeHymnsUseCase, searchHymnsUseCase, settingsRepository, testDispatcher)
        subscribeAndAdvance()

        viewModel.onQueryChange("Quão")
        advanceUntilIdle()

        assertEquals("Quão", viewModel.query.value)
    }

    @Test
    fun `onQueryChange triggers search with new query`() = runTest {
        val hymnsFlow = MutableStateFlow<SnapshotState<List<Hymn>>>(SnapshotState.Data(fakeHymns))
        every { observeHymnsUseCase() } returns hymnsFlow
        every { searchHymnsUseCase(fakeHymns, "Quão") } returns listOf(fakeHymns[0])
        every { searchHymnsUseCase(fakeHymns, "") } returns fakeHymns
        viewModel = HymnalViewModel(observeHymnsUseCase, searchHymnsUseCase, settingsRepository, testDispatcher)
        subscribeAndAdvance()

        viewModel.onQueryChange("Quão")
        advanceUntilIdle()

        verify { searchHymnsUseCase(fakeHymns, "Quão") }
    }

    @Test
    fun `onQueryChange updates filteredHymns`() = runTest {
        val hymnsFlow = MutableStateFlow<SnapshotState<List<Hymn>>>(SnapshotState.Data(fakeHymns))
        every { observeHymnsUseCase() } returns hymnsFlow
        val filtered = listOf(fakeHymns[0])
        every { searchHymnsUseCase(fakeHymns, "Quão") } returns filtered
        every { searchHymnsUseCase(fakeHymns, "") } returns fakeHymns
        viewModel = HymnalViewModel(observeHymnsUseCase, searchHymnsUseCase, settingsRepository, testDispatcher)
        subscribeAndAdvance()

        viewModel.onQueryChange("Quão")
        advanceUntilIdle()

        assertEquals(filtered, viewModel.uiState.value.filteredHymns)
    }

    // endregion

    // region refresh

    @Test
    fun `refresh calls observeHymnsUseCase refresh`() = runTest {
        viewModel.refresh(minDurationMs = 0L)
        advanceUntilIdle()
        coVerify { observeHymnsUseCase.refresh() }
    }

    @Test
    fun `isRefreshing is false after refresh completes`() = runTest {
        viewModel.refresh(minDurationMs = 0L)
        advanceUntilIdle()
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `refresh guard prevents concurrent refreshes`() = runTest {
        coEvery { observeHymnsUseCase.refresh() } coAnswers {
            kotlinx.coroutines.delay(5_000)
            RefreshResult.Updated
        }
        viewModel.refresh() // schedules coroutine
        runCurrent() // run it until delay — sets _isRefreshing = true
        viewModel.refresh() // guarded
        advanceUntilIdle()
        coVerify(exactly = 1) { observeHymnsUseCase.refresh() }
    }

    // endregion

    // region hymnalFontSize

    @Test
    fun `hymnalFontSize reflects value from settingsRepository`() = runTest {
        every { settingsRepository.hymnalFontSizeFlow } returns flowOf(20f)
        viewModel = HymnalViewModel(observeHymnsUseCase, searchHymnsUseCase, settingsRepository, testDispatcher)

        val job = launch { viewModel.hymnalFontSize.collect { } }
        advanceUntilIdle()
        job.cancel()

        assertEquals(20f, viewModel.hymnalFontSize.value)
    }

    @Test
    fun `setHymnalFontSize delegates to settingsRepository`() = runTest {
        coEvery { settingsRepository.setHymnalFontSize(any()) } returns Unit
        viewModel.setHymnalFontSize(18f)
        advanceUntilIdle()
        coVerify { settingsRepository.setHymnalFontSize(18f) }
    }

    // endregion
}
