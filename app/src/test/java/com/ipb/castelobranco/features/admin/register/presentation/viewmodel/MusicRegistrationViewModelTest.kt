package com.ipb.castelobranco.features.admin.register.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import androidx.lifecycle.SavedStateHandle
import com.ipb.castelobranco.features.admin.register.domain.FakeSetlistConfirmationRepository
import com.ipb.castelobranco.features.admin.register.domain.usecase.GetSetlistForDateUseCase
import com.ipb.castelobranco.features.admin.register.domain.usecase.ObserveSongsUseCase
import com.ipb.castelobranco.features.admin.register.domain.usecase.SubmitSundayPlaysUseCase
import com.ipb.castelobranco.features.admin.register.presentation.state.MusicRegistrationEvent
import com.ipb.castelobranco.features.admin.register.presentation.state.RegistrationType
import com.ipb.castelobranco.core.domain.model.Song
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
class MusicRegistrationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var observeSongsUseCase: ObserveSongsUseCase
    private lateinit var submitSundayPlaysUseCase: SubmitSundayPlaysUseCase
    private lateinit var viewModel: MusicRegistrationViewModel

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

    private fun createViewModel(): MusicRegistrationViewModel {
        return MusicRegistrationViewModel(
            observeSongsUseCase,
            submitSundayPlaysUseCase,
            GetSetlistForDateUseCase(FakeSetlistConfirmationRepository()),
            SavedStateHandle(),
        ).also {
            viewModel = it
        }
    }

    // region Init

    @Test
    fun `Init starts loading songs`() = runTest {
        createViewModel()
        viewModel.onEvent(MusicRegistrationEvent.Init)
        advanceUntilIdle()

        // Songs flow emits Loading → isLoadingSongs eventually set
        // After refresh completes, isLoadingSongs becomes false
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `Init with Data snapshot populates availableSongs`() = runTest {
        createViewModel()
        viewModel.onEvent(MusicRegistrationEvent.Init)

        songsFlow.value = SnapshotState.Data(fakeSongs)
        advanceUntilIdle()

        assertEquals(fakeSongs, viewModel.uiState.value.availableSongs)
        assertFalse(viewModel.uiState.value.isLoadingSongs)
    }

    @Test
    fun `Init with Error snapshot shows snackbar`() = runTest {
        createViewModel()
        viewModel.onEvent(MusicRegistrationEvent.Init)

        songsFlow.value = SnapshotState.Error(AppError.Server(code = 500, message = "fail"))
        advanceUntilIdle()

        assertEquals("Falha ao carregar músicas.", viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `Init called twice does not re-observe`() = runTest {
        createViewModel()
        viewModel.onEvent(MusicRegistrationEvent.Init)
        songsFlow.value = SnapshotState.Data(fakeSongs)
        advanceUntilIdle()

        // Second Init should be ignored (alreadyObserving = true)
        viewModel.onEvent(MusicRegistrationEvent.Init)
        advanceUntilIdle()

        assertEquals(fakeSongs, viewModel.uiState.value.availableSongs)
    }

    // endregion

    // region RegistrationType

    @Test
    fun `RegistrationTypeChanged to MUSIC clears date and errors`() = runTest {
        createViewModel()

        viewModel.onEvent(MusicRegistrationEvent.RegistrationTypeChanged(RegistrationType.MUSIC))

        val state = viewModel.uiState.value
        assertEquals(RegistrationType.MUSIC, state.registrationType)
        assertNull(state.selectedDate)
        assertFalse(state.showDatePicker)
        assertTrue(state.sundayRowErrors.isEmpty())
    }

    @Test
    fun `RegistrationTypeChanged to SUNDAY keeps date`() = runTest {
        createViewModel()

        viewModel.onEvent(MusicRegistrationEvent.RegistrationTypeChanged(RegistrationType.SUNDAY))

        assertEquals(RegistrationType.SUNDAY, viewModel.uiState.value.registrationType)
    }

    // endregion

    // region DatePicker

    @Test
    fun `OpenDatePicker sets showDatePicker true when SUNDAY`() = runTest {
        createViewModel()

        viewModel.onEvent(MusicRegistrationEvent.OpenDatePicker)

        assertTrue(viewModel.uiState.value.showDatePicker)
    }

    @Test
    fun `OpenDatePicker does nothing when MUSIC`() = runTest {
        createViewModel()
        viewModel.onEvent(MusicRegistrationEvent.RegistrationTypeChanged(RegistrationType.MUSIC))

        viewModel.onEvent(MusicRegistrationEvent.OpenDatePicker)

        assertFalse(viewModel.uiState.value.showDatePicker)
    }

    @Test
    fun `DismissDatePicker sets showDatePicker false`() = runTest {
        createViewModel()
        viewModel.onEvent(MusicRegistrationEvent.OpenDatePicker)

        viewModel.onEvent(MusicRegistrationEvent.DismissDatePicker)

        assertFalse(viewModel.uiState.value.showDatePicker)
    }

    @Test
    fun `DatePicked sets selectedDate and closes picker`() = runTest {
        createViewModel()
        viewModel.onEvent(MusicRegistrationEvent.OpenDatePicker)

        val date = LocalDate.of(2025, 5, 4)
        viewModel.onEvent(MusicRegistrationEvent.DatePicked(date))

        assertEquals(date, viewModel.uiState.value.selectedDate)
        assertFalse(viewModel.uiState.value.showDatePicker)
    }

    // endregion

    // region SundayRows

    @Test
    fun `SundaySongSelected updates song in row`() = runTest {
        createViewModel()
        viewModel.onEvent(MusicRegistrationEvent.Init)
        songsFlow.value = SnapshotState.Data(fakeSongs)
        advanceUntilIdle()

        viewModel.onEvent(MusicRegistrationEvent.SundaySongSelected(1, fakeSongs[0]))

        val row = viewModel.uiState.value.sundayRows.find { it.position == 1 }!!
        assertEquals(fakeSongs[0].id, row.selectedSongId)
    }

    @Test
    fun `SundayToneChanged updates tone in row`() = runTest {
        createViewModel()

        viewModel.onEvent(MusicRegistrationEvent.SundayToneChanged(1, "G"))

        val row = viewModel.uiState.value.sundayRows.find { it.position == 1 }!!
        assertEquals("G", row.tone)
    }

    @Test
    fun `AddSundayRow adds a new row`() = runTest {
        createViewModel()
        val initialCount = viewModel.uiState.value.sundayRows.size

        viewModel.onEvent(MusicRegistrationEvent.AddSundayRow)

        assertEquals(initialCount + 1, viewModel.uiState.value.sundayRows.size)
    }

    @Test
    fun `RemoveSundayRow with position lte 4 does not remove`() = runTest {
        createViewModel()
        val initialCount = viewModel.uiState.value.sundayRows.size

        viewModel.onEvent(MusicRegistrationEvent.RemoveSundayRow(1))

        assertEquals(initialCount, viewModel.uiState.value.sundayRows.size)
    }

    @Test
    fun `RemoveSundayRow with position gt 4 removes the row`() = runTest {
        createViewModel()
        viewModel.onEvent(MusicRegistrationEvent.AddSundayRow) // position 5
        val countAfterAdd = viewModel.uiState.value.sundayRows.size

        viewModel.onEvent(MusicRegistrationEvent.RemoveSundayRow(5))

        assertEquals(countAfterAdd - 1, viewModel.uiState.value.sundayRows.size)
    }

    // endregion

    // region Submit

    @Test
    fun `Submit with MUSIC type shows not implemented message`() = runTest {
        createViewModel()
        viewModel.onEvent(MusicRegistrationEvent.RegistrationTypeChanged(RegistrationType.MUSIC))

        viewModel.onEvent(MusicRegistrationEvent.Submit)

        assertEquals("Registro de música ainda não implementado.", viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `Submit success shows success snackbar`() = runTest {
        createViewModel()
        viewModel.onEvent(MusicRegistrationEvent.Init)
        songsFlow.value = SnapshotState.Data(fakeSongs)
        advanceUntilIdle()

        // Fill rows so canSubmit = true
        viewModel.onEvent(MusicRegistrationEvent.SundaySongSelected(1, fakeSongs[0]))
        viewModel.onEvent(MusicRegistrationEvent.SundayToneChanged(1, "G"))
        viewModel.onEvent(MusicRegistrationEvent.SundaySongSelected(2, fakeSongs[1]))
        viewModel.onEvent(MusicRegistrationEvent.SundayToneChanged(2, "D"))
        // Remove empty rows
        viewModel.onEvent(MusicRegistrationEvent.RemoveSundayRow(3))
        viewModel.onEvent(MusicRegistrationEvent.RemoveSundayRow(4))

        coEvery { submitSundayPlaysUseCase(any(), any(), any()) } returns SubmitSundayPlaysUseCase.Result.Success

        viewModel.onEvent(MusicRegistrationEvent.Submit)
        advanceUntilIdle()

        assertEquals("Enviado com sucesso.", viewModel.uiState.value.snackbarMessage)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `Submit failure shows error snackbar`() = runTest {
        createViewModel()
        viewModel.onEvent(MusicRegistrationEvent.Init)
        songsFlow.value = SnapshotState.Data(fakeSongs)
        advanceUntilIdle()

        viewModel.onEvent(MusicRegistrationEvent.SundaySongSelected(1, fakeSongs[0]))
        viewModel.onEvent(MusicRegistrationEvent.SundayToneChanged(1, "G"))
        viewModel.onEvent(MusicRegistrationEvent.RemoveSundayRow(2))
        viewModel.onEvent(MusicRegistrationEvent.RemoveSundayRow(3))
        viewModel.onEvent(MusicRegistrationEvent.RemoveSundayRow(4))

        coEvery { submitSundayPlaysUseCase(any(), any(), any()) } returns
            SubmitSundayPlaysUseCase.Result.Failure("Erro no servidor")

        viewModel.onEvent(MusicRegistrationEvent.Submit)
        advanceUntilIdle()

        assertEquals("Erro no servidor", viewModel.uiState.value.snackbarMessage)
    }

    // endregion

    // region Snackbar

    @Test
    fun `SnackbarShown clears snackbar message`() = runTest {
        createViewModel()
        viewModel.onEvent(MusicRegistrationEvent.RegistrationTypeChanged(RegistrationType.MUSIC))
        viewModel.onEvent(MusicRegistrationEvent.Submit)
        assertTrue(viewModel.uiState.value.snackbarMessage != null)

        viewModel.onEvent(MusicRegistrationEvent.SnackbarShown)

        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    // endregion

    // region MusicForm

    @Test
    fun `MusicTitleChanged updates musicForm title`() = runTest {
        createViewModel()

        viewModel.onEvent(MusicRegistrationEvent.MusicTitleChanged("Oceans"))

        assertEquals("Oceans", viewModel.uiState.value.musicForm.title)
    }

    @Test
    fun `MusicArtistChanged updates musicForm artist`() = runTest {
        createViewModel()

        viewModel.onEvent(MusicRegistrationEvent.MusicArtistChanged("Hillsong"))

        assertEquals("Hillsong", viewModel.uiState.value.musicForm.artist)
    }

    // endregion
}
