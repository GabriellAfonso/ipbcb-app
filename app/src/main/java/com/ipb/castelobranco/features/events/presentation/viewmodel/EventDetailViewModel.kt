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
import java.time.temporal.ChronoUnit
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

internal fun Event.toDetailUi(): EventDetailUi {
    val fullDate = EventDateLabel.fullDateLabel(startDate, endDate)
    val time = startTime?.let(EventDateLabel::timeLabel)
    val days = endDate?.let { ChronoUnit.DAYS.between(startDate, it).toInt() + 1 } ?: 1
    return EventDetailUi(
        title = title,
        dateLabel = fullDate,
        timeLabel = time,
        location = location,
        description = description,
        imageUrl = imageUrl,
        isMembersOnly = isMembersOnly,
        shortDateLabel = EventDateLabel.dateLabel(startDate, endDate, time = null),
        durationLabel = "$days dias".takeIf { days > 1 },
        shareText = listOfNotNull(
            title,
            listOfNotNull(fullDate, time).joinToString(" · "),
            location,
            description?.let { "\n$it" },
        ).joinToString("\n"),
    )
}
