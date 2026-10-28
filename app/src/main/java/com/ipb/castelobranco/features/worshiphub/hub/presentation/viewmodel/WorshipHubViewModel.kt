package com.ipb.castelobranco.features.worshiphub.hub.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.setlist.DeletePendingSetlistUseCase
import com.ipb.castelobranco.core.domain.setlist.ObserveSundaySetlistUseCase
import com.ipb.castelobranco.core.domain.setlist.SundaySetlistRepository
import com.ipb.castelobranco.core.domain.setlist.SyncSundaySetlistUseCase
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.core.domain.worship.ObserveWorshipAccessUseCase
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.usecase.GetChordChartsUseCase
import com.ipb.castelobranco.features.worshiphub.hub.presentation.state.WorshipHubUiState
import com.ipb.castelobranco.features.worshiphub.hub.presentation.state.buildHubBand
import com.ipb.castelobranco.features.worshiphub.hub.presentation.state.setlistRemoveFailedMessage
import com.ipb.castelobranco.features.worshiphub.hub.presentation.state.setlistRemovedMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZoneId
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class WorshipHubViewModel @Inject constructor(
    private val observeSundaySetlist: ObserveSundaySetlistUseCase,
    private val observeWorshipAccess: ObserveWorshipAccessUseCase,
    getChordChartsUseCase: GetChordChartsUseCase,
    private val syncSundaySetlist: SyncSundaySetlistUseCase,
    private val deleteSetlist: DeletePendingSetlistUseCase,
    private val sundaySetlistRepository: SundaySetlistRepository,
    private val dateProvider: DateProvider,
) : ViewModel() {

    private val isDeleting = MutableStateFlow(false)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val uiState: StateFlow<WorshipHubUiState> = combine(
        observeSundaySetlist(),
        observeWorshipAccess(),
        getChordChartsUseCase.observe(),
        isDeleting,
    ) { setlist, access, charts, deleting ->
        // Same pick as the chord charts' Sunday section: the first chart of each song.
        val chartIdBySongId = (charts as? SnapshotState.Data)?.value.orEmpty()
            .reversed()
            .associate { it.songId to it.id }
        WorshipHubUiState(
            band = buildHubBand(setlist, access, chartIdBySongId, dateProvider.today(), ZoneId.systemDefault()),
            isDeleting = deleting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initialState())

    /**
     * Setlist and flags are already in memory: the first frame shows them. Starting empty drew the hub without the
     * band and brought it in a frame later. The chord charts join on the first emission.
     */
    private fun initialState() = WorshipHubUiState(
        band = buildHubBand(
            observeSundaySetlist.current(),
            observeWorshipAccess.current(),
            emptyMap(),
            dateProvider.today(),
            ZoneId.systemDefault(),
        ),
    )

    /**
     * Removes the shown setlist on the server, like the admin panel does. Gone from the device at once; then the
     * current setlist is read again, which may bring a later Sunday's.
     */
    fun removeSetlist() {
        val date = uiState.value.band?.takeIf { it.canDelete }?.date ?: return
        if (isDeleting.value) return
        isDeleting.value = true
        viewModelScope.launch {
            deleteSetlist(date)
                .onSuccess {
                    sundaySetlistRepository.clear()
                    _messages.tryEmit(setlistRemovedMessage(date))
                    runCatching { syncSundaySetlist() }
                }
                .onFailure {
                    Timber.w(it, "Failed to remove the Sunday setlist")
                    _messages.tryEmit(setlistRemoveFailedMessage(date))
                }
            isDeleting.value = false
        }
    }

    init {
        // Opening the hub re-reads the setlist, silently, like pulling the chord chart list.
        viewModelScope.launch {
            runCatching { syncSundaySetlist() }.getOrElse { Result.failure(it) }
                .onFailure { Timber.w(it, "Failed to refresh the Sunday setlist") }
        }
    }
}
