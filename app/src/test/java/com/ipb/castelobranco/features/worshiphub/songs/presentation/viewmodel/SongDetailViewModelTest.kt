package com.ipb.castelobranco.features.worshiphub.songs.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.model.ChordChart
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.model.Lyrics
import com.ipb.castelobranco.features.worshiphub.songs.domain.FakeSongEditRepository
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongEditFields
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongWriteResult
import com.ipb.castelobranco.features.worshiphub.songs.domain.usecase.DeleteSongUseCase
import com.ipb.castelobranco.features.worshiphub.songs.domain.usecase.GetSongDetailUseCase
import com.ipb.castelobranco.features.worshiphub.songs.domain.validation.SongEditValidator
import com.ipb.castelobranco.features.worshiphub.songs.domain.usecase.UpdateSongUseCase
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.SongDetailEvent
import com.ipb.castelobranco.features.worshiphub.songs.domain.usecase.SongDetail
import com.ipb.castelobranco.core.domain.model.Song
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getSongDetailUseCase: GetSongDetailUseCase
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: SongDetailViewModel
    private val accessRepository = FakeAccessRepository()
    private val editRepository = FakeSongEditRepository()

    private val fakeSong = Song(id = 1, title = "Oceans", artist = "Hillsong", categoryName = "Louvor", youtubeLink = "https://youtube.com/oceans")

    private val fakeDetail = SongDetail(
        song        = fakeSong,
        playCount   = 5,
        tones       = listOf("D", "C"),
        lastSundays = listOf("25/05/2025", "18/05/2025"),
        chordCharts = listOf(
            ChordChart(id = 10, songId = 1, content = "...", tone = "D", instrument = "violão"),
        ),
        lyrics = Lyrics(id = 20, songId = 1, content = "..."),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getSongDetailUseCase = mockk()
        savedStateHandle = SavedStateHandle(mapOf("songId" to 1))

        every { getSongDetailUseCase.observe(1) } returns flowOf(SnapshotState.Loading)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SongDetailViewModel {
        return SongDetailViewModel(
            savedStateHandle,
            getSongDetailUseCase,
            ObserveAccessUseCase(accessRepository),
            UpdateSongUseCase(editRepository),
            DeleteSongUseCase(editRepository),
        )
    }

    private fun TestScope.subscribeAndAdvance(vm: SongDetailViewModel) {
        val job = launch { vm.uiState.collect { } }
        advanceUntilIdle()
        job.cancel()
    }

    @Test
    fun `uiState initial value has isLoading true`() = runTest {
        viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `uiState maps SongDetail to UiState correctly`() = runTest {
        every { getSongDetailUseCase.observe(1) } returns flowOf(SnapshotState.Data(fakeDetail))
        viewModel = createViewModel()

        subscribeAndAdvance(viewModel)

        val state = viewModel.uiState.value
        assertEquals("Oceans", state.songName)
        assertEquals("Hillsong", state.artist)
        assertEquals(5, state.playCount)
        assertEquals(listOf("D", "C"), state.tones)
        assertEquals(listOf("25/05/2025", "18/05/2025"), state.lastSundays)
        assertEquals(1, state.chordCharts.size)
        assertTrue(state.hasLyrics)
        assertEquals(20, state.lyricsId)
        assertEquals("https://youtube.com/oceans", state.youtubeLink)
    }

    @Test
    fun `uiState sets error when use case emits Error`() = runTest {
        every { getSongDetailUseCase.observe(1) } returns
            flowOf(SnapshotState.Error(AppError.Server(code = 500, message = "fail")))
        viewModel = createViewModel()

        subscribeAndAdvance(viewModel)

        assertEquals(
            "Não foi possível completar a operação. Tente novamente mais tarde.",
            viewModel.uiState.value.error,
        )
    }

    @Test
    fun `uiState has no lyrics when detail lyrics is null`() = runTest {
        val detailNoLyrics = fakeDetail.copy(lyrics = null)
        every { getSongDetailUseCase.observe(1) } returns flowOf(SnapshotState.Data(detailNoLyrics))
        viewModel = createViewModel()

        subscribeAndAdvance(viewModel)

        val state = viewModel.uiState.value
        assertEquals(false, state.hasLyrics)
        assertNull(state.lyricsId)
    }

    private fun TestScope.loadedViewModel(vararg levels: Pair<Scope, AccessLevel>): SongDetailViewModel {
        accessRepository.state.value = accessOf(null, *levels)
        every { getSongDetailUseCase.observe(1) } returns flowOf(SnapshotState.Data(fakeDetail))
        val vm = createViewModel()
        backgroundScope.launch { vm.uiState.collect { } }
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `manage can edit but only owner can delete`() = runTest {
        val manager = loadedViewModel(Scope.SONGS to AccessLevel.MANAGE)
        assertTrue(manager.uiState.value.canEdit)
        assertFalse(manager.uiState.value.canDelete)

        val owner = loadedViewModel(Scope.SONGS to AccessLevel.OWNER)
        assertTrue(owner.uiState.value.canEdit)
        assertTrue(owner.uiState.value.canDelete)

        val viewer = loadedViewModel(Scope.SONGS to AccessLevel.VIEW)
        assertFalse(viewer.uiState.value.canEdit)
    }

    @Test
    fun `entering edit mode fills the form with the current values`() = runTest {
        val vm = loadedViewModel(Scope.SONGS to AccessLevel.MANAGE)

        vm.enterEditMode()
        advanceUntilIdle()

        val form = vm.uiState.value.edit!!
        assertEquals("Oceans", form.title)
        assertEquals("Hillsong", form.artist)
        assertEquals("https://youtube.com/oceans", form.youtubeLink)
    }

    @Test
    fun `saving invalid fields shows the error on the field and sends nothing`() = runTest {
        val vm = loadedViewModel(Scope.SONGS to AccessLevel.MANAGE)
        vm.enterEditMode()
        vm.onTitleChange(" ")

        vm.saveEdit()
        advanceUntilIdle()

        assertEquals(SongEditValidator.TITLE_REQUIRED, vm.uiState.value.edit!!.fieldErrors.title)
        assertTrue(editRepository.updates.isEmpty())

        vm.onTitleChange("Oceans (Where Feet May Fail)")
        advanceUntilIdle()
        assertEquals(null, vm.uiState.value.edit!!.fieldErrors.title)
    }

    @Test
    fun `successful save leaves edit mode`() = runTest {
        val vm = loadedViewModel(Scope.SONGS to AccessLevel.MANAGE)
        vm.enterEditMode()
        vm.onYoutubeLinkChange("")

        vm.saveEdit()
        advanceUntilIdle()

        assertEquals(1 to SongEditFields("Oceans", "Hillsong", ""), editRepository.updates.single())
        assertFalse(vm.uiState.value.isEditing)
    }

    @Test
    fun `duplicate keeps edit mode with the message`() = runTest {
        editRepository.updateResult = SongWriteResult.Duplicate
        val vm = loadedViewModel(Scope.SONGS to AccessLevel.MANAGE)
        vm.enterEditMode()
        vm.onTitleChange("Outra")

        vm.saveEdit()
        advanceUntilIdle()

        val form = vm.uiState.value.edit!!
        assertFalse(form.isSaving)
        assertEquals("Já existe uma música com esse nome e artista.", form.saveError)
    }

    @Test
    fun `successful delete emits Deleted`() = runTest {
        val vm = loadedViewModel(Scope.SONGS to AccessLevel.OWNER)
        val events = mutableListOf<SongDetailEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }

        vm.deleteSong()
        advanceUntilIdle()

        assertEquals(listOf(1), editRepository.deletes)
        assertEquals(listOf<SongDetailEvent>(SongDetailEvent.Deleted), events)
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun `song in use shows the API's explanation`() = runTest {
        editRepository.deleteResult = SongWriteResult.InUse("Foi tocada em 7 domingos.")
        val vm = loadedViewModel(Scope.SONGS to AccessLevel.OWNER)

        vm.deleteSong()
        advanceUntilIdle()

        assertEquals("Foi tocada em 7 domingos.", vm.uiState.value.deleteError)
        assertFalse(vm.uiState.value.isDeleting)

        vm.dismissDeleteError()
        advanceUntilIdle()
        assertEquals(null, vm.uiState.value.deleteError)
    }
}
