package com.ipb.castelobranco.features.worshiphub.register.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import androidx.lifecycle.SavedStateHandle
import com.ipb.castelobranco.core.domain.setlist.FakeSetlistConfirmationRepository
import com.ipb.castelobranco.core.domain.setlist.GetSetlistForDateUseCase
import com.ipb.castelobranco.features.worshiphub.register.domain.usecase.ObserveSongsUseCase
import com.ipb.castelobranco.features.worshiphub.register.domain.usecase.SubmitSundayPlaysUseCase
import com.ipb.castelobranco.features.worshiphub.register.presentation.state.SundayRegistrationEvent
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.features.worshiphub.tables.domain.usecase.RefreshPlayedTablesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class SundayRegistrationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var observeSongsUseCase: ObserveSongsUseCase
    private lateinit var submitSundayPlaysUseCase: SubmitSundayPlaysUseCase
    private lateinit var viewModel: SundayRegistrationViewModel
    private val refreshPlayedTables = mockk<RefreshPlayedTablesUseCase>(relaxed = true)

    private val songsFlow = MutableStateFlow<SnapshotState<List<Song>>>(SnapshotState.Loading)

    private val fakeSongs = listOf(
        Song(id = 1, title = "Oceans", artist = "Hillsong", categoryName = "Louvor"),
        Song(id = 2, title = "Way Maker", artist = "Sinach", categoryName = "Adoração"),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        observeSongsUseCase = mockk()
        submitSundayPlaysUseCase = mockk()

        every { observeSongsUseCase.observe() } returns songsFlow
        coEvery { observeSongsUseCase.refresh() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SundayRegistrationViewModel {
        return SundayRegistrationViewModel(
            observeSongsUseCase,
            submitSundayPlaysUseCase,
            GetSetlistForDateUseCase(FakeSetlistConfirmationRepository()),
            refreshPlayedTables,
            SavedStateHandle(),
        ).also {
            viewModel = it
        }
    }

    // region Init

    @Test
    fun `Init starts loading songs`() = runTest {
        createViewModel()
        viewModel.onEvent(SundayRegistrationEvent.Init)
        advanceUntilIdle()

        // Songs flow emits Loading → isLoadingSongs eventually set
        // After refresh completes, isLoadingSongs becomes false
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `Init with Data snapshot populates availableSongs`() = runTest {
        createViewModel()
        viewModel.onEvent(SundayRegistrationEvent.Init)

        songsFlow.value = SnapshotState.Data(fakeSongs)
        advanceUntilIdle()

        assertEquals(fakeSongs, viewModel.uiState.value.availableSongs)
        assertFalse(viewModel.uiState.value.isLoadingSongs)
    }

    @Test
    fun `Init with Error snapshot shows snackbar`() = runTest {
        createViewModel()
        viewModel.onEvent(SundayRegistrationEvent.Init)

        songsFlow.value = SnapshotState.Error(AppError.Server(code = 500, message = "fail"))
        advanceUntilIdle()

        assertEquals("Falha ao carregar músicas.", viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `Init called twice observes once but refreshes every time`() = runTest {
        createViewModel()
        viewModel.onEvent(SundayRegistrationEvent.Init)
        songsFlow.value = SnapshotState.Data(fakeSongs)
        advanceUntilIdle()

        // Reopening the screen must pick up songs registered meanwhile
        viewModel.onEvent(SundayRegistrationEvent.Init)
        advanceUntilIdle()

        assertEquals(fakeSongs, viewModel.uiState.value.availableSongs)
        verify(exactly = 1) { observeSongsUseCase.observe() }
        coVerify(exactly = 2) { observeSongsUseCase.refresh() }
    }

    // endregion

    // region DatePicker

    @Test
    fun `OpenDatePicker sets showDatePicker true`() = runTest {
        createViewModel()

        viewModel.onEvent(SundayRegistrationEvent.OpenDatePicker)

        assertTrue(viewModel.uiState.value.showDatePicker)
    }

    @Test
    fun `DismissDatePicker sets showDatePicker false`() = runTest {
        createViewModel()
        viewModel.onEvent(SundayRegistrationEvent.OpenDatePicker)

        viewModel.onEvent(SundayRegistrationEvent.DismissDatePicker)

        assertFalse(viewModel.uiState.value.showDatePicker)
    }

    @Test
    fun `DatePicked sets selectedDate and closes picker`() = runTest {
        createViewModel()
        viewModel.onEvent(SundayRegistrationEvent.OpenDatePicker)

        val date = LocalDate.of(2025, 5, 4)
        viewModel.onEvent(SundayRegistrationEvent.DatePicked(date))

        assertEquals(date, viewModel.uiState.value.selectedDate)
        assertFalse(viewModel.uiState.value.showDatePicker)
    }

    // endregion

    // region SundayRows

    @Test
    fun `SundaySongSelected updates song in row`() = runTest {
        createViewModel()
        viewModel.onEvent(SundayRegistrationEvent.Init)
        songsFlow.value = SnapshotState.Data(fakeSongs)
        advanceUntilIdle()

        viewModel.onEvent(SundayRegistrationEvent.SundaySongSelected(1, fakeSongs[0]))

        val row = viewModel.uiState.value.sundayRows.find { it.position == 1 }!!
        assertEquals(fakeSongs[0].id, row.selectedSongId)
    }

    @Test
    fun `SundayToneChanged updates tone in row`() = runTest {
        createViewModel()

        viewModel.onEvent(SundayRegistrationEvent.SundayToneChanged(1, "G"))

        val row = viewModel.uiState.value.sundayRows.find { it.position == 1 }!!
        assertEquals("G", row.tone)
    }

    @Test
    fun `AddSundayRow adds a new row`() = runTest {
        createViewModel()
        val initialCount = viewModel.uiState.value.sundayRows.size

        viewModel.onEvent(SundayRegistrationEvent.AddSundayRow)

        assertEquals(initialCount + 1, viewModel.uiState.value.sundayRows.size)
    }

    @Test
    fun `RemoveSundayRow with position lte 4 does not remove`() = runTest {
        createViewModel()
        val initialCount = viewModel.uiState.value.sundayRows.size

        viewModel.onEvent(SundayRegistrationEvent.RemoveSundayRow(1))

        assertEquals(initialCount, viewModel.uiState.value.sundayRows.size)
    }

    @Test
    fun `RemoveSundayRow with position gt 4 removes the row`() = runTest {
        createViewModel()
        viewModel.onEvent(SundayRegistrationEvent.AddSundayRow) // position 5
        val countAfterAdd = viewModel.uiState.value.sundayRows.size

        viewModel.onEvent(SundayRegistrationEvent.RemoveSundayRow(5))

        assertEquals(countAfterAdd - 1, viewModel.uiState.value.sundayRows.size)
    }

    // endregion

    // region Submit

    @Test
    fun `Submit success shows success snackbar`() = runTest {
        createViewModel()
        viewModel.onEvent(SundayRegistrationEvent.Init)
        songsFlow.value = SnapshotState.Data(fakeSongs)
        advanceUntilIdle()

        // Fill rows so canSubmit = true
        viewModel.onEvent(SundayRegistrationEvent.SundaySongSelected(1, fakeSongs[0]))
        viewModel.onEvent(SundayRegistrationEvent.SundayToneChanged(1, "G"))
        viewModel.onEvent(SundayRegistrationEvent.SundaySongSelected(2, fakeSongs[1]))
        viewModel.onEvent(SundayRegistrationEvent.SundayToneChanged(2, "D"))
        // Remove empty rows
        viewModel.onEvent(SundayRegistrationEvent.RemoveSundayRow(3))
        viewModel.onEvent(SundayRegistrationEvent.RemoveSundayRow(4))

        coEvery { submitSundayPlaysUseCase(any(), any(), any()) } returns SubmitSundayPlaysUseCase.Result.Success

        viewModel.onEvent(SundayRegistrationEvent.Submit)
        advanceUntilIdle()

        assertEquals("Enviado com sucesso.", viewModel.uiState.value.snackbarMessage)
        assertFalse(viewModel.uiState.value.isSubmitting)
        // The Tables read the played songs: they pick up the new Sunday on return.
        coVerify(exactly = 1) { refreshPlayedTables() }
    }

    @Test
    fun `Submit failure shows error snackbar`() = runTest {
        createViewModel()
        viewModel.onEvent(SundayRegistrationEvent.Init)
        songsFlow.value = SnapshotState.Data(fakeSongs)
        advanceUntilIdle()

        viewModel.onEvent(SundayRegistrationEvent.SundaySongSelected(1, fakeSongs[0]))
        viewModel.onEvent(SundayRegistrationEvent.SundayToneChanged(1, "G"))
        viewModel.onEvent(SundayRegistrationEvent.RemoveSundayRow(2))
        viewModel.onEvent(SundayRegistrationEvent.RemoveSundayRow(3))
        viewModel.onEvent(SundayRegistrationEvent.RemoveSundayRow(4))

        coEvery { submitSundayPlaysUseCase(any(), any(), any()) } returns
            SubmitSundayPlaysUseCase.Result.Failure("Erro no servidor")

        viewModel.onEvent(SundayRegistrationEvent.Submit)
        advanceUntilIdle()

        assertEquals("Erro no servidor", viewModel.uiState.value.snackbarMessage)
        coVerify(exactly = 0) { refreshPlayedTables() }
    }

    // endregion

    // region Snackbar

    @Test
    fun `SnackbarShown clears snackbar message`() = runTest {
        createViewModel()
        viewModel.onEvent(SundayRegistrationEvent.Init)
        songsFlow.value = SnapshotState.Error(AppError.Server(code = 500, message = "fail"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.snackbarMessage != null)

        viewModel.onEvent(SundayRegistrationEvent.SnackbarShown)

        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    // endregion

}
