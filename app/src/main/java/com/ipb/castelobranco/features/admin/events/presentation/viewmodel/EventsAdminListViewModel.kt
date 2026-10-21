package com.ipb.castelobranco.features.admin.events.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ipb.castelobranco.core.di.EventImageLoader
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.core.presentation.format.EventDateLabel
import com.ipb.castelobranco.features.admin.events.domain.model.EventRecord
import com.ipb.castelobranco.features.admin.events.domain.model.EventStatusFilter
import com.ipb.castelobranco.features.admin.events.domain.model.EventVisibility
import com.ipb.castelobranco.features.admin.events.domain.usecase.ListEventsUseCase
import com.ipb.castelobranco.features.admin.events.presentation.state.EventAdminItemUi
import com.ipb.castelobranco.features.admin.events.presentation.state.EventsAdminEvent
import com.ipb.castelobranco.features.admin.events.presentation.state.EventsAdminListUiState
import com.ipb.castelobranco.features.admin.events.presentation.util.FailureKind
import com.ipb.castelobranco.features.admin.events.presentation.util.authorLabel
import com.ipb.castelobranco.features.admin.events.presentation.util.toEventsAdminEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Read again on every visit and every filter change: nothing is kept between visits. */
@HiltViewModel
class EventsAdminListViewModel @Inject constructor(
    private val listEvents: ListEventsUseCase,
    private val dateProvider: DateProvider,
    observeAccess: ObserveAccessUseCase,
    @param:EventImageLoader val imageLoader: ImageLoader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsAdminListUiState())
    val uiState: StateFlow<EventsAdminListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EventsAdminEvent>()
    val events: SharedFlow<EventsAdminEvent> = _events.asSharedFlow()

    private var loading: Job? = null

    init {
        viewModelScope.launch {
            observeAccess().collect { access ->
                _uiState.update { it.copy(canCreate = access.allows(Scope.EVENTS, AccessLevel.MANAGE)) }
            }
        }
    }

    /** The screen calls it on every resume, which covers the first visit too. */
    fun load() {
        loading?.cancel()
        val filter = _uiState.value.filter
        _uiState.update { it.copy(isLoading = true, error = null) }
        loading = viewModelScope.launch {
            listEvents(filter)
                .onSuccess { records ->
                    val today = dateProvider.today()
                    _uiState.update { state ->
                        state.copy(isLoading = false, items = records.map { it.toItemUi(today) })
                    }
                }
                .onFailure { throwable ->
                    when (val event = throwable.toEventsAdminEvent(FailureKind.READ)) {
                        is EventsAdminEvent.ShowMessage ->
                            _uiState.update { it.copy(isLoading = false, error = event.message) }
                        else -> {
                            _uiState.update { it.copy(isLoading = false) }
                            _events.emit(event)
                        }
                    }
                }
        }
    }

    fun onFilterSelected(filter: EventStatusFilter) {
        if (filter == _uiState.value.filter) return
        _uiState.update { it.copy(filter = filter, items = emptyList()) }
        load()
    }
}

internal fun EventRecord.toItemUi(today: LocalDate) = EventAdminItemUi(
    id = id,
    title = title,
    dateLabel = EventDateLabel.dateLabel(startDate, endDate, startTime),
    imageUrl = imageUrl,
    isMembersOnly = visibility == EventVisibility.MEMBERS,
    isPast = isPastOn(today),
    authorLabel = authorLabel(),
)
