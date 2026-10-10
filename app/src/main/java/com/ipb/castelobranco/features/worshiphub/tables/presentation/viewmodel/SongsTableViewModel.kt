package com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SuggestedSong
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySet
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopSong
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopTone
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.DraftRow
import com.ipb.castelobranco.features.worshiphub.tables.domain.usecase.ClearRepertoireDraftUseCase
import com.ipb.castelobranco.features.worshiphub.tables.domain.usecase.RestoreRepertoireDraftUseCase
import com.ipb.castelobranco.features.worshiphub.tables.domain.usecase.SaveRepertoireDraftUseCase
import com.ipb.castelobranco.core.domain.setlist.setlistDateFor
import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.core.domain.worship.ObserveWorshipAccessUseCase
import com.ipb.castelobranco.core.di.DefaultDispatcher
import com.ipb.castelobranco.features.worshiphub.tables.domain.usecase.SearchSundaysUseCase
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SaveSetlistResult
import com.ipb.castelobranco.features.worshiphub.tables.domain.usecase.RepertoireValidation
import com.ipb.castelobranco.features.worshiphub.tables.domain.usecase.SaveSundaySetlistUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * @param canSave the profile allows saving the Sunday setlist — shows "Salvar".
 * @param pendingSaveDate the Sunday the confirmation dialog is asking about; `null` when it is closed.
 */
data class RepertoireSaveState(
    val canSave: Boolean = false,
    val isSaveEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val pendingSaveDate: LocalDate? = null,
)

sealed interface RepertoireEvent {
    data class Saved(val message: String) : RepertoireEvent
    data class SaveFailed(val message: String) : RepertoireEvent
}

data class RepertoireRowState(
    val position: Int,
    val selectedSong: Song? = null,
    val tone: String = "",
    val isFixed: Boolean = false
)

