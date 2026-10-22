package com.ipb.castelobranco.features.worshiphub.songs.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.features.worshiphub.register.domain.FakeWorshipRegisterRepository
import com.ipb.castelobranco.features.worshiphub.register.domain.usecase.RegisterSongUseCase
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class SongCreateViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val registerRepository = FakeWorshipRegisterRepository()
    private val songsRepository = mockk<SongsRepository>()
    private lateinit var viewModel: SongCreateViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { songsRepository.refreshAllSongs() } returns RefreshResult.Updated
        viewModel = SongCreateViewModel(RegisterSongUseCase(registerRepository), songsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fill(title: String = "Oceans", artist: String = "Hillsong") {
        viewModel.onTitleChange(title)
        viewModel.onArtistChange(artist)
    }

    @Test
    fun `saving registers the trimmed song, refreshes the catalogue and closes`() = runTest {
        fill(title = " Oceans ", artist = "Hillsong ")

        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(listOf("Oceans" to "Hillsong"), registerRepository.registeredSongs)
        coVerify(exactly = 1) { songsRepository.refreshAllSongs() }
        assertTrue(viewModel.uiState.value.savedSuccessfully)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `409 shows the duplicate message and keeps the form`() = runTest {
        registerRepository.registerSongResult = { _, _ -> Result.failure(AppError.Server(code = 409)) }
        fill()

        viewModel.onSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Essa música já está cadastrada.", state.saveError)
        assertEquals("Oceans", state.title)
        assertFalse(state.savedSuccessfully)
        coVerify(exactly = 0) { songsRepository.refreshAllSongs() }
    }

    @Test
    fun `400 shows the invalid fields message`() = runTest {
        registerRepository.registerSongResult = { _, _ -> Result.failure(AppError.Server(code = 400)) }
        fill()

        viewModel.onSave()
        advanceUntilIdle()

        assertEquals("Título ou artista inválido. Use até 100 caracteres.", viewModel.uiState.value.saveError)
    }

    @Test
    fun `a blank field never reaches the repository`() = runTest {
        fill(artist = "  ")

        viewModel.onSave()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canSave)
        assertTrue(registerRepository.registeredSongs.isEmpty())
    }

    @Test
    fun `editing a field clears the error`() = runTest {
        registerRepository.registerSongResult = { _, _ -> Result.failure(AppError.Server(code = 409)) }
        fill()
        viewModel.onSave()
        advanceUntilIdle()

        viewModel.onTitleChange("Oceans (ao vivo)")

        assertNull(viewModel.uiState.value.saveError)
    }
}
