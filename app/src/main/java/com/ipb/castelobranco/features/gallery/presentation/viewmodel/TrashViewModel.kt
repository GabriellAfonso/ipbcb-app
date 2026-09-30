package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.features.gallery.di.GalleryThumbnailLoader
import com.ipb.castelobranco.features.gallery.domain.trash.LoadTrashUseCase
import com.ipb.castelobranco.features.gallery.domain.trash.RestoreResult
import com.ipb.castelobranco.features.gallery.domain.trash.RestoreTrashItemUseCase
import com.ipb.castelobranco.features.gallery.domain.trash.TrashEntry
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKey
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKind
import com.ipb.castelobranco.features.gallery.presentation.state.TrashEvent
import com.ipb.castelobranco.features.gallery.presentation.state.TrashUiState
import com.ipb.castelobranco.features.gallery.presentation.state.toGalleryPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

/**
 * The trash screen: online only, read on every open (this ViewModel lives with the trash entry, not
 * with the gallery graph). One restore at a time; a success is already in the gallery's local copy
 * when its row leaves.
 */
@HiltViewModel
class TrashViewModel @Inject constructor(
    private val loadTrash: LoadTrashUseCase,
    private val restoreItem: RestoreTrashItemUseCase,
    observeAccess: ObserveAccessUseCase,
    @param:GalleryThumbnailLoader val previewLoader: ImageLoader,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val _state = MutableStateFlow(TrashUiState())
    val state: StateFlow<TrashUiState> = _state.asStateFlow()

    // A channel keeps an event until the screen collects it again (e.g. back from "Abrir álbum").
    private val _events = Channel<TrashEvent>(Channel.BUFFERED)
    val events: Flow<TrashEvent> = _events.receiveAsFlow()

    private var entries: List<TrashEntry> = emptyList()
    private var loadJob: Job? = null

    init {
        load()
        // Close only after `owner` was seen: the first emission may come before the profile.
        viewModelScope.launch {
            var hadOwner = false
            observeAccess()
                .map { it.toGalleryPermissions().canDelete }
                .distinctUntilChanged()
                .collect { isOwner ->
                    if (isOwner) {
                        hadOwner = true
                    } else if (hadOwner) {
                        _events.send(TrashEvent.Close(TrashTexts.ACCESS_LOST))
                    }
                }
        }
    }

    fun retry() = load()

    /** Pull-to-refresh; ignored while a restore runs, so its row never disappears under it. */
    fun refresh() {
        if (_state.value.canRefresh) load()
    }

    fun restore(key: TrashKey) {
        if (_state.value.restoring != null) return
        _state.update { it.copy(restoring = key, highlighted = null) }
        viewModelScope.launch {
            val result = restoreItem(key)
            _state.update { it.copy(restoring = null) }
            val text = TrashTexts.result(result, key.kind)
            when (result) {
                RestoreResult.Restored -> {
                    entries = entries.filterNot { it.key == key }
                    _state.update { state -> state.copy(rows = state.rows.filterNot { it.key == key }) }
                    _events.send(TrashEvent.Message(text))
                }
                RestoreResult.NotInTrash -> {
                    _events.send(TrashEvent.Message(text))
                    load()
                }
                is RestoreResult.TrashedParent -> {
                    val parent = TrashKey(TrashKind.ALBUM, result.parentAlbumId)
                    if (entries.any { it.key == parent }) _state.update { it.copy(highlighted = parent) }
                    _events.send(TrashEvent.Message(text))
                }
                is RestoreResult.NameConflict -> _events.send(
                    TrashEvent.Message(text, openAlbumId = result.conflictingAlbumId.takeIf { result.isOnDevice }),
                )
                is RestoreResult.Failed -> _events.send(TrashEvent.Message(text))
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        _state.update { state ->
            if (state.rows.isEmpty()) {
                state.copy(isLoading = true, error = null, highlighted = null)
            } else {
                state.copy(isRefreshing = true, highlighted = null)
            }
        }
        loadJob = viewModelScope.launch {
            loadTrash().fold(
                onSuccess = { list ->
                    entries = list
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            rows = list.map { TrashTexts.row(it, zone) },
                        )
                    }
                },
                onFailure = { failure ->
                    val text = TrashTexts.readError(failure.toAppError())
                    val hadRows = _state.value.rows.isNotEmpty()
                    _state.update { state ->
                        state.copy(isLoading = false, isRefreshing = false, error = text.takeUnless { hadRows })
                    }
                    if (hadRows) _events.send(TrashEvent.Message(text))
                },
            )
        }
    }
}
