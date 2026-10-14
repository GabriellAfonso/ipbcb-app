package com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel

import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SuggestedSong
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** "Gerar" sends `exclude` with the songs already shown in each free position. */
@OptIn(ExperimentalCoroutinesApi::class)
class SongsTableViewModelExcludeTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SongsRepository
    private lateinit var viewModel: SongsTableViewModel

    private val songs = (1..20).map { Song(id = it, title = "Song $it", artist = "Artist", categoryName = "Louvor") }
    private val suggestions = MutableStateFlow<SnapshotState<List<SuggestedSong>>>(SnapshotState.Loading)

    /** Each refresh answers with the next response in line; the requests are recorded in [sentExcluded]. */
    private val responses = ArrayDeque<List<SuggestedSong>>()
    private val sentFixed = mutableListOf<Map<Int, Int>>()
    private val sentExcluded = mutableListOf<Map<Int, Set<Int>>>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        every { repository.observeAllSongs() } returns flowOf(SnapshotState.Data(songs))
        every { repository.observeSongsBySunday() } returns flowOf(SnapshotState.Loading)
        every { repository.observeTopSongs() } returns flowOf(SnapshotState.Loading)
        every { repository.observeTopTones() } returns flowOf(SnapshotState.Loading)
        every { repository.observeSuggestedSongs() } returns suggestions
        coEvery { repository.refreshSuggestedSongs(any(), any()) } answers {
            sentFixed += firstArg<Map<Int, Int>>()
            sentExcluded += secondArg<Map<Int, Set<Int>>>()
            suggestions.value = SnapshotState.Data(responses.removeFirst())
            RefreshResult.Updated
        }
        viewModel = songsTableViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun playedIdOf(songId: Int) = 1000 + songId

    /** One suggestion per position; [songIds] lists the song id for positions 1 to 4. */
    private fun response(vararg songIds: Int) = songIds.mapIndexed { index, songId ->
        SuggestedSong(
            id = playedIdOf(songId), songId = songId, title = "Song $songId", artist = "Artist",
            date = "01/01/2025", tone = "D", position = index + 1,
        )
    }

    private fun TestScope.generate(vararg songIds: Int) {
        responses += response(*songIds)
        viewModel.refreshSuggestedSongs(minDurationMs = 0L)
        advanceUntilIdle()
    }

    @Test
    fun `first generation sends no exclusion`() = runTest {
        generate(1, 2, 3, 4)

        assertEquals(emptyMap<Int, Set<Int>>(), sentExcluded.single())
    }

    @Test
    fun `sends the songs already shown in each free position`() = runTest {
        generate(1, 2, 3, 4)
        generate(5, 6, 7, 8)
        generate(9, 10, 11, 12)

        assertEquals(
            mapOf(1 to setOf(1, 5), 2 to setOf(2, 6), 3 to setOf(3, 7), 4 to setOf(4, 8)),
            sentExcluded.last(),
        )
    }

    @Test
    fun `does not send exclusion for a fixed position`() = runTest {
        generate(1, 2, 3, 4)
        viewModel.toggleFixed(position = 1)
        viewModel.toggleFixed(position = 2)
        generate(1, 2, 7, 8)
        generate(1, 2, 11, 12)

        assertEquals(mapOf(3 to setOf(3, 7), 4 to setOf(4, 8)), sentExcluded.last())
    }

    @Test
    fun `a generated fixed row goes in fixed with its Played id, not the Song id`() = runTest {
        generate(1, 2, 3, 4)
        viewModel.toggleFixed(position = 2)
        generate(5, 2, 7, 8)

        assertEquals(mapOf(2 to playedIdOf(2)), sentFixed.last())
    }

    @Test
    fun `a hand-picked fixed song is excluded from every free position`() = runTest {
        generate(1, 2, 3, 4)
        viewModel.selectSong(position = 2, song = songs[19])
        viewModel.toggleFixed(position = 2)
        generate(5, 20, 7, 8)

        assertEquals(emptyMap<Int, Int>(), sentFixed.last())
        assertEquals(
            mapOf(1 to setOf(1, 20), 3 to setOf(3, 20), 4 to setOf(4, 20)),
            sentExcluded.last(),
        )
    }

    @Test
    fun `a generated row keeps its Played id until a song is picked by hand`() = runTest {
        generate(1, 2, 3, 4)

        assertEquals(playedIdOf(3), viewModel.repertoireRows.value.first { it.position == 3 }.playedId)
        viewModel.selectSong(position = 3, song = songs[19])
        assertEquals(null, viewModel.repertoireRows.value.first { it.position == 3 }.playedId)
    }

    @Test
    fun `repeated song resets only its position`() = runTest {
        generate(1, 2, 3, 4)
        generate(5, 6, 7, 8)
        generate(9, 10, 3, 12) // position 3 ran out: 3 comes back
        generate(13, 14, 15, 16)

        assertEquals(
            mapOf(1 to setOf(1, 5, 9), 2 to setOf(2, 6, 10), 3 to setOf(3), 4 to setOf(4, 8, 12)),
            sentExcluded.last(),
        )
    }

    @Test
    fun `unfixing starts the position over with the song it holds`() = runTest {
        generate(1, 2, 3, 4)
        generate(5, 6, 7, 8)
        viewModel.toggleFixed(position = 3)
        generate(9, 10, 7, 12)
        viewModel.toggleFixed(position = 3)
        generate(13, 14, 15, 16)

        assertEquals(setOf(7), sentExcluded[3][3])
    }

    @Test
    fun `clearing the repertoire starts a new session`() = runTest {
        generate(1, 2, 3, 4)
        viewModel.clearRepertoire()
        generate(5, 6, 7, 8)

        assertEquals(emptyMap<Int, Set<Int>>(), sentExcluded.last())
    }
}
