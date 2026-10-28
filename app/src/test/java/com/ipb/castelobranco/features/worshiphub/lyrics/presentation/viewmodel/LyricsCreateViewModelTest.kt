package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.repository.LyricsRepository
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsCreateViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var lyricsRepository: LyricsRepository
    private lateinit var songsRepository: SongsRepository

    private val fakeSongs = listOf(
        Song(id = 1, title = "Oceans", artist = "Hillsong", categoryName = "Louvor"),
        Song(id = 2, title = "Way Maker", artist = "Sinach", categoryName = "Adoração"),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        lyricsRepository = mockk()
        songsRepository = mockk()
        every { songsRepository.observeAllSongs() } returns flowOf(SnapshotState.Data(fakeSongs))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = LyricsCreateViewModel(lyricsRepository, songsRepository)

    @Test
    fun `initial state has empty fields`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("", state.songQuery)
        assertEquals("", state.content)
        assertNull(state.selectedSong)
        assertFalse(state.savedSuccessfully)
    }

    @Test
    fun `songs loaded from repository`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.allSongs.size)
    }

    @Test
    fun `onSongQueryChange filters songs by title`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSongQueryChange("oceans")
        advanceUntilIdle()

        val filtered = vm.uiState.value.filteredSongs
        assertEquals(1, filtered.size)
        assertEquals("Oceans", filtered.first().title)
    }

    @Test
    fun `onSongSelected sets selectedSong and clears list`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSongQueryChange("oceans")
        advanceUntilIdle()
        vm.onSongSelected(fakeSongs.first())
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(fakeSongs.first(), state.selectedSong)
        assertEquals("Oceans", state.songQuery)
        assertTrue(state.filteredSongs.isEmpty())
    }

    @Test
    fun `onSongCleared goes back to an empty search`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onSongSelected(fakeSongs.first())
        advanceUntilIdle()

        vm.onSongCleared()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(null, state.selectedSong)
        assertEquals("", state.songQuery)
    }

    @Test
    fun `missing lists what blocks saving, in the form's order`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        assertEquals(listOf("música", "letra"), vm.uiState.value.missing)

        vm.onSongSelected(fakeSongs.first())
        advanceUntilIdle()

        assertEquals(listOf("letra"), vm.uiState.value.missing)
    }

    @Test
    fun `onContentChange updates content`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onContentChange("Nova letra aqui")
        advanceUntilIdle()

        assertEquals("Nova letra aqui", vm.uiState.value.content)
    }

    @Test
    fun `onSave success sets savedSuccessfully true`() = runTest {
        coEvery { lyricsRepository.createLyrics(any(), any()) } returns Result.success(Unit)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSongSelected(fakeSongs.first())
        vm.onContentChange("Letra de teste")
        vm.onSave()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.savedSuccessfully)
        assertNull(vm.uiState.value.saveError)
    }

    @Test
    fun `onSave failure sets saveError`() = runTest {
        coEvery { lyricsRepository.createLyrics(any(), any()) } returns Result.failure(
            AppError.Server(
                code = 400,
                message = """{"error_code":"lyrics_exists","detail":"Já existe letra para esta música"}""",
                errorCode = "lyrics_exists",
                userMessage = "Já existe letra para esta música",
            )
        )

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSongSelected(fakeSongs.first())
        vm.onContentChange("Letra de teste")
        vm.onSave()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.savedSuccessfully)
        assertNotNull(vm.uiState.value.saveError)
        assertEquals("Já existe letra para esta música", vm.uiState.value.saveError)
    }

    @Test
    fun `onSave does nothing when no song selected`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onContentChange("Letra de teste")
        vm.onSave()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.savedSuccessfully)
        assertNull(vm.uiState.value.saveError)
    }
}
