package com.ipb.castelobranco.features.worshiphub.register.presentation.viewmodel

import timber.log.Timber
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.register.domain.usecase.ObserveSongsUseCase
import com.ipb.castelobranco.features.worshiphub.register.domain.usecase.SubmitSundayPlaysUseCase
import com.ipb.castelobranco.features.worshiphub.tables.domain.usecase.RefreshPlayedTablesUseCase
import com.ipb.castelobranco.features.worshiphub.register.domain.validation.SundayRegistrationValidator
import com.ipb.castelobranco.features.worshiphub.register.presentation.state.SundayRegistrationEvent
import com.ipb.castelobranco.features.worshiphub.register.presentation.state.SundayRegistrationUiState
import com.ipb.castelobranco.features.worshiphub.register.presentation.util.addRow
import com.ipb.castelobranco.features.worshiphub.register.presentation.util.removeRow
import com.ipb.castelobranco.features.worshiphub.register.presentation.util.selectSong
import com.ipb.castelobranco.features.worshiphub.register.presentation.util.updateTone
import com.ipb.castelobranco.core.domain.model.Song
import androidx.lifecycle.SavedStateHandle
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.domain.setlist.SundaySetlist
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.core.domain.setlist.GetSetlistForDateUseCase
import com.ipb.castelobranco.features.worshiphub.register.presentation.state.PrefillState
import com.ipb.castelobranco.features.worshiphub.register.presentation.state.SundaySongRowState
import com.ipb.castelobranco.features.worshiphub.register.presentation.util.SongLabelFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SundayRegistrationViewModel @Inject constructor(
    private val observeSongsUseCase: ObserveSongsUseCase,
    private val submitSundayPlaysUseCase: SubmitSundayPlaysUseCase,
    private val getSetlistForDate: GetSetlistForDateUseCase,
    private val refreshPlayedTables: RefreshPlayedTablesUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Set when opened to confirm a Sunday (notification or pending card): its setlist pre-fills the rows. */
    private val prefillDate: LocalDate? = savedStateHandle.get<String>(AppRoutes.ARG_SUNDAY_DATE)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private val _uiState = MutableStateFlow(
        SundayRegistrationUiState(
            selectedDate = prefillDate ?: LocalDate.now(),
            prefillDate = prefillDate,
        )
    )
    val uiState: StateFlow<SundayRegistrationUiState> = _uiState.asStateFlow()

    private var songsJob: Job? = null

    init {
        if (prefillDate != null) loadPrefill(prefillDate)
    }

    private fun loadPrefill(date: LocalDate) {
        _uiState.update { it.copy(prefill = PrefillState.Loading) }
        viewModelScope.launch {
            getSetlistForDate(date).fold(
                onSuccess = { setlist ->
                    _uiState.update { state ->
                        state.copy(sundayRows = rowsFrom(setlist), prefill = PrefillState.Loaded)
                            .recomputeSundayErrors()
                    }
                },
                onFailure = { error ->
                    val appError = error.toAppError()
                    _uiState.update { state ->
                        if (appError is AppError.Server && appError.code == HTTP_NOT_FOUND) {
                            state.copy(
                                prefill = PrefillState.NotFound,
                                snackbarMessage = "Repertório de ${date.format(DAY_MONTH)} não encontrado. " +
                                    "Preencha as músicas.",
                            )
                        } else {
                            state.copy(prefill = PrefillState.Failed(appError.toUserMessage()))
                        }
                    }
                },
            )
        }
    }

    /** One row per setlist item, in order, never fewer than the screen's default rows. */
    private fun rowsFrom(setlist: SundaySetlist): List<SundaySongRowState> {
        val items = setlist.items.sortedBy { it.position }
        val count = maxOf(SundayRegistrationUiState.defaultSundayRows().size, items.size)
        return (1..count).map { position ->
            val item = items.getOrNull(position - 1)
            if (item == null) {
                SundaySongRowState(position = position)
            } else {
                val song = Song(id = item.songId, title = item.title, artist = item.artist, categoryName = "")
                SundaySongRowState(
                    position = position,
                    songQuery = SongLabelFormatter.format(song),
                    selectedSongId = item.songId,
                    tone = item.tone,
                )
            }
        }
    }

    fun onEvent(event: SundayRegistrationEvent) {
        when (event) {
            SundayRegistrationEvent.Init -> init()

            SundayRegistrationEvent.OpenDatePicker -> openDatePicker()
            SundayRegistrationEvent.DismissDatePicker -> dismissDatePicker()
            is SundayRegistrationEvent.DatePicked -> confirmDate(event.date)

            is SundayRegistrationEvent.SundaySongSelected -> selectSong(event.position, event.song)
            is SundayRegistrationEvent.SundayToneChanged -> updateTone(event.position, event.tone)

            SundayRegistrationEvent.AddSundayRow -> addRow()
            is SundayRegistrationEvent.RemoveSundayRow -> removeRow(event.position)

            SundayRegistrationEvent.Submit -> submit()
            SundayRegistrationEvent.SnackbarShown -> consumeSnackbar()
            SundayRegistrationEvent.RetryPrefill -> prefillDate?.let { loadPrefill(it) }
        }
    }

    /** Observes the catalog once per ViewModel, but refreshes it every time the screen opens. */
    private fun init() {
        if (songsJob == null) observeSongs()
        refreshSongs()
    }

    private fun observeSongs() {
        songsJob = viewModelScope.launch {
            observeSongsUseCase.observe().collect { snapshot ->
                when (snapshot) {
                    is SnapshotState.Loading -> {
                        _uiState.update { it.copy(isLoadingSongs = true) }
                    }
                    is SnapshotState.Data -> {
                        _uiState.update { state ->
                            state.copy(
                                availableSongs = snapshot.value,
                                isLoadingSongs = false
                            ).recomputeSundayErrors()
                        }
                    }
                    is SnapshotState.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingSongs = false,
                                snackbarMessage = "Falha ao carregar músicas."
                            )
                        }
                    }
                }
            }
        }
    }

    private fun refreshSongs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSongs = true) }
            try {
                observeSongsUseCase.refresh()
            } catch (e: Throwable) {
                Timber.w(e, "Failed to refresh songs")
                _uiState.update { it.copy(snackbarMessage = "Falha ao atualizar músicas.") }
            } finally {
                _uiState.update { it.copy(isLoadingSongs = false) }
            }
        }
    }

    private fun openDatePicker() {
        // Confirming a Sunday: the date belongs to the setlist and cannot change.
        if (prefillDate != null) return
        _uiState.update { it.copy(showDatePicker = true) }
    }

    private fun dismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    private fun confirmDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date, showDatePicker = false) }
    }

    private fun selectSong(position: Int, song: Song) {
        _uiState.update { state ->
            state.copy(sundayRows = selectSong(state.sundayRows, position, song))
                .recomputeSundayErrors()
        }
    }

    private fun updateTone(position: Int, tone: String) {
        _uiState.update { state ->
            state.copy(sundayRows = updateTone(state.sundayRows, position, tone))
                .recomputeSundayErrors()
        }
    }

    private fun addRow() {
        _uiState.update { state ->
            state.copy(sundayRows = addRow(state.sundayRows))
                .recomputeSundayErrors()
        }
    }

    private fun removeRow(position: Int) {
        _uiState.update { state ->
            state.copy(sundayRows = removeRow(state.sundayRows, position))
                .recomputeSundayErrors()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        if (!state.canSubmit) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = submitSundayPlaysUseCase(
                rows = state.sundayRows,
                availableSongs = state.availableSongs,
                selectedDate = state.selectedDate
            )
            _uiState.update { current ->
                when (result) {
                    is SubmitSundayPlaysUseCase.Result.ValidationError -> current.copy(
                        sundayRowErrors = result.errorsByPosition,
                        snackbarMessage = "Existem posições incompletas. Corrija antes de enviar.",
                        isSubmitting = false
                    )
                    SubmitSundayPlaysUseCase.Result.Success -> current.copy(
                        snackbarMessage = "Enviado com sucesso.",
                        isSubmitting = false
                    )
                    is SubmitSundayPlaysUseCase.Result.Failure -> current.copy(
                        snackbarMessage = result.message,
                        isSubmitting = false
                    )
                }
            }
            if (result == SubmitSundayPlaysUseCase.Result.Success) {
                runCatching { refreshPlayedTables() }
                    .onFailure { Timber.w(it, "Failed to refresh the played tables") }
            }
        }
    }

    private fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun SundayRegistrationUiState.recomputeSundayErrors(): SundayRegistrationUiState {
        val validation = SundayRegistrationValidator.validateSundayRows(sundayRows, availableSongs)
        return copy(sundayRowErrors = validation.errorsByPosition)
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
        val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
    }
}
