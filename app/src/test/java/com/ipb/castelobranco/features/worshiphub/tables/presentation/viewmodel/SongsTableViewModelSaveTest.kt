package com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel

import app.cash.turbine.test
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.testing.FakeSundaySetlistRepository
import com.ipb.castelobranco.core.testing.FakeWorshipAccessRepository
import com.ipb.castelobranco.core.testing.WORSHIP_LEADER
import com.ipb.castelobranco.core.testing.WORSHIP_MEMBER
import com.ipb.castelobranco.features.worshiphub.tables.domain.FakeRepertoireDraftRepository
import com.ipb.castelobranco.features.worshiphub.tables.domain.FakeSetlistSaveRepository
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SaveSetlistFailure
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SaveSetlistResult
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SongsTableViewModelSaveTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SongsRepository

    private val oceans = Song(id = 1, title = "Oceans", artist = "Hillsong", categoryName = "Louvor")
    private val wednesday = LocalDate.of(2026, 9, 30)
    private val nextSunday = LocalDate.of(2026, 10, 4)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        every { repository.observeAllSongs() } returns MutableStateFlow(SnapshotState.Data(listOf(oceans)))
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
    fun `Salvar is hidden without permission to save`() = runTest {
        val viewModel = songsTableViewModel(repository, worshipAccess = FakeWorshipAccessRepository(WORSHIP_MEMBER))
        advanceUntilIdle()

        assertFalse(viewModel.saveState.value.canSave)
    }

    @Test
    fun `Salvar is enabled only with a filled row that has a key`() = runTest {
        val viewModel = songsTableViewModel(repository, worshipAccess = FakeWorshipAccessRepository(WORSHIP_LEADER))
        advanceUntilIdle()
        assertTrue(viewModel.saveState.value.canSave)
        assertFalse(viewModel.saveState.value.isSaveEnabled)

        viewModel.selectSong(1, oceans)
        viewModel.onToneChange(1, "")
        advanceUntilIdle()
        assertFalse(viewModel.saveState.value.isSaveEnabled)

        viewModel.onToneChange(1, "G")
        advanceUntilIdle()
        assertTrue(viewModel.saveState.value.isSaveEnabled)
    }

    @Test
    fun `the confirmation names the next Sunday and a save stores the setlist`() = runTest {
        val saves = FakeSetlistSaveRepository()
        val stored = FakeSundaySetlistRepository()
        val viewModel = songsTableViewModel(
            repository,
            worshipAccess = FakeWorshipAccessRepository(WORSHIP_LEADER),
            today = wednesday,
            saveRepository = saves,
            sundaySetlist = stored,
        )
        advanceUntilIdle()
        viewModel.selectSong(1, oceans)
        viewModel.onToneChange(1, "G")
        advanceUntilIdle()

        viewModel.requestSave()
        advanceUntilIdle()
        assertEquals(nextSunday, viewModel.saveState.value.pendingSaveDate)

        viewModel.events.test {
            viewModel.confirmSave()
            advanceUntilIdle()
            assertEquals(RepertoireEvent.Saved("Repertório de domingo 04/10 salvo."), awaitItem())
        }
        assertNull(viewModel.saveState.value.pendingSaveDate)
        assertEquals(nextSunday, saves.calls.single().first)
        assertNotNull(stored.state.value)
    }

    @Test
    fun `a refused save reports it and keeps the draft`() = runTest {
        val drafts = FakeRepertoireDraftRepository()
        val viewModel = songsTableViewModel(
            repository,
            draftRepository = drafts,
            worshipAccess = FakeWorshipAccessRepository(WORSHIP_LEADER),
            saveRepository = FakeSetlistSaveRepository(SaveSetlistResult.Failed(SaveSetlistFailure.NoConnection)),
        )
        advanceUntilIdle()
        viewModel.selectSong(1, oceans)
        viewModel.onToneChange(1, "G")
        advanceUntilIdle()
        val draftBefore = drafts.stored

        viewModel.requestSave()
        viewModel.events.test {
            viewModel.confirmSave()
            advanceUntilIdle()
            assertEquals(RepertoireEvent.SaveFailed("Sem conexão. Tente novamente."), awaitItem())
        }
        assertEquals(draftBefore, drafts.stored)
        assertEquals(oceans, viewModel.repertoireRows.value[0].selectedSong)
        assertFalse(viewModel.saveState.value.isSaving)
    }

    @Test
    fun `dismissing the confirmation sends nothing`() = runTest {
        val saves = FakeSetlistSaveRepository()
        val viewModel = songsTableViewModel(
            repository,
            worshipAccess = FakeWorshipAccessRepository(WORSHIP_LEADER),
            saveRepository = saves,
        )
        advanceUntilIdle()
        viewModel.selectSong(1, oceans)
        viewModel.onToneChange(1, "G")
        advanceUntilIdle()

        viewModel.requestSave()
        viewModel.dismissSave()
        advanceUntilIdle()

        assertNull(viewModel.saveState.value.pendingSaveDate)
        assertTrue(saves.calls.isEmpty())
    }
}
