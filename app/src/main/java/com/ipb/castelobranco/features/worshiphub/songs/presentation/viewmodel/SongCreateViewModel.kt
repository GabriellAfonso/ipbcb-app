package com.ipb.castelobranco.features.worshiphub.songs.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.worshiphub.register.domain.usecase.RegisterSongUseCase
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.SongCreateUiState
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SongCreateViewModel @Inject constructor(
    private val registerSong: RegisterSongUseCase,
    private val songsRepository: SongsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongCreateUiState())
    val uiState: StateFlow<SongCreateUiState> = _uiState.asStateFlow()

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title, saveError = null) }
    }

    fun onArtistChange(artist: String) {
        _uiState.update { it.copy(artist = artist, saveError = null) }
    }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            registerSong(title = state.title, artist = state.artist).fold(
                onSuccess = {
                    // The catalogue is shared: the new song shows up in the list, Sunday rows and the repertoire.
                    runCatching { songsRepository.refreshAllSongs() }
                        .onFailure { Timber.w(it, "Failed to refresh songs after creating one") }
                    _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to register song")
                    _uiState.update { it.copy(isSaving = false, saveError = errorMessage(error.toAppError())) }
                },
            )
        }
    }

    private fun errorMessage(error: AppError): String = when {
        error is AppError.Server && error.code == HTTP_CONFLICT -> "Essa música já está cadastrada."
        error is AppError.Server && error.code == HTTP_BAD_REQUEST ->
            "Título ou artista inválido. Use até 100 caracteres."
        else -> error.toUserMessage()
    }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_CONFLICT = 409
    }
}
