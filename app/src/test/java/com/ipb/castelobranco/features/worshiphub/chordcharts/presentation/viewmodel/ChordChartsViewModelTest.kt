package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.viewmodel

import com.ipb.castelobranco.core.data.local.SetlistPreferences
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.model.ChordChart
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
class ChordChartsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getChordChartsUseCase: GetChordChartsUseCase
    private lateinit var songsRepository: SongsRepository
    private lateinit var setlistPreferences: SetlistPreferences
    private val accessRepository = FakeAccessRepository()
    private lateinit var viewModel: ChordChartsViewModel

    private val fakeSongs = listOf(
        Song(id = 1, title = "Oceans", artist = "Hillsong", categoryName = "Louvor"),
        Song(id = 2, title = "Way Maker", artist = "Sinach", categoryName = "Adoração"),
    )
    private val fakeCharts = listOf(
        ChordChart(id = 10, songId = 1, content = "...", tone = "D", instrument = "violão"),
        ChordChart(id = 11, songId = 2, content = "...", tone = "G", instrument = "teclado"),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getChordChartsUseCase = mockk()
        songsRepository = mockk()
        setlistPreferences = mockk()

        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Loading)
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Loading)
        every { setlistPreferences.pinnedSongIds } returns flowOf(emptyList())
        coEvery { getChordChartsUseCase.refresh() } returns RefreshResult.Updated
        coEvery { setlistPreferences.toggleSong(any()) } returns Unit

        viewModel = newViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = ChordChartsViewModel(
        getChordChartsUseCase,
        songsRepository,
        setlistPreferences,
        ObserveAccessUseCase(accessRepository),
    )

    // region uiState — edit buttons follow `songs` (spec 006)

    @Test
    fun `canEdit needs manage on songs`() = runTest {
        subscribeAndAdvance()
        assertFalse(viewModel.uiState.value.canEdit)

        accessRepository.state.value = accessOf(Role.LEADER, Scope.SONGS to AccessLevel.MANAGE)
        subscribeAndAdvance()
        assertTrue(viewModel.uiState.value.canEdit)

        accessRepository.state.value = accessOf(Role.MEDIA, Scope.GALLERY to AccessLevel.MANAGE)
        subscribeAndAdvance()
        assertFalse(viewModel.uiState.value.canEdit)
    }

    // endregion

    // Helper: subscribe so WhileSubscribed upstream activates, then advance and cancel
    private fun TestScope.subscribeAndAdvance() {
        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        job.cancel()
    }

    // region uiState — Loading

    @Test
    fun `uiState initial value has isLoading true`() = runTest {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `uiState is loading when observe emits Loading`() = runTest {
        subscribeAndAdvance()
        assertTrue(viewModel.uiState.value.isLoading)
    }

    // endregion

    // region uiState — Error

    @Test
    fun `uiState sets error when observe emits Error`() = runTest {
        every { getChordChartsUseCase.observe() } returns
            flowOf(SnapshotState.Error(AppError.Network(message = "network error")))
        viewModel = newViewModel()

        subscribeAndAdvance()

        assertEquals(
            "Sem conexão com a internet. Verifique sua rede e tente novamente.",
            viewModel.uiState.value.error,
        )
    }

    @Test
    fun `uiState error is null when observe emits Loading`() = runTest {
        subscribeAndAdvance()
        assertNull(viewModel.uiState.value.error)
    }

    // endregion

    // region uiState — Data

    @Test
    fun `uiState populates charts with song names resolved from songsRepository`() = runTest {
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeCharts))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = newViewModel()

        subscribeAndAdvance()

        val charts = viewModel.uiState.value.charts
        assertEquals(2, charts.size)
        assertEquals("Oceans", charts.first { it.id == 10 }.songName)
        assertEquals("Way Maker", charts.first { it.id == 11 }.songName)
    }

    @Test
    fun `uiState uses fallback song name when song is not found`() = runTest {
        val chartsWithUnknownSong = listOf(ChordChart(id = 20, songId = 99, content = "...", tone = "A", instrument = "violão"))
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Data(chartsWithUnknownSong))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = newViewModel()

        subscribeAndAdvance()

        assertEquals("Song #99", viewModel.uiState.value.charts.first().songName)
    }

    @Test
    fun `uiState sorts pinned charts first`() = runTest {
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeCharts))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        every { setlistPreferences.pinnedSongIds } returns flowOf(listOf(2))
        viewModel = newViewModel()

        subscribeAndAdvance()

        val charts = viewModel.uiState.value.charts
        assertTrue(charts.first().isPinned)
        assertEquals(11, charts.first().id)
    }

    @Test
    fun `uiState orders pinned charts by pin insertion order`() = runTest {
        val charts = listOf(
            ChordChart(id = 1, songId = 1, content = "...", tone = "C", instrument = "violão"),
            ChordChart(id = 2, songId = 2, content = "...", tone = "D", instrument = "violão"),
            ChordChart(id = 3, songId = 1, content = "...", tone = "E", instrument = "violão"),
        )
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Data(charts))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        every { setlistPreferences.pinnedSongIds } returns flowOf(listOf(1, 2))
        viewModel = newViewModel()

        subscribeAndAdvance()

        val result = viewModel.uiState.value.charts
        // songId=1 (pinOrder=0) → charts 1,3; songId=2 (pinOrder=1) → chart 2
        assertEquals(listOf(1, 3, 2), result.map { it.id })
    }

    // endregion

    // region onQueryChange

    @Test
    fun `onQueryChange updates query in uiState`() = runTest {
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeCharts))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = newViewModel()

        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onQueryChange("Oceans")
        advanceUntilIdle()
        job.cancel()

        assertEquals("Oceans", viewModel.uiState.value.query)
    }

    @Test
    fun `onQueryChange filters charts by song name case-insensitively`() = runTest {
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeCharts))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = newViewModel()

        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onQueryChange("oceans")
        advanceUntilIdle()
        job.cancel()

        val filtered = viewModel.uiState.value.filteredCharts
        assertEquals(1, filtered.size)
        assertEquals(10, filtered.first().id)
    }

    @Test
    fun `onQueryChange matches accented song name with unaccented query`() = runTest {
        val accentedSongs = listOf(
            Song(id = 1, title = "Oceanos da Graça", artist = "Hillsong", categoryName = "Louvor"),
            Song(id = 2, title = "Way Maker", artist = "Sinach", categoryName = "Adoração"),
        )
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeCharts))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(accentedSongs))
        viewModel = newViewModel()

        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onQueryChange("graca")
        advanceUntilIdle()
        job.cancel()

        val filtered = viewModel.uiState.value.filteredCharts
        assertEquals(1, filtered.size)
        assertEquals(10, filtered.first().id)
    }

    @Test
    fun `onQueryChange with blank query returns all charts`() = runTest {
        every { getChordChartsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeCharts))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = newViewModel()

        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onQueryChange("")
        advanceUntilIdle()
        job.cancel()

        assertEquals(2, viewModel.uiState.value.filteredCharts.size)
    }

    // endregion

    // region onTogglePin

    @Test
    fun `onTogglePin delegates to setlistPreferences toggleChordChart`() = runTest {
        viewModel.onTogglePin(10)
        advanceUntilIdle()
        coVerify { setlistPreferences.toggleSong(10) }
    }

    // endregion

    // region refresh

    @Test
    fun `refresh calls getChordChartsUseCase refresh`() = runTest {
        viewModel.refresh(minDurationMs = 0L)
        advanceUntilIdle()
        coVerify { getChordChartsUseCase.refresh() }
    }

    @Test
    fun `isRefreshing is false after refresh completes`() = runTest {
        viewModel.refresh(minDurationMs = 0L)
        advanceUntilIdle()
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `refresh guard prevents concurrent refreshes`() = runTest {
        coEvery { getChordChartsUseCase.refresh() } coAnswers {
            kotlinx.coroutines.delay(5_000); RefreshResult.Updated
        }
        viewModel.refresh() // schedules coroutine
        runCurrent() // advance to where _isRefreshing = true
        viewModel.refresh() // guarded
        advanceUntilIdle()
        coVerify(exactly = 1) { getChordChartsUseCase.refresh() }
    }

    // endregion
}
