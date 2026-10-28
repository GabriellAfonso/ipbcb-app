package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.domain.util.normalize
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.repository.ChordChartRepository
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.state.ChordChartCreateUiState
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChordChartCreateViewModel @Inject constructor(
    private val chordChartRepository: ChordChartRepository,
    private val songsRepository: SongsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChordChartCreateUiState())
    val uiState: StateFlow<ChordChartCreateUiState> = _uiState.asStateFlow()

    init {
        songsRepository.observeAllSongs()
            .onEach { state ->
                val songs = (state as? SnapshotState.Data)?.value.orEmpty()
                _uiState.update { it.copy(allSongs = songs, filteredSongs = filter(songs, it.songQuery)) }
            }
            .launchIn(viewModelScope)
    }

    fun onSongQueryChange(query: String) {
        _uiState.update { it.copy(songQuery = query, filteredSongs = filter(it.allSongs, query)) }
    }

    fun onSongSelected(song: Song) {
        _uiState.update { it.copy(selectedSong = song, songQuery = song.title, filteredSongs = emptyList()) }
    }

    /** "Trocar": back to an empty search. */
    fun onSongCleared() {
        _uiState.update { it.copy(selectedSong = null, songQuery = "", filteredSongs = filter(it.allSongs, "")) }
    }

    fun onContentChange(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    fun onToneChange(tone: String) {
        _uiState.update { it.copy(tone = tone) }
    }

    fun onInstrumentChange(instrument: String) {
        _uiState.update { it.copy(instrument = instrument) }
    }

    fun onSave() {
        val state = _uiState.value
        val song = state.selectedSong ?: return
        if (state.content.isBlank() || state.tone.isBlank() || state.instrument.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            chordChartRepository.createChordChart(song.id, state.content, state.tone, state.instrument)
                .onSuccess { _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, saveError = e.toAppError().toUserMessage()) }
                }
        }
    }

    private fun filter(songs: List<Song>, query: String): List<Song> =
        if (query.isBlank()) songs
        else songs.filter { it.title.normalize().contains(query.normalize(), ignoreCase = true) }
}
