package com.ipb.castelobranco.features.worshiphub.songs.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongEditFields
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongWriteResult
import com.ipb.castelobranco.features.worshiphub.songs.domain.usecase.DeleteSongUseCase
import com.ipb.castelobranco.features.worshiphub.songs.domain.usecase.GetSongDetailUseCase
import com.ipb.castelobranco.features.worshiphub.songs.domain.usecase.UpdateSongUseCase
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.ChordChartOption
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.SongDetailEvent
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.SongDetailUiState
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.SongEditFormState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class WriteState(
    val edit: SongEditFormState? = null,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
    /** Set once the delete succeeds, so the screen keeps a spinner instead of "not found" while it leaves. */
    val isDeleted: Boolean = false,
)

@HiltViewModel
class SongDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getSongDetailUseCase: GetSongDetailUseCase,
    observeAccess: ObserveAccessUseCase,
    private val updateSongUseCase: UpdateSongUseCase,
    private val deleteSongUseCase: DeleteSongUseCase,
) : ViewModel() {

    private val songId: Int = savedStateHandle["songId"]!!

    private val _writeState = MutableStateFlow(WriteState())

    private val _events = MutableSharedFlow<SongDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SongDetailEvent> = _events.asSharedFlow()

    val uiState: StateFlow<SongDetailUiState> = combine(
        getSongDetailUseCase.observe(songId),
        observeAccess(),
        _writeState,
    ) { state, access, write ->
        val canEdit = access.allows(Scope.SONGS, AccessLevel.MANAGE)
        val canDelete = access.allows(Scope.SONGS, AccessLevel.OWNER)
        when {
            write.isDeleted -> SongDetailUiState(isLoading = true)
            state is SnapshotState.Loading -> SongDetailUiState(isLoading = true)
            state is SnapshotState.Error   -> SongDetailUiState(error = state.error.toUserMessage())
            state is SnapshotState.Data    -> {
                val detail = state.value
                SongDetailUiState(
                    songName    = detail.song.title,
                    artist      = detail.song.artist,
                    playCount   = detail.playCount,
                    tones       = detail.tones,
                    lastSundays = detail.lastSundays,
                    chordCharts = detail.chordCharts.map { chart ->
                        ChordChartOption(
                            id         = chart.id,
                            tone       = chart.tone,
                            instrument = chart.instrument,
                        )
                    },
                    hasLyrics   = detail.lyrics != null,
                    lyricsId    = detail.lyrics?.id,
                    youtubeLink = detail.song.youtubeLink,
                    canEdit     = canEdit,
                    canDelete   = canDelete,
                    edit        = write.edit.takeIf { canEdit },
                    isDeleting  = write.isDeleting,
                    deleteError = write.deleteError,
                )
            }
            else -> SongDetailUiState(isLoading = true)
        }
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = SongDetailUiState(isLoading = true),
    )

    fun enterEditMode() {
        val current = uiState.value
        _writeState.update {
            it.copy(
                edit = SongEditFormState(
                    title       = current.songName,
                    artist      = current.artist,
                    youtubeLink = current.youtubeLink.orEmpty(),
                ),
            )
        }
    }

    fun onTitleChange(text: String) = updateForm {
        it.copy(title = text, fieldErrors = it.fieldErrors.copy(title = null))
    }

    fun onArtistChange(text: String) = updateForm {
        it.copy(artist = text, fieldErrors = it.fieldErrors.copy(artist = null))
    }

    fun onYoutubeLinkChange(text: String) = updateForm {
        it.copy(youtubeLink = text, fieldErrors = it.fieldErrors.copy(youtubeLink = null))
    }

    fun cancelEdit() {
        _writeState.update { it.copy(edit = null) }
    }

    fun saveEdit() {
        val form = _writeState.value.edit ?: return
        if (form.isSaving) return
        val current = uiState.value
        val original = SongEditFields(current.songName, current.artist, current.youtubeLink.orEmpty())
        val edited = SongEditFields(form.title, form.artist, form.youtubeLink)

        updateForm { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            when (val result = updateSongUseCase(songId, original, edited)) {
                SongWriteResult.Success -> _writeState.update { it.copy(edit = null) }
                is SongWriteResult.Invalid -> updateForm { it.copy(isSaving = false, fieldErrors = result.errors) }
                else -> updateForm { it.copy(isSaving = false, saveError = result.saveMessage()) }
            }
        }
    }

    fun deleteSong() {
        if (_writeState.value.isDeleting) return
        _writeState.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            when (val result = deleteSongUseCase(songId)) {
                SongWriteResult.Success -> {
                    _writeState.update { it.copy(isDeleting = false, isDeleted = true) }
                    _events.emit(SongDetailEvent.Deleted)
                }
                else -> _writeState.update { it.copy(isDeleting = false, deleteError = result.deleteMessage()) }
            }
        }
    }

    fun dismissDeleteError() {
        _writeState.update { it.copy(deleteError = null) }
    }

    private fun updateForm(transform: (SongEditFormState) -> SongEditFormState) {
        _writeState.update { state -> state.edit?.let { state.copy(edit = transform(it)) } ?: state }
    }

    private fun SongWriteResult.saveMessage(): String = when (this) {
        SongWriteResult.Duplicate    -> DUPLICATE_MESSAGE
        SongWriteResult.NoPermission -> NO_EDIT_PERMISSION_MESSAGE
        SongWriteResult.NotFound     -> NOT_FOUND_MESSAGE
        is SongWriteResult.Failed    -> error.toAppError().toUserMessage()
        else                         -> GENERIC_ERROR_MESSAGE
    }

    private fun SongWriteResult.deleteMessage(): String = when (this) {
        is SongWriteResult.InUse     -> message ?: IN_USE_MESSAGE
        SongWriteResult.NoPermission -> NO_DELETE_PERMISSION_MESSAGE
        SongWriteResult.NotFound     -> NOT_FOUND_MESSAGE
        is SongWriteResult.Failed    -> error.toAppError().toUserMessage()
        else                         -> GENERIC_ERROR_MESSAGE
    }

    private companion object {
        const val DUPLICATE_MESSAGE = "Já existe uma música com esse nome e artista."
        const val NO_EDIT_PERMISSION_MESSAGE = "Você não tem permissão para editar músicas."
        const val NO_DELETE_PERMISSION_MESSAGE = "Você não tem permissão para excluir músicas."
        const val NOT_FOUND_MESSAGE = "Essa música não existe mais."
        const val IN_USE_MESSAGE = "Essa música já foi usada em domingos ou setlists e não pode ser excluída."
        const val GENERIC_ERROR_MESSAGE = "Não foi possível completar a operação. Tente novamente mais tarde."
    }
}
