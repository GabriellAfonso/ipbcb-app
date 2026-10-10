package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.data.local.SetlistPreferences
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.core.domain.access.Access
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.usecase.GetChordChartsUseCase
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.state.ChordChartListItem
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.state.ChordChartsUiState
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import com.ipb.castelobranco.core.domain.setlist.ObserveSundaySetlistUseCase
import com.ipb.castelobranco.core.domain.setlist.SyncSundaySetlistUseCase
import com.ipb.castelobranco.features.worshiphub.shared.domain.ContentSearch
import com.ipb.castelobranco.features.worshiphub.shared.domain.buildSundaySection
import com.ipb.castelobranco.features.worshiphub.shared.domain.matchesTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChordChartsViewModel @Inject constructor(
    private val getChordChartsUseCase: GetChordChartsUseCase,
    private val songsRepository: SongsRepository,
    private val setlistPreferences: SetlistPreferences,
    observeAccess: ObserveAccessUseCase,
    observeSundaySetlist: ObserveSundaySetlistUseCase,
    private val syncSundaySetlist: SyncSundaySetlistUseCase,
) : ViewModel() {

    private val _query = MutableStateFlow("")

    /** "Buscar na letra": off on every visit, so the plain search stays name-only. */
    private val _searchLyrics = MutableStateFlow(false)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val pinnedSongs = setlistPreferences.pinnedSongIds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val access = observeAccess()

    private val search = combine(_query, _searchLyrics, access) { query, searchLyrics, access ->
        SearchInput(query, searchLyrics, access)
    }

    // Content is normalized once per snapshot, so each keystroke only runs `contains`.
    private val indexed = getChordChartsUseCase.observe().map { state ->
        val index = (state as? SnapshotState.Data)?.value.orEmpty()
            .associate { it.id to ContentSearch.index(it.content) }
        state to index
    }

    fun refresh(minDurationMs: Long = 600L) {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            val start = System.currentTimeMillis()
            // Pulling also re-reads the Sunday setlist: the gesture people try when it has not shown up.
            val setlist = launch {
                runCatching { syncSundaySetlist() }.getOrElse { Result.failure(it) }
                    .onFailure { Timber.w(it, "Failed to refresh the Sunday setlist") }
            }
            runCatching { getChordChartsUseCase.refresh() }
                .onFailure { Timber.w(it, "Failed to refresh chord charts") }
            setlist.join()
            val elapsed = System.currentTimeMillis() - start
            if (elapsed < minDurationMs) delay(minDurationMs - elapsed)
            _isRefreshing.value = false
        }
    }

    val uiState: StateFlow<ChordChartsUiState> = combine(
        indexed,
        songsRepository.observeAllSongs(),
        pinnedSongs,
        search,
        observeSundaySetlist(),
    ) { (chartsState, contentIndex), songsState, pinnedSongIds, (query, searchLyrics, access), sundaySetlist ->
        val songMap = (songsState as? SnapshotState.Data)?.value
            .orEmpty()
            .associateBy { it.id }

        val canEdit = access.allows(Scope.SONGS, AccessLevel.MANAGE)

        when (chartsState) {
            is SnapshotState.Loading -> ChordChartsUiState(isLoading = true, canEdit = canEdit)
            is SnapshotState.Error   -> ChordChartsUiState(error = chartsState.error.toUserMessage(), canEdit = canEdit)
            is SnapshotState.Data    -> {
                val pinOrder = pinnedSongIds.withIndex().associate { (index, songId) -> songId to index }
                val sorted = chartsState.value.map { chart ->
                    ChordChartListItem(
                        id         = chart.id,
                        songId     = chart.songId,
                        songName   = songMap[chart.songId]?.title ?: "Song #${chart.songId}",
                        tone       = chart.tone,
                        instrument = chart.instrument,
                        isPinned   = chart.songId in pinOrder,
                    )
                }.sortedBy { pinOrder[it.songId] ?: Int.MAX_VALUE }

                // The Sunday section only shows without a search; its songs are not repeated below it.
                val section = if (query.isBlank()) {
                    buildSundaySection(
                        setlist = sundaySetlist,
                        contentBySongId = chartsState.value.reversed().associate { it.songId to it.id },
                    )
                } else {
                    null
                }

                val filtered = if (query.isBlank()) sorted.filterNot { section?.songIds?.contains(it.songId) == true }
                else {
                    val (byTitle, others) = sorted.partition { matchesTitle(it.songName, query) }
                    if (!searchLyrics) byTitle
                    else byTitle + others.mapNotNull { item ->
                        contentIndex[item.id]?.findSnippet(query)?.let { item.copy(lyricsSnippet = it) }
                    }
                }

                ChordChartsUiState(
                    charts         = sorted,
                    filteredCharts = filtered,
                    query          = query,
                    searchLyrics   = searchLyrics,
                    canEdit        = canEdit,
                    sundaySection  = section,
                )
            }
        }
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = ChordChartsUiState(isLoading = true),
    )

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun onSearchLyricsChange(enabled: Boolean) {
        _searchLyrics.value = enabled
    }

    fun onTogglePin(songId: Int) {
        viewModelScope.launch { setlistPreferences.toggleSong(songId) }
    }
}

private data class SearchInput(
    val query: String,
    val searchLyrics: Boolean,
    val access: Access,
)
