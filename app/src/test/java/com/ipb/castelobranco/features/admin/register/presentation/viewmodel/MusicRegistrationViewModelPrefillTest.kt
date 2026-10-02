package com.ipb.castelobranco.features.admin.register.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.domain.setlist.SetlistItem
import com.ipb.castelobranco.core.domain.setlist.SundaySetlist
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.admin.register.domain.FakeSetlistConfirmationRepository
import com.ipb.castelobranco.features.admin.register.domain.usecase.GetSetlistForDateUseCase
import com.ipb.castelobranco.features.admin.register.domain.usecase.ObserveSongsUseCase
import com.ipb.castelobranco.features.admin.register.domain.usecase.SubmitSundayPlaysUseCase
import com.ipb.castelobranco.features.admin.register.presentation.state.MusicRegistrationEvent
import com.ipb.castelobranco.features.admin.register.presentation.state.PrefillState
import com.ipb.castelobranco.features.admin.register.presentation.state.RegistrationType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MusicRegistrationViewModelPrefillTest {

    private val testDispatcher = StandardTestDispatcher()
    private val sunday = LocalDate.of(2026, 10, 4)
    private val songs = MutableStateFlow<SnapshotState<List<Song>>>(
        SnapshotState.Data(listOf(Song(12, "Grande é o Senhor", "Adhemar", "Louvor"))),
    )
    private lateinit var observeSongs: ObserveSongsUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        observeSongs = mockk()
        every { observeSongs.observe() } returns songs
        coEvery { observeSongs.refresh() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeSetlistConfirmationRepository, date: String? = "2026-10-04") =
        MusicRegistrationViewModel(
            observeSongs,
            mockk<SubmitSundayPlaysUseCase>(),
            GetSetlistForDateUseCase(repository),
            SavedStateHandle(if (date == null) emptyMap() else mapOf("date" to date)),
        )

    private fun setlist(itemCount: Int) = SundaySetlist(
        date = sunday,
        items = (1..itemCount).map { SetlistItem(it, 12, "Grande é o Senhor", "Adhemar", "G$it".take(2)) }.reversed(),
        savedByName = "Ana",
        savedAt = "",
    )

    @Test
    fun `the setlist fills the rows in order with their keys, date fixed`() = runTest {
        val vm = viewModel(FakeSetlistConfirmationRepository(byDateResult = { Result.success(setlist(2)) }))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(PrefillState.Loaded, state.prefill)
        assertEquals(RegistrationType.SUNDAY, state.registrationType)
        assertEquals(sunday, state.selectedDate)
        assertEquals(4, state.sundayRows.size)
        assertEquals(listOf(12, 12, null, null), state.sundayRows.map { it.selectedSongId })
        assertEquals(listOf("G1", "G2", "", ""), state.sundayRows.map { it.tone })
        assertEquals("Grande é o Senhor [Adhemar]", state.sundayRows.first().songQuery)
    }

    @Test
    fun `more items than default rows get one row each`() = runTest {
        val vm = viewModel(FakeSetlistConfirmationRepository(byDateResult = { Result.success(setlist(6)) }))
        advanceUntilIdle()

        assertEquals(6, vm.uiState.value.sundayRows.size)
    }

    @Test
    fun `the date picker stays closed while confirming a Sunday`() = runTest {
        val vm = viewModel(FakeSetlistConfirmationRepository(byDateResult = { Result.success(setlist(1)) }))
        advanceUntilIdle()

        vm.onEvent(MusicRegistrationEvent.OpenDatePicker)

        assertFalse(vm.uiState.value.showDatePicker)
    }

    @Test
    fun `a missing setlist leaves empty rows and says so`() = runTest {
        val vm = viewModel(FakeSetlistConfirmationRepository())
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(PrefillState.NotFound, state.prefill)
        assertTrue(state.sundayRows.all { it.selectedSongId == null })
        assertEquals("Repertório de 04/10 não encontrado. Preencha as músicas.", state.snackbarMessage)
    }

    @Test
    fun `a failed read can be retried`() = runTest {
        val repository = FakeSetlistConfirmationRepository(byDateResult = { Result.failure(AppError.Network()) })
        val vm = viewModel(repository)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.prefill is PrefillState.Failed)

        repository.byDateResult = { Result.success(setlist(1)) }
        vm.onEvent(MusicRegistrationEvent.RetryPrefill)
        advanceUntilIdle()

        assertEquals(PrefillState.Loaded, vm.uiState.value.prefill)
        assertEquals(2, repository.byDateCalls)
    }

    @Test
    fun `without a date the screen opens as before`() = runTest {
        val repository = FakeSetlistConfirmationRepository()
        val vm = viewModel(repository, date = null)
        advanceUntilIdle()

        assertEquals(PrefillState.None, vm.uiState.value.prefill)
        assertNull(vm.uiState.value.prefillDate)
        assertEquals(0, repository.byDateCalls)
    }
}
