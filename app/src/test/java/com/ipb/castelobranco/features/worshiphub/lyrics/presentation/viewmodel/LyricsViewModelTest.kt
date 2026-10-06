package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.viewmodel

import com.ipb.castelobranco.core.data.local.SetlistPreferences
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.FakeSundaySetlistRepository
import com.ipb.castelobranco.core.testing.setlistOf
import com.ipb.castelobranco.core.domain.setlist.ObserveSundaySetlistUseCase
import com.ipb.castelobranco.core.domain.setlist.SyncSundaySetlistUseCase
import com.ipb.castelobranco.core.domain.util.DateProvider
import java.time.LocalDate
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.model.Lyrics
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.usecase.GetLyricsUseCase
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
class LyricsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getLyricsUseCase: GetLyricsUseCase
    private lateinit var songsRepository: SongsRepository
    private lateinit var setlistPreferences: SetlistPreferences
    private val accessRepository = FakeAccessRepository()
    private val sundaySetlist = FakeSundaySetlistRepository()
    private val observeSundaySetlist = ObserveSundaySetlistUseCase(sundaySetlist, DateProvider { TODAY })
    private val syncSundaySetlist: SyncSundaySetlistUseCase = mockk {
        coEvery { this@mockk.invoke() } returns Result.success(Unit)
    }
    private lateinit var viewModel: LyricsViewModel

    private val fakeSongs = listOf(
        Song(id = 1, title = "Oceans", artist = "Hillsong", categoryName = "Louvor"),
        Song(id = 2, title = "Way Maker", artist = "Sinach", categoryName = "Adoração"),
    )
    private val fakeLyrics = listOf(
        Lyrics(id = 10, songId = 1, content = "You call me out upon the waters..."),
        Lyrics(id = 11, songId = 2, content = "You are here moving in our midst..."),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getLyricsUseCase = mockk()
        songsRepository = mockk()
        setlistPreferences = mockk()

        every { getLyricsUseCase.observe() } returns flowOf(SnapshotState.Loading)
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Loading)
        every { setlistPreferences.pinnedSongIds } returns flowOf(emptyList())
        coEvery { getLyricsUseCase.refresh() } returns RefreshResult.Updated
        coEvery { setlistPreferences.toggleSong(any()) } returns Unit

        viewModel = LyricsViewModel(
            getLyricsUseCase, songsRepository, setlistPreferences, ObserveAccessUseCase(accessRepository),
            observeSundaySetlist, syncSundaySetlist,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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
        every { getLyricsUseCase.observe() } returns
            flowOf(SnapshotState.Error(AppError.Network(message = "fetch failed")))
        viewModel = LyricsViewModel(
            getLyricsUseCase, songsRepository, setlistPreferences, ObserveAccessUseCase(accessRepository),
            observeSundaySetlist, syncSundaySetlist,
        )

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
    fun `uiState populates lyrics with song names resolved from songsRepository`() = runTest {
        every { getLyricsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeLyrics))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = LyricsViewModel(
            getLyricsUseCase, songsRepository, setlistPreferences, ObserveAccessUseCase(accessRepository),
            observeSundaySetlist, syncSundaySetlist,
        )

        subscribeAndAdvance()

        val lyrics = viewModel.uiState.value.lyrics
        assertEquals(2, lyrics.size)
        assertEquals("Oceans", lyrics.first { it.id == 10 }.songName)
        assertEquals("Way Maker", lyrics.first { it.id == 11 }.songName)
    }

    @Test
    fun `uiState uses fallback song name when song is not found`() = runTest {
        val lyricsWithUnknownSong = listOf(Lyrics(id = 20, songId = 99, content = "..."))
        every { getLyricsUseCase.observe() } returns flowOf(SnapshotState.Data(lyricsWithUnknownSong))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = LyricsViewModel(
            getLyricsUseCase, songsRepository, setlistPreferences, ObserveAccessUseCase(accessRepository),
            observeSundaySetlist, syncSundaySetlist,
        )

        subscribeAndAdvance()

        assertEquals("Song #99", viewModel.uiState.value.lyrics.first().songName)
    }

    @Test
    fun `uiState sorts pinned lyrics first`() = runTest {
        every { getLyricsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeLyrics))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        every { setlistPreferences.pinnedSongIds } returns flowOf(listOf(2))
        viewModel = LyricsViewModel(
            getLyricsUseCase, songsRepository, setlistPreferences, ObserveAccessUseCase(accessRepository),
            observeSundaySetlist, syncSundaySetlist,
        )

        subscribeAndAdvance()

        val lyrics = viewModel.uiState.value.lyrics
        assertTrue(lyrics.first().isPinned)
        assertEquals(11, lyrics.first().id)
    }

    @Test
    fun `uiState orders pinned lyrics by pin insertion order`() = runTest {
        val lyrics = listOf(
            Lyrics(id = 1, songId = 1, content = "..."),
            Lyrics(id = 2, songId = 2, content = "..."),
            Lyrics(id = 3, songId = 1, content = "..."),
        )
        every { getLyricsUseCase.observe() } returns flowOf(SnapshotState.Data(lyrics))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        every { setlistPreferences.pinnedSongIds } returns flowOf(listOf(1, 2))
        viewModel = LyricsViewModel(
            getLyricsUseCase, songsRepository, setlistPreferences, ObserveAccessUseCase(accessRepository),
            observeSundaySetlist, syncSundaySetlist,
        )

        subscribeAndAdvance()

        val result = viewModel.uiState.value.lyrics
        // songId=1 (pinOrder=0) → lyrics 1,3; songId=2 (pinOrder=1) → lyrics 2
        assertEquals(listOf(1, 3, 2), result.map { it.id })
    }

    // endregion

    // region onQueryChange

    @Test
    fun `onQueryChange updates query in uiState`() = runTest {
        every { getLyricsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeLyrics))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = LyricsViewModel(
            getLyricsUseCase, songsRepository, setlistPreferences, ObserveAccessUseCase(accessRepository),
            observeSundaySetlist, syncSundaySetlist,
        )

        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onQueryChange("Oceans")
        advanceUntilIdle()
        job.cancel()

        assertEquals("Oceans", viewModel.uiState.value.query)
    }

    @Test
    fun `onQueryChange filters lyrics by song name case-insensitively`() = runTest {
        every { getLyricsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeLyrics))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = LyricsViewModel(
            getLyricsUseCase, songsRepository, setlistPreferences, ObserveAccessUseCase(accessRepository),
            observeSundaySetlist, syncSundaySetlist,
        )

        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onQueryChange("oceans")
        advanceUntilIdle()
        job.cancel()

        val filtered = viewModel.uiState.value.filteredLyrics
        assertEquals(1, filtered.size)
        assertEquals(10, filtered.first().id)
    }

    @Test
    fun `onQueryChange matches accented song name with unaccented query`() = runTest {
        val accentedSongs = listOf(
            Song(id = 1, title = "Oceanos da Graça", artist = "Hillsong", categoryName = "Louvor"),
            Song(id = 2, title = "Way Maker", artist = "Sinach", categoryName = "Adoração"),
        )
        every { getLyricsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeLyrics))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(accentedSongs))
        viewModel = LyricsViewModel(
            getLyricsUseCase, songsRepository, setlistPreferences, ObserveAccessUseCase(accessRepository),
            observeSundaySetlist, syncSundaySetlist,
        )

        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onQueryChange("graca")
        advanceUntilIdle()
        job.cancel()

        val filtered = viewModel.uiState.value.filteredLyrics
        assertEquals(1, filtered.size)
        assertEquals(10, filtered.first().id)
    }

    @Test
    fun `onQueryChange with blank query returns all lyrics`() = runTest {
        every { getLyricsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeLyrics))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = LyricsViewModel(
            getLyricsUseCase, songsRepository, setlistPreferences, ObserveAccessUseCase(accessRepository),
            observeSundaySetlist, syncSundaySetlist,
        )

        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onQueryChange("")
        advanceUntilIdle()
        job.cancel()

        assertEquals(2, viewModel.uiState.value.filteredLyrics.size)
    }

    // endregion

    // region onTogglePin

    @Test
    fun `onTogglePin delegates to setlistPreferences toggleLyrics`() = runTest {
        viewModel.onTogglePin(10)
        advanceUntilIdle()
        coVerify { setlistPreferences.toggleSong(10) }
    }

    // endregion

    // region refresh

    @Test
    fun `refresh calls getLyricsUseCase refresh`() = runTest {
        viewModel.refresh(minDurationMs = 0L)
        advanceUntilIdle()
        coVerify { getLyricsUseCase.refresh() }
    }

    @Test
    fun `refresh also re-reads the Sunday setlist`() = runTest {
        viewModel.refresh(minDurationMs = 0L)
        advanceUntilIdle()
        coVerify(exactly = 1) { syncSundaySetlist() }
    }

    @Test
    fun `a failed setlist read does not break the refresh`() = runTest {
        coEvery { syncSundaySetlist() } returns Result.failure(IllegalStateException("offline"))
        viewModel.refresh(minDurationMs = 0L)
        advanceUntilIdle()
        coVerify { getLyricsUseCase.refresh() }
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `isRefreshing is false after refresh completes`() = runTest {
        viewModel.refresh(minDurationMs = 0L)
        advanceUntilIdle()
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `refresh guard prevents concurrent refreshes`() = runTest {
        coEvery { getLyricsUseCase.refresh() } coAnswers {
            kotlinx.coroutines.delay(5_000); RefreshResult.Updated
        }
        viewModel.refresh() // schedules coroutine
        runCurrent() // advance to where _isRefreshing = true
        viewModel.refresh() // guarded
        advanceUntilIdle()
        coVerify(exactly = 1) { getLyricsUseCase.refresh() }
    }

    // endregion

    // region Sunday section

    @Test
    fun `the Sunday setlist shows above the list, in order, and is not repeated below`() = runTest {
        every { getLyricsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeLyrics))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        sundaySetlist.state.value = setlistOf(TODAY, 2, 99, 1)
        viewModel = LyricsViewModel(
            getLyricsUseCase, songsRepository, setlistPreferences, ObserveAccessUseCase(accessRepository),
            observeSundaySetlist, syncSundaySetlist,
        )

        subscribeAndAdvance()

        val section = viewModel.uiState.value.sundaySection!!
        assertEquals(listOf(11, 10), section.entries.map { it.contentId })
        assertEquals("G", section.entries.first().tone)
        assertTrue(viewModel.uiState.value.filteredLyrics.isEmpty())
    }

    @Test
    fun `a search hides the section and finds its songs in the list`() = runTest {
        every { getLyricsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeLyrics))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        sundaySetlist.state.value = setlistOf(TODAY, 1)
        viewModel = LyricsViewModel(
            getLyricsUseCase, songsRepository, setlistPreferences, ObserveAccessUseCase(accessRepository),
            observeSundaySetlist, syncSundaySetlist,
        )

        viewModel.onQueryChange("oce")
        subscribeAndAdvance()

        assertNull(viewModel.uiState.value.sundaySection)
        assertEquals(listOf(10), viewModel.uiState.value.filteredLyrics.map { it.id })
    }

    @Test
    fun `a setlist of a past Sunday is not shown`() = runTest {
        every { getLyricsUseCase.observe() } returns flowOf(SnapshotState.Data(fakeLyrics))
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        sundaySetlist.state.value = setlistOf(TODAY.minusDays(1), 1)
        viewModel = LyricsViewModel(
            getLyricsUseCase, songsRepository, setlistPreferences, ObserveAccessUseCase(accessRepository),
            observeSundaySetlist, syncSundaySetlist,
        )

        subscribeAndAdvance()

        assertNull(viewModel.uiState.value.sundaySection)
        assertEquals(2, viewModel.uiState.value.filteredLyrics.size)
    }

    // endregion

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 4)
    }
}
