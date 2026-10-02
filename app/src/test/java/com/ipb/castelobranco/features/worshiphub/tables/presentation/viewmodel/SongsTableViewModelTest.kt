package com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SuggestedSong
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySet
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySetItem
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopSong
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopTone
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongsTableViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SongsRepository
    private lateinit var viewModel: SongsTableViewModel

    private val fakeSongs = listOf(
        Song(id = 1, title = "Oceans", artist = "Hillsong", categoryName = "Louvor"),
        Song(id = 2, title = "Way Maker", artist = "Sinach", categoryName = "Adoração"),
    )
    private val fakeSundays = listOf(
        SundaySet(date = "07/04/2024", songs = listOf(SundaySetItem(1, "Oceans", "Hillsong", "D")))
    )
    private val fakeTopSongs = listOf(TopSong(title = "Oceans", playCount = 10))
    private val fakeTopTones = listOf(TopTone(tone = "D", count = 8))
    private val fakeSuggested = listOf(
        SuggestedSong(id = 1, songId = 1, title = "Oceans", artist = "Hillsong", date = "07/04/2024", tone = "D", position = 1)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()

        every { repository.observeAllSongs() } returns flowOf(SnapshotState.Loading)
        every { repository.observeSongsBySunday() } returns flowOf(SnapshotState.Loading)
        every { repository.observeTopSongs() } returns flowOf(SnapshotState.Loading)
        every { repository.observeTopTones() } returns flowOf(SnapshotState.Loading)
        every { repository.observeSuggestedSongs() } returns flowOf(SnapshotState.Loading)
        coEvery { repository.refreshSongsBySunday() } returns RefreshResult.Updated
        coEvery { repository.refreshTopSongs() } returns RefreshResult.Updated
        coEvery { repository.refreshTopTones() } returns RefreshResult.Updated
        coEvery { repository.refreshAllSongs() } returns RefreshResult.Updated
        coEvery { repository.refreshSuggestedSongs(any<Map<Int, Int>>()) } returns RefreshResult.Updated

        viewModel = songsTableViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region allSongs

    @Test
    fun `allSongs emits empty list when state is Loading`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.allSongs.value.isEmpty())
    }

    @Test
    fun `allSongs emits list when state is Data`() = runTest {
        every { repository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = songsTableViewModel(repository)

        val job = launch { viewModel.allSongs.collect { } }
        advanceUntilIdle()
        job.cancel()

        assertEquals(fakeSongs, viewModel.allSongs.value)
    }

    @Test
    fun `allSongs emits empty list when state is Error`() = runTest {
        every { repository.observeAllSongs() } returns flowOf(SnapshotState.Error(AppError.Unknown()))
        viewModel = songsTableViewModel(repository)

        val job = launch { viewModel.allSongs.collect { } }
        advanceUntilIdle()
        job.cancel()

        assertTrue(viewModel.allSongs.value.isEmpty())
    }

    // endregion

    // region lastSundays

    @Test
    fun `lastSundays emits Loading when state is Loading`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.lastSundays.value is SnapshotState.Loading)
    }

    @Test
    fun `lastSundays emits list when state is Data`() = runTest {
        every { repository.observeSongsBySunday() } returns flowOf(SnapshotState.Data(fakeSundays))
        viewModel = songsTableViewModel(repository)

        val job = launch { viewModel.lastSundays.collect { } }
        advanceUntilIdle()
        job.cancel()

        assertEquals(SnapshotState.Data(fakeSundays), viewModel.lastSundays.value)
    }

    // endregion

    // region topSongs

    @Test
    fun `topSongs emits Loading when state is Loading`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.topSongs.value is SnapshotState.Loading)
    }

    @Test
    fun `topSongs emits list when state is Data`() = runTest {
        every { repository.observeTopSongs() } returns flowOf(SnapshotState.Data(fakeTopSongs))
        viewModel = songsTableViewModel(repository)

        val job = launch { viewModel.topSongs.collect { } }
        advanceUntilIdle()
        job.cancel()

        assertEquals(SnapshotState.Data(fakeTopSongs), viewModel.topSongs.value)
    }

    // endregion

    // region topTones

    @Test
    fun `topTones emits Loading when state is Loading`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.topTones.value is SnapshotState.Loading)
    }

    @Test
    fun `topTones emits list when state is Data`() = runTest {
        every { repository.observeTopTones() } returns flowOf(SnapshotState.Data(fakeTopTones))
        viewModel = songsTableViewModel(repository)

        val job = launch { viewModel.topTones.collect { } }
        advanceUntilIdle()
        job.cancel()

        assertEquals(SnapshotState.Data(fakeTopTones), viewModel.topTones.value)
    }

    // endregion

    // region suggestedSongs

    @Test
    fun `suggestedSongs emits Loading when state is Loading`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.suggestedSongs.value is SnapshotState.Loading)
    }

    @Test
    fun `suggestedSongs emits list when state is Data`() = runTest {
        every { repository.observeSuggestedSongs() } returns flowOf(SnapshotState.Data(fakeSuggested))
        viewModel = songsTableViewModel(repository)

        val job = launch { viewModel.suggestedSongs.collect { } }
        advanceUntilIdle()
        job.cancel()

        assertEquals(SnapshotState.Data(fakeSuggested), viewModel.suggestedSongs.value)
    }

    // endregion

    // region refreshCurrentTab

    @Test
    fun `refreshCurrentTab tab 0 calls refreshSongsBySunday`() = runTest {
        viewModel.refreshCurrentTab(tabIndex = 0, minDurationMs = 0L)
        advanceUntilIdle()
        coVerify { repository.refreshSongsBySunday() }
    }

    @Test
    fun `refreshCurrentTab tab 1 calls refreshTopSongs`() = runTest {
        viewModel.refreshCurrentTab(tabIndex = 1, minDurationMs = 0L)
        advanceUntilIdle()
        coVerify { repository.refreshTopSongs() }
    }

    @Test
    fun `refreshCurrentTab tab 2 calls refreshTopTones`() = runTest {
        viewModel.refreshCurrentTab(tabIndex = 2, minDurationMs = 0L)
        advanceUntilIdle()
        coVerify { repository.refreshTopTones() }
    }

    @Test
    fun `refreshCurrentTab tab 3 does not call any repository method`() = runTest {
        viewModel.refreshCurrentTab(tabIndex = 3, minDurationMs = 0L)
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.refreshTopSongs() }
        coVerify(exactly = 0) { repository.refreshTopTones() }
        coVerify(exactly = 0) { repository.refreshSongsBySunday() }
    }

    @Test
    fun `isRefreshing is false after refreshCurrentTab completes`() = runTest {
        viewModel.refreshCurrentTab(tabIndex = 0, minDurationMs = 0L)
        advanceUntilIdle()
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `refreshCurrentTab guard prevents concurrent refresh`() = runTest {
        coEvery { repository.refreshSongsBySunday() } coAnswers {
            kotlinx.coroutines.delay(5_000); RefreshResult.Updated
        }
        viewModel.refreshCurrentTab(tabIndex = 0) // schedules coroutine
        runCurrent() // run it up to the delay — sets _isRefreshing = true
        viewModel.refreshCurrentTab(tabIndex = 0) // guarded
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.refreshSongsBySunday() }
    }

    // endregion

    // region selectSong

    @Test
    fun `repertoireRows starts with 4 empty rows`() = runTest {
        val rows = viewModel.repertoireRows.value
        assertEquals(4, rows.size)
        assertEquals(listOf(1, 2, 3, 4), rows.map { it.position })
        assertTrue(rows.all { it.selectedSong == null && it.tone.isBlank() && !it.isFixed })
    }

    @Test
    fun `selectSong sets the song at the given position`() = runTest {
        val song = fakeSongs[0]
        viewModel.selectSong(position = 1, song = song)
        val rows = viewModel.repertoireRows.value
        assertEquals(song, rows.first { it.position == 1 }.selectedSong)
    }

    @Test
    fun `selectSong with null clears the song at the given position`() = runTest {
        val song = fakeSongs[0]
        viewModel.selectSong(position = 1, song = song)
        viewModel.selectSong(position = 1, song = null)
        assertEquals(null, viewModel.repertoireRows.value.first { it.position == 1 }.selectedSong)
    }

    @Test
    fun `selectSong only affects the targeted position`() = runTest {
        val song = fakeSongs[0]
        viewModel.selectSong(position = 2, song = song)
        val rows = viewModel.repertoireRows.value
        assertEquals(null, rows.first { it.position == 1 }.selectedSong)
        assertEquals(song, rows.first { it.position == 2 }.selectedSong)
        assertEquals(null, rows.first { it.position == 3 }.selectedSong)
        assertEquals(null, rows.first { it.position == 4 }.selectedSong)
    }

    @Test
    fun `selectSong with null also clears isFixed`() = runTest {
        val song = fakeSongs[0]
        viewModel.selectSong(position = 1, song = song)
        viewModel.toggleFixed(position = 1)
        assertTrue(viewModel.repertoireRows.value.first { it.position == 1 }.isFixed)

        viewModel.selectSong(position = 1, song = null)
        assertFalse(viewModel.repertoireRows.value.first { it.position == 1 }.isFixed)
    }

    @Test
    fun `selectSong with a different song keeps isFixed`() = runTest {
        viewModel.selectSong(position = 1, song = fakeSongs[0])
        viewModel.toggleFixed(position = 1)
        viewModel.selectSong(position = 1, song = fakeSongs[1])
        val row = viewModel.repertoireRows.value.first { it.position == 1 }
        assertEquals(fakeSongs[1], row.selectedSong)
        assertTrue(row.isFixed)
    }

    @Test
    fun `selectSong fills tone with the most used tone from lastSundays`() = runTest {
        val sundays = listOf(
            SundaySet("01/04/2024", listOf(SundaySetItem(1, "Oceans", "Hillsong", "D"))),
            SundaySet("08/04/2024", listOf(SundaySetItem(1, "Oceans", "Hillsong", "D"))),
            SundaySet("15/04/2024", listOf(SundaySetItem(1, "Oceans", "Hillsong", "G"))),
        )
        every { repository.observeSongsBySunday() } returns flowOf(SnapshotState.Data(sundays))
        viewModel = songsTableViewModel(repository)
        val job = launch { viewModel.lastSundays.collect { } }
        advanceUntilIdle()

        viewModel.selectSong(position = 1, song = fakeSongs[0])

        assertEquals("D", viewModel.repertoireRows.value.first { it.position == 1 }.tone)
        job.cancel()
    }

    @Test
    fun `selectSong leaves tone empty when song has no history`() = runTest {
        val sundays = listOf(
            SundaySet("01/04/2024", listOf(SundaySetItem(1, "Oceans", "Hillsong", "D")))
        )
        every { repository.observeSongsBySunday() } returns flowOf(SnapshotState.Data(sundays))
        viewModel = songsTableViewModel(repository)
        val job = launch { viewModel.lastSundays.collect { } }
        advanceUntilIdle()

        viewModel.selectSong(position = 1, song = fakeSongs[1]) // Way Maker, no history

        assertEquals("", viewModel.repertoireRows.value.first { it.position == 1 }.tone)
        job.cancel()
    }

    @Test
    fun `selectSong overwrites manual tone when song changes`() = runTest {
        val sundays = listOf(
            SundaySet("01/04/2024", listOf(
                SundaySetItem(1, "Oceans", "Hillsong", "D"),
                SundaySetItem(2, "Way Maker", "Sinach", "G"),
            ))
        )
        every { repository.observeSongsBySunday() } returns flowOf(SnapshotState.Data(sundays))
        viewModel = songsTableViewModel(repository)
        val job = launch { viewModel.lastSundays.collect { } }
        advanceUntilIdle()

        viewModel.selectSong(position = 1, song = fakeSongs[0]) // tone = "D"
        viewModel.onToneChange(position = 1, tone = "E")        // user edits to "E"
        viewModel.selectSong(position = 1, song = fakeSongs[1]) // switch song

        assertEquals("G", viewModel.repertoireRows.value.first { it.position == 1 }.tone)
        job.cancel()
    }

    // endregion

    // region onToneChange

    @Test
    fun `onToneChange updates tone without touching song or isFixed`() = runTest {
        viewModel.selectSong(position = 1, song = fakeSongs[0])
        viewModel.toggleFixed(position = 1)

        viewModel.onToneChange(position = 1, tone = "F")

        val row = viewModel.repertoireRows.value.first { it.position == 1 }
        assertEquals("F", row.tone)
        assertEquals(fakeSongs[0], row.selectedSong)
        assertTrue(row.isFixed)
    }

    @Test
    fun `onToneChange only affects the targeted position`() = runTest {
        viewModel.selectSong(position = 1, song = fakeSongs[0])
        viewModel.selectSong(position = 2, song = fakeSongs[1])

        viewModel.onToneChange(position = 2, tone = "A")

        assertEquals("", viewModel.repertoireRows.value.first { it.position == 1 }.tone)
        assertEquals("A", viewModel.repertoireRows.value.first { it.position == 2 }.tone)
    }

    // endregion

    // region toggleFixed

    @Test
    fun `toggleFixed sets isFixed to true when row has a selected song`() = runTest {
        viewModel.selectSong(position = 1, song = fakeSongs[0])
        viewModel.toggleFixed(position = 1)
        assertTrue(viewModel.repertoireRows.value.first { it.position == 1 }.isFixed)
    }

    @Test
    fun `toggleFixed twice clears isFixed`() = runTest {
        viewModel.selectSong(position = 1, song = fakeSongs[0])
        viewModel.toggleFixed(position = 1)
        viewModel.toggleFixed(position = 1)
        assertFalse(viewModel.repertoireRows.value.first { it.position == 1 }.isFixed)
    }

    @Test
    fun `toggleFixed is a no-op when no song is selected`() = runTest {
        viewModel.toggleFixed(position = 1)
        assertFalse(viewModel.repertoireRows.value.first { it.position == 1 }.isFixed)
    }

    // endregion

    // region refreshSuggestedSongs

    @Test
    fun `refreshSuggestedSongs sends only fixed rows in the fixed map`() = runTest {
        viewModel.selectSong(position = 1, song = fakeSongs[0])
        viewModel.toggleFixed(position = 1)
        viewModel.selectSong(position = 2, song = fakeSongs[1]) // not fixed
        val expectedFixed = mapOf(1 to fakeSongs[0].id)

        viewModel.refreshSuggestedSongs(minDurationMs = 0L)
        advanceUntilIdle()

        coVerify { repository.refreshSuggestedSongs(expectedFixed) }
    }

    @Test
    fun `refreshSuggestedSongs sends empty fixed map when nothing is fixed`() = runTest {
        viewModel.selectSong(position = 1, song = fakeSongs[0]) // selected but not fixed
        viewModel.refreshSuggestedSongs(minDurationMs = 0L)
        advanceUntilIdle()
        coVerify { repository.refreshSuggestedSongs(emptyMap()) }
    }

    @Test
    fun `refreshSuggestedSongs syncs unfixed rows from API response`() = runTest {
        every { repository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        every { repository.observeSuggestedSongs() } returns flowOf(SnapshotState.Data(fakeSuggested))
        viewModel = songsTableViewModel(repository)

        viewModel.refreshSuggestedSongs(minDurationMs = 0L)
        advanceUntilIdle()

        val rows = viewModel.repertoireRows.value
        val pos1 = rows.first { it.position == 1 }
        assertEquals(fakeSongs[0], pos1.selectedSong)
        assertEquals("D", pos1.tone)
    }

    @Test
    fun `refreshSuggestedSongs does not overwrite fixed rows`() = runTest {
        every { repository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        every { repository.observeSuggestedSongs() } returns flowOf(SnapshotState.Data(fakeSuggested))
        viewModel = songsTableViewModel(repository)

        viewModel.selectSong(position = 1, song = fakeSongs[1])
        viewModel.toggleFixed(position = 1)

        viewModel.refreshSuggestedSongs(minDurationMs = 0L)
        advanceUntilIdle()

        val pos1 = viewModel.repertoireRows.value.first { it.position == 1 }
        assertEquals(fakeSongs[1], pos1.selectedSong)
        assertEquals("", pos1.tone)
        assertTrue(pos1.isFixed)
    }

    @Test
    fun `isRefreshingSuggestedSongs is false after refresh completes`() = runTest {
        viewModel.refreshSuggestedSongs(minDurationMs = 0L)
        advanceUntilIdle()
        assertFalse(viewModel.isRefreshingSuggestedSongs.value)
    }

    @Test
    fun `refreshSuggestedSongs guard prevents concurrent refresh`() = runTest {
        coEvery { repository.refreshSuggestedSongs(any<Map<Int, Int>>()) } coAnswers {
            kotlinx.coroutines.delay(5_000); RefreshResult.Updated
        }
        viewModel.refreshSuggestedSongs() // schedules coroutine
        runCurrent() // advance to where _isRefreshingSuggestedSongs = true
        viewModel.refreshSuggestedSongs() // guarded
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.refreshSuggestedSongs(any<Map<Int, Int>>()) }
    }

    // endregion

}
