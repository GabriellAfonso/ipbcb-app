package com.ipb.castelobranco.features.events.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ipb.castelobranco.core.di.EventImageLoader
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.presentation.format.EventDateLabel
import com.ipb.castelobranco.features.events.domain.model.Event
import com.ipb.castelobranco.features.events.domain.usecase.ObserveActiveEventsUseCase
import com.ipb.castelobranco.features.events.domain.usecase.RefreshEventsUseCase
import com.ipb.castelobranco.features.events.presentation.state.EventListItemUi
import com.ipb.castelobranco.features.events.presentation.state.EventsListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** Every active event, without the carousel's limit (`specs/events/spec.md` §3.1). */
@HiltViewModel
class EventsListViewModel @Inject constructor(
    observeActiveEvents: ObserveActiveEventsUseCase,
    private val refreshEvents: RefreshEventsUseCase,
    @param:EventImageLoader val imageLoader: ImageLoader,
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val refreshFailed = MutableStateFlow(false)

    val uiState: StateFlow<EventsListUiState> = combine(
        observeActiveEvents(),
        isRefreshing,
        refreshFailed,
    ) { events, refreshing, failed ->
        val nothingSaved = events.isNullOrEmpty()
        EventsListUiState(
            // An empty list is only "no events" once the fetch has answered.
            isLoading = events == null || (nothingSaved && refreshing),
            isRefreshing = refreshing,
            events = events.orEmpty().map { it.toListItem() },
            loadFailed = nothingSaved && failed && !refreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EventsListUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            val result = runCatching { refreshEvents() }
                .onFailure { Timber.w(it, "Events refresh failed") }
                .getOrNull()
            refreshFailed.value = result == null || result is RefreshResult.Error
            isRefreshing.value = false
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

internal fun Event.toListItem() = EventListItemUi(
    id = id,
    title = title,
    dateLabel = EventDateLabel.dateLabel(startDate, endDate, startTime),
    location = location,
    imageUrl = imageUrl,
    isMembersOnly = isMembersOnly,
)
