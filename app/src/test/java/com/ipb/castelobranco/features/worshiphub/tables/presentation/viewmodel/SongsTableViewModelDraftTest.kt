package com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel

import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.testing.FakeWallClock
import com.ipb.castelobranco.features.worshiphub.tables.domain.FakeRepertoireDraftRepository
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.DraftRow
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.RepertoireDraft
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongsTableViewModelDraftTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SongsRepository
    private val catalog = MutableStateFlow<SnapshotState<List<Song>>>(SnapshotState.Loading)

    private val oceans = Song(id = 1, title = "Oceans", artist = "Hillsong", categoryName = "Louvor")
    private val wayMaker = Song(id = 2, title = "Way Maker", artist = "Sinach", categoryName = "Adoração")

    private val now = 50_000_000L
    private val draftRows = listOf(
        DraftRow(1, 1, "D", true),
        DraftRow(2, 99, "E", false),
        DraftRow(3, null, "", false),
        DraftRow(4, 2, "A", false),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        every { repository.observeAllSongs() } returns catalog
        every { repository.observeSongsBySunday() } returns flowOf(SnapshotState.Loading)
        every { repository.observeTopSongs() } returns flowOf(SnapshotState.Loading)
        every { repository.observeTopTones() } returns flowOf(SnapshotState.Loading)
        every { repository.observeSuggestedSongs() } returns flowOf(SnapshotState.Loading)
        coEvery { repository.refreshAllSongs() } returns RefreshResult.Updated
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the draft waits for the catalog, then comes back with missing songs emptied`() = runTest {
        val drafts = FakeRepertoireDraftRepository(RepertoireDraft(draftRows, now - 1_000))
        val viewModel = songsTableViewModel(repository, drafts, FakeWallClock(now))
        advanceUntilIdle()
        assertTrue(viewModel.repertoireRows.value.all { it.selectedSong == null })

        catalog.value = SnapshotState.Data(listOf(oceans, wayMaker))
        advanceUntilIdle()

        val rows = viewModel.repertoireRows.value
        assertEquals(oceans, rows[0].selectedSong)
        assertEquals("D", rows[0].tone)
        assertTrue(rows[0].isFixed)
        assertNull(rows[1].selectedSong)
        assertEquals("", rows[1].tone)
        assertEquals(wayMaker, rows[3].selectedSong)
        assertTrue("restoring must not renew the hour", drafts.saves.isEmpty())
    }

    @Test
    fun `an expired draft leaves the rows empty`() = runTest {
        val drafts = FakeRepertoireDraftRepository(RepertoireDraft(draftRows, now - 2 * 3_600_000))
        val viewModel = songsTableViewModel(repository, drafts, FakeWallClock(now))
        catalog.value = SnapshotState.Data(listOf(oceans, wayMaker))
        advanceUntilIdle()

        assertTrue(viewModel.repertoireRows.value.all { it.selectedSong == null })
        assertNull(drafts.stored)
    }

    @Test
    fun `every edit saves the draft with the current time`() = runTest {
        val drafts = FakeRepertoireDraftRepository()
        val clock = FakeWallClock(now)
        val viewModel = songsTableViewModel(repository, drafts, clock)
        catalog.value = SnapshotState.Data(listOf(oceans, wayMaker))
        advanceUntilIdle()

        viewModel.selectSong(1, oceans)
        advanceUntilIdle()
        clock.now = now + 1_000
        viewModel.onToneChange(1, "G")
        advanceUntilIdle()
        viewModel.toggleFixed(1)
        advanceUntilIdle()

        assertEquals(3, drafts.saves.size)
        val last = drafts.saves.last()
        assertEquals(now + 1_000, last.updatedAtMillis)
        assertEquals(DraftRow(1, 1, "G", true), last.rows[0])
    }

    @Test
    fun `clear empties the rows and the stored draft`() = runTest {
        val drafts = FakeRepertoireDraftRepository(RepertoireDraft(draftRows, now))
        val viewModel = songsTableViewModel(repository, drafts, FakeWallClock(now))
        catalog.value = SnapshotState.Data(listOf(oceans, wayMaker))
        advanceUntilIdle()

        viewModel.clearRepertoire()
        advanceUntilIdle()

        assertTrue(viewModel.repertoireRows.value.all { it.selectedSong == null && it.tone.isEmpty() && !it.isFixed })
        assertNull(drafts.stored)
    }
}
