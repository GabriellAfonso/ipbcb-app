package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.ipb.castelobranco.core.data.local.SongScrollMode
import com.ipb.castelobranco.core.data.local.ThemePreferences
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.features.worshiphub.chordcharts.data.local.ChordChartPreferences
import com.ipb.castelobranco.features.profile.domain.model.MeProfile
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.model.ChordChart
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.repository.ChordChartRepository
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.usecase.GetChordChartsUseCase
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
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
class ChordChartDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var getChordChartsUseCase: GetChordChartsUseCase
    private lateinit var chordChartRepository: ChordChartRepository
    private lateinit var songsRepository: SongsRepository
    private lateinit var themePreferences: ThemePreferences
    private lateinit var chartPreferences: ChordChartPreferences
    private val textScale = MutableStateFlow(100)
    private val accessRepository = FakeAccessRepository()

    private val fakeSongs = listOf(
        Song(id = 10, title = "Oceans", artist = "Hillsong", categoryName = "Louvor"),
    )
    private val fakeCharts = listOf(
        ChordChart(id = 1, songId = 10, content = "[G]Amazing [D]grace", tone = "G", instrument = "Violão"),
        ChordChart(id = 2, songId = 99, content = "...", tone = "D", instrument = "Teclado"),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getChordChartsUseCase = mockk()
        chordChartRepository = mockk()
        songsRepository = mockk()
        themePreferences = mockk()

        every { themePreferences.songScrollModeFlow } returns flowOf(SongScrollMode.HORIZONTAL)
        chartPreferences = mockk()
        every { chartPreferences.textScaleFlow } returns textScale
        coEvery { chartPreferences.setTextScale(any()) } answers { textScale.value = firstArg() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(chordChartId: Int = 1): ChordChartDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("chordChartId" to chordChartId))
        return ChordChartDetailViewModel(
            savedStateHandle,
            getChordChartsUseCase,
            chordChartRepository,
            songsRepository,
            themePreferences,
            chartPreferences,
            ObserveAccessUseCase(accessRepository),
        )
    }

    // region uiState

    @Test
    fun `initial state is loading`() = runTest {
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Loading)
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Loading)

        val vm = createViewModel()

        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun `Data state maps songName and tone`() = runTest {
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeCharts))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))

        val vm = createViewModel(chordChartId = 1)
        val collector = testScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Oceans", state.songName)
        assertEquals("G", state.tone)
        assertNull(state.error)

        collector.cancel()
    }

    @Test
    fun `Data state with unknown songId uses fallback name`() = runTest {
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeCharts))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))

        val vm = createViewModel(chordChartId = 2)
        val collector = testScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("Song #99", vm.uiState.value.songName)

        collector.cancel()
    }

    @Test
    fun `Data state with chart not found shows error`() = runTest {
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeCharts))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))

        val vm = createViewModel(chordChartId = 999)
        val collector = testScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("Chart not found", vm.uiState.value.error)

        collector.cancel()
    }

    @Test
    fun `Error state maps error message`() = runTest {
        every { getChordChartsUseCase.observe() } returns
            flowOf(SnapshotState.Error(AppError.Server(code = 500, message = "fail")))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))

        val vm = createViewModel()
        val collector = testScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("Não foi possível completar a operação. Tente novamente mais tarde.", vm.uiState.value.error)

        collector.cancel()
    }

    // endregion

    // region text size

    @Test
    fun `the text size is saved and stops at its limits`() = runTest(testDispatcher) {
        textScale.value = 150
        val vm = editableViewModel()
        val collector = testScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.growText()
        advanceUntilIdle()
        assertEquals(160, vm.uiState.value.textScalePercent)
        assertFalse(vm.uiState.value.canGrowText)

        vm.growText()
        advanceUntilIdle()
        assertEquals(160, vm.uiState.value.textScalePercent)

        vm.shrinkText()
        advanceUntilIdle()
        assertEquals(150, vm.uiState.value.textScalePercent)
        coVerify { chartPreferences.setTextScale(150) }

        collector.cancel()
    }

    // endregion

    // region edit

    private fun editableViewModel(): ChordChartDetailViewModel {
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeCharts))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        return createViewModel()
    }

    @Test
    fun `edit form starts with the chart's current values`() = runTest(testDispatcher) {
        val vm = editableViewModel()
        advanceUntilIdle()

        vm.enterEditMode()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isEditing)
        assertEquals("[G]Amazing [D]grace", state.editContent)
        assertEquals("G", state.editTone)
        assertEquals("Violão", state.editInstrument)
        assertEquals(emptyList<String>(), state.editMissing)
    }

    @Test
    fun `saveEdit sends the changed fields and goes back to reading`() = runTest(testDispatcher) {
        coEvery { chordChartRepository.updateChordChart(1, "[C]Novo", "A", "Baixo") } returns Result.success(Unit)
        val vm = editableViewModel()
        advanceUntilIdle()
        vm.enterEditMode()
        vm.onEditContentChange("[C]Novo")
        vm.onEditToneChange("A")
        vm.onEditInstrumentChange("Baixo")

        vm.saveEdit()
        advanceUntilIdle()

        coVerify(exactly = 1) { chordChartRepository.updateChordChart(1, "[C]Novo", "A", "Baixo") }
        assertEquals(false, vm.uiState.value.isEditing)
    }

    @Test
    fun `conflict keeps the form, shows the server text and clears on a new tone`() = runTest(testDispatcher) {
        val conflict = AppError.Server(409, userMessage = "Já existe uma cifra dessa música no tom A para Violão.")
        coEvery { chordChartRepository.updateChordChart(1, any(), "A", "Violão") } returns Result.failure(conflict)
        val vm = editableViewModel()
        advanceUntilIdle()
        vm.enterEditMode()
        vm.onEditToneChange("A")

        vm.saveEdit()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEditing)
        assertEquals("A", vm.uiState.value.editTone)
        assertEquals("Já existe uma cifra dessa música no tom A para Violão.", vm.uiState.value.saveError)

        vm.onEditToneChange("B")
        advanceUntilIdle()

        assertNull(vm.uiState.value.saveError)
    }

    @Test
    fun `a blank field is reported as missing and nothing is sent`() = runTest(testDispatcher) {
        val vm = editableViewModel()
        advanceUntilIdle()
        vm.enterEditMode()
        vm.onEditToneChange("")
        vm.onEditInstrumentChange(" ")
        vm.onEditContentChange("   ")
        advanceUntilIdle()

        vm.saveEdit()
        advanceUntilIdle()

        assertEquals(listOf("tom", "instrumento", "cifra"), vm.uiState.value.editMissing)
        coVerify(exactly = 0) { chordChartRepository.updateChordChart(any(), any(), any(), any()) }
    }

    // endregion

    // region scrollMode

    @Test
    fun `scrollMode is unknown until the preference is read, then is the saved mode`() = runTest {
        val saved = MutableSharedFlow<SongScrollMode>()
        every { themePreferences.songScrollModeFlow } returns saved
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Loading)
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Loading)

        val vm = createViewModel()

        advanceUntilIdle()
        assertNull(vm.scrollMode.value)

        saved.emit(SongScrollMode.VERTICAL)
        advanceUntilIdle()
        assertEquals(SongScrollMode.VERTICAL, vm.scrollMode.value)
    }

    @Test
    fun `toggleScrollMode switches from HORIZONTAL to VERTICAL`() = runTest {
        val scrollFlow = MutableStateFlow(SongScrollMode.HORIZONTAL)
        every { themePreferences.songScrollModeFlow } returns scrollFlow
        coEvery { themePreferences.setSongScrollMode(SongScrollMode.VERTICAL) } answers {
            scrollFlow.value = SongScrollMode.VERTICAL
        }
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Loading)
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Loading)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.toggleScrollMode()
        advanceUntilIdle()

        coVerify { themePreferences.setSongScrollMode(SongScrollMode.VERTICAL) }
    }

    // endregion
}