@HiltViewModel
class SongsTableViewModel @Inject constructor(
    private val repository: SongsRepository,
    private val restoreDraft: RestoreRepertoireDraftUseCase,
    private val saveDraft: SaveRepertoireDraftUseCase,
    private val clearDraft: ClearRepertoireDraftUseCase,
    observeWorshipAccess: ObserveWorshipAccessUseCase,
    private val dateProvider: DateProvider,
    private val saveSetlist: SaveSundaySetlistUseCase,
    private val searchSundays: SearchSundaysUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    val allSongs: StateFlow<List<Song>> = repository.observeAllSongs()
        .map { state -> if (state is SnapshotState.Data) state.value else emptyList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun refreshAllSongs() {
        viewModelScope.launch {
            runCatching { repository.refreshAllSongs() }
                .onFailure { Timber.w(it, "Failed to refresh all songs") }
        }
    }

    val lastSundays: StateFlow<SnapshotState<List<SundaySet>>> = repository.observeSongsBySunday()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SnapshotState.Loading)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /** [lastSundays] filtered by [searchQuery]; normalizing runs once per data load, off the main thread. */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val filteredSundays: StateFlow<SnapshotState<List<SundaySet>>> = combine(
        lastSundays.map { state ->
            when (state) {
                is SnapshotState.Data -> SnapshotState.Data(searchSundays.index(state.value))
                is SnapshotState.Loading -> state
                is SnapshotState.Error -> state
            }
        },
        // Empty query (opening the tab, clearing the field) skips the debounce so the full list is instant.
        _searchQuery.debounce { query -> if (query.isEmpty()) 0L else SEARCH_DEBOUNCE_MS },
    ) { state, query -> state to query }
        .mapLatest { (state, query) ->
            when (state) {
                is SnapshotState.Data -> SnapshotState.Data(searchSundays(state.value, query))
                is SnapshotState.Loading -> state
                is SnapshotState.Error -> state
            }
        }
        .flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.Eagerly, SnapshotState.Loading)

    val topSongs: StateFlow<SnapshotState<List<TopSong>>> = repository.observeTopSongs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SnapshotState.Loading)

    val topTones: StateFlow<SnapshotState<List<TopTone>>> = repository.observeTopTones()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SnapshotState.Loading)

    val suggestedSongs: StateFlow<SnapshotState<List<SuggestedSong>>> = repository.observeSuggestedSongs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SnapshotState.Loading)

    private val _isRefreshingSuggestedSongs = MutableStateFlow(false)
    val isRefreshingSuggestedSongs: StateFlow<Boolean> = _isRefreshingSuggestedSongs.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshCurrentTab(tabIndex: Int, minDurationMs: Long = 600L) {
        if (tabIndex == 3) return // Repertório has its own generate button
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            val start = System.currentTimeMillis()
            runCatching {
                when (tabIndex) {
                    0 -> repository.refreshSongsBySunday()
                    1 -> repository.refreshTopSongs()
                    2 -> repository.refreshTopTones()
                }
            }.onFailure { Timber.w(it, "Failed to refresh tab %d", tabIndex) }
            val elapsed = System.currentTimeMillis() - start
            if (elapsed < minDurationMs) delay(minDurationMs - elapsed)
            _isRefreshing.value = false
        }
    }

    private val _repertoireRows = MutableStateFlow(emptyRows())
    val repertoireRows: StateFlow<List<RepertoireRowState>> = _repertoireRows.asStateFlow()

    private val isSaving = MutableStateFlow(false)
    private val pendingSaveDate = MutableStateFlow<LocalDate?>(null)

    val saveState: StateFlow<RepertoireSaveState> = combine(
        observeWorshipAccess(),
        _repertoireRows,
        isSaving,
        pendingSaveDate,
    ) { access, rows, saving, pending ->
        RepertoireSaveState(
            canSave = access.canSaveSetlist,
            isSaveEnabled = !saving && RepertoireValidation.canSave(rows.toDraftRows()),
            isSaving = saving,
            pendingSaveDate = pending,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RepertoireSaveState())

    private val _events = MutableSharedFlow<RepertoireEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RepertoireEvent> = _events.asSharedFlow()

    init {
        restoreDraftWhenCatalogLoads()
    }

    /** Opens the confirmation for the Sunday the setlist applies to: today when Sunday, else the next one. */
    fun requestSave() {
        if (!saveState.value.isSaveEnabled) return
        pendingSaveDate.value = setlistDateFor(dateProvider.today())
    }

    fun dismissSave() {
        pendingSaveDate.value = null
    }

    fun confirmSave() {
        val date = pendingSaveDate.value ?: return
        pendingSaveDate.value = null
        if (isSaving.value) return
        isSaving.value = true
        val rows = _repertoireRows.value.toDraftRows()
        viewModelScope.launch {
            try {
                val event = when (val result = saveSetlist(date, rows)) {
                    is SaveSetlistResult.Saved -> RepertoireEvent.Saved(RepertoireTexts.saved(result.setlist.date))
                    is SaveSetlistResult.Failed -> RepertoireEvent.SaveFailed(RepertoireTexts.failure(result.failure))
                }
                _events.emit(event)
            } finally {
                isSaving.value = false
            }
        }
    }

    /**
     * The draft is restored once, when the song catalog is known: a row is emptied only when the
     * catalog exists and lacks its song. Restoring does not count as a change, so it never renews the hour.
     */
    private fun restoreDraftWhenCatalogLoads() {
        viewModelScope.launch {
            val catalog = (repository.observeAllSongs().firstOrNull { it is SnapshotState.Data }
                as? SnapshotState.Data)?.value ?: return@launch
            val songsById = catalog.associateBy { it.id }
            val restored = runCatching { restoreDraft(songsById.keys) }
                .onFailure { Timber.w(it, "Failed to restore repertoire draft") }
                .getOrNull() ?: return@launch
            _repertoireRows.update { current ->
                current.map { row ->
                    val draftRow = restored.find { it.position == row.position } ?: return@map row
                    val song = draftRow.songId?.let { songsById[it] }
                    row.copy(
                        selectedSong = song,
                        tone = if (song == null) "" else draftRow.tone,
                        isFixed = song != null && draftRow.isFixed,
                    )
                }
            }
        }
    }

    private fun persistDraft() {
        val rows = _repertoireRows.value.toDraftRows()
        viewModelScope.launch {
            runCatching { saveDraft(rows) }
                .onFailure { Timber.w(it, "Failed to save repertoire draft") }
        }
    }

    /** "Limpar repertório": empty rows and no draft left on the device. */
    fun clearRepertoire() {
        _repertoireRows.value = emptyRows()
        viewModelScope.launch {
            runCatching { clearDraft() }
                .onFailure { Timber.w(it, "Failed to clear repertoire draft") }
        }
    }

    fun selectSong(position: Int, song: Song?) {
        val autoTone = song?.let { mostUsedToneFor(it) } ?: ""
        _repertoireRows.update { rows ->
            rows.map { row ->
                if (row.position == position) {
                    row.copy(
                        selectedSong = song,
                        tone = autoTone,
                        isFixed = if (song == null) false else row.isFixed
                    )
                } else row
            }
        }
        persistDraft()
    }

    fun onToneChange(position: Int, tone: String) {
        _repertoireRows.update { rows ->
            rows.map { row ->
                if (row.position == position) row.copy(tone = tone) else row
            }
        }
        persistDraft()
    }

    fun toggleFixed(position: Int) {
        _repertoireRows.update { rows ->
            rows.map { row ->
                if (row.position == position && row.selectedSong != null) {
                    row.copy(isFixed = !row.isFixed)
                } else row
            }
        }
        persistDraft()
    }

    private fun mostUsedToneFor(song: Song): String {
        val sundays = (lastSundays.value as? SnapshotState.Data)?.value ?: return ""
        return sundays
            .flatMap { it.songs }
            .filter { it.title == song.title && it.artist == song.artist }
            .takeIf { it.isNotEmpty() }
            ?.groupingBy { it.tone }
            ?.eachCount()
            ?.maxByOrNull { it.value }
            ?.key
            .orEmpty()
    }

    fun refreshSuggestedSongs(minDurationMs: Long = 600L) {
        viewModelScope.launch {
            if (_isRefreshingSuggestedSongs.value) return@launch
            _isRefreshingSuggestedSongs.value = true

            try {
                val fixed = _repertoireRows.value
                    .filter { it.isFixed && it.selectedSong != null }
                    .associate { it.position to it.selectedSong!!.id }

                val refreshJob = async { repository.refreshSuggestedSongs(fixed) }
                val minTimeJob = async { delay(minDurationMs) }

                refreshJob.await()
                minTimeJob.await()

                syncRepertoireFromSuggestions()
            } catch (e: Exception) {
                Timber.w(e, "Failed to refresh suggested songs")
            } finally {
                _isRefreshingSuggestedSongs.value = false
            }
        }
    }

    private suspend fun syncRepertoireFromSuggestions() {
        val suggestionsState = repository.observeSuggestedSongs().first()
        val suggestions = (suggestionsState as? SnapshotState.Data)?.value ?: return
        val songs = (repository.observeAllSongs().first() as? SnapshotState.Data)?.value ?: emptyList()

        _repertoireRows.update { rows ->
            rows.map { row ->
                if (row.isFixed) return@map row
                val suggestion = suggestions.find { it.position == row.position }
                if (suggestion != null) {
                    val song = songs.find { it.id == suggestion.songId }
                        ?: row.selectedSong
                    row.copy(selectedSong = song, tone = suggestion.tone)
                } else {
                    row
                }
            }
        }
        persistDraft()
    }

    sealed class SubmitResult {
        data object Success : SubmitResult()
        data class Error(val message: String) : SubmitResult()
    }

    private companion object {
        const val ROW_COUNT = 4
        const val SEARCH_DEBOUNCE_MS = 150L

        fun emptyRows() = (1..ROW_COUNT).map { RepertoireRowState(position = it) }

        fun List<RepertoireRowState>.toDraftRows() = map { row ->
            DraftRow(position = row.position, songId = row.selectedSong?.id, tone = row.tone, isFixed = row.isFixed)
        }
    }
}
