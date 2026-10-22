package com.ipb.castelobranco.features.worshiphub.songs.presentation.viewmodel

import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.model.Song
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongsListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var songsRepository: SongsRepository
    private lateinit var viewModel: SongsListViewModel
    private val accessRepository = FakeAccessRepository()

    private val fakeSongs = listOf(
        Song(id = 1, title = "Oceans", artist = "Hillsong", categoryName = "Louvor"),
        Song(id = 2, title = "Way Maker", artist = "Sinach", categoryName = "Adoração"),
        Song(id = 3, title = "Oceanos da Graça", artist = "Hillsong", categoryName = "Louvor"),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        songsRepository = mockk()
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Loading)
        coEvery { songsRepository.refreshAllSongs() } returns RefreshResult.Updated
        viewModel = SongsListViewModel(songsRepository, ObserveAccessUseCase(accessRepository))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.subscribeAndAdvance() {
        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        job.cancel()
    }

    @Test
    fun `uiState initial value has isLoading true`() = runTest {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `uiState populates songs from repository`() = runTest {
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = SongsListViewModel(songsRepository, ObserveAccessUseCase(accessRepository))

        subscribeAndAdvance()

        val state = viewModel.uiState.value
        assertEquals(3, state.songs.size)
        assertEquals(3, state.filteredSongs.size)
    }

    @Test
    fun `onQueryChange filters by title`() = runTest {
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = SongsListViewModel(songsRepository, ObserveAccessUseCase(accessRepository))

        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onQueryChange("Way")
        advanceUntilIdle()
        job.cancel()

        val filtered = viewModel.uiState.value.filteredSongs
        assertEquals(1, filtered.size)
        assertEquals("Way Maker", filtered.first().title)
    }

    @Test
    fun `onQueryChange filters by artist`() = runTest {
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = SongsListViewModel(songsRepository, ObserveAccessUseCase(accessRepository))

        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onQueryChange("Sinach")
        advanceUntilIdle()
        job.cancel()

        val filtered = viewModel.uiState.value.filteredSongs
        assertEquals(1, filtered.size)
        assertEquals("Way Maker", filtered.first().title)
    }

    @Test
    fun `onQueryChange is accent-insensitive`() = runTest {
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = SongsListViewModel(songsRepository, ObserveAccessUseCase(accessRepository))

        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onQueryChange("graca")
        advanceUntilIdle()
        job.cancel()

        val filtered = viewModel.uiState.value.filteredSongs
        assertEquals(1, filtered.size)
        assertEquals("Oceanos da Graça", filtered.first().title)
    }

    @Test
    fun `blank query returns all songs`() = runTest {
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = SongsListViewModel(songsRepository, ObserveAccessUseCase(accessRepository))

        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onQueryChange("")
        advanceUntilIdle()
        job.cancel()

        assertEquals(3, viewModel.uiState.value.filteredSongs.size)
    }

    @Test
    fun `refresh calls repository refreshAllSongs`() = runTest {
        viewModel.refresh(minDurationMs = 0L)
        advanceUntilIdle()
        coVerify { songsRepository.refreshAllSongs() }
    }

    @Test
    fun `isRefreshing is false after refresh completes`() = runTest {
        viewModel.refresh(minDurationMs = 0L)
        advanceUntilIdle()
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `uiState sets error when observe emits Error`() = runTest {
        every { songsRepository.observeAllSongs() } returns
            flowOf(SnapshotState.Error(AppError.Network(message = "network error")))
        viewModel = SongsListViewModel(songsRepository, ObserveAccessUseCase(accessRepository))

        subscribeAndAdvance()

        assertEquals(
            "Sem conexão com a internet. Verifique sua rede e tente novamente.",
            viewModel.uiState.value.error,
        )
    }

    @Test
    fun `canEdit only with manage on songs`() = runTest {
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
        viewModel = SongsListViewModel(songsRepository, ObserveAccessUseCase(accessRepository))
        val job = launch { viewModel.uiState.collect { } }

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.canEdit)

        accessRepository.state.value = accessOf(Role.LEADER, Scope.SONGS to AccessLevel.MANAGE)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canEdit)

        accessRepository.state.value = accessOf(Role.MEDIA, Scope.SONGS to AccessLevel.VIEW)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.canEdit)
        job.cancel()
    }
}
