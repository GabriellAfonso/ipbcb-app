package com.ipb.castelobranco.features.events.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ipb.castelobranco.core.di.EventImageLoader
import com.ipb.castelobranco.core.presentation.format.EventDateLabel
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.features.events.domain.model.Event
import com.ipb.castelobranco.features.events.domain.usecase.GetEventUseCase
import com.ipb.castelobranco.features.events.presentation.state.EventDetailUi
import com.ipb.castelobranco.features.events.presentation.state.EventDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Reads the event from the saved list: the detail opens offline, exactly as the carousel showed it. */
@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getEvent: GetEventUseCase,
    @param:EventImageLoader val imageLoader: ImageLoader,
) : ViewModel() {

    private val eventId: Int? = savedStateHandle.get<Any>(AppRoutes.ARG_EVENT_ID)?.toString()?.toIntOrNull()

    val uiState: StateFlow<EventDetailUiState> = (eventId?.let(getEvent::invoke) ?: flowOf(null))
        .map { event ->
            EventDetailUiState(isLoading = false, event = event?.toDetailUi(), isGone = event == null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EventDetailUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

internal fun Event.toDetailUi() = EventDetailUi(
    title = title,
    dateLabel = EventDateLabel.fullDateLabel(startDate, endDate),
    timeLabel = startTime?.let(EventDateLabel::timeLabel),
    location = location,
    description = description,
    imageUrl = imageUrl,
    isMembersOnly = isMembersOnly,
)
