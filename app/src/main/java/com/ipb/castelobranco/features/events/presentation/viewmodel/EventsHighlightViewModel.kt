package com.ipb.castelobranco.features.events.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ipb.castelobranco.core.di.EventImageLoader
import com.ipb.castelobranco.core.presentation.components.HighlightEventUi
import com.ipb.castelobranco.core.presentation.format.EventDateLabel
import com.ipb.castelobranco.features.events.domain.model.Event
import com.ipb.castelobranco.features.events.domain.usecase.ObserveHighlightEventsUseCase
import com.ipb.castelobranco.features.events.domain.usecase.RefreshEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * The carousel's event pages. The list is fetched again whenever the home comes back and whenever the
 * user logs in or out (`specs/events/spec.md` §2.1): logout stays on the home, so only the login change
 * tells it that the members-only events must go and the public list must come.
 */
@HiltViewModel
class EventsHighlightViewModel @Inject constructor(
    observeHighlightEvents: ObserveHighlightEventsUseCase,
    private val refreshEvents: RefreshEventsUseCase,
    @param:EventImageLoader val imageLoader: ImageLoader,
) : ViewModel() {

    val pages: StateFlow<List<HighlightEventUi>> = observeHighlightEvents()
        .map { events -> events.map { it.toHighlightUi() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var lastLoginState: Boolean? = null

    fun refresh() {
        viewModelScope.launch {
            runCatching { refreshEvents() }
                .onFailure { Timber.w(it, "Events refresh failed") }
        }
    }

    /** The first value only records where the session stands; the boot already fetches the list. */
    fun onLoginStateChanged(isLoggedIn: Boolean) {
        val previous = lastLoginState
        lastLoginState = isLoggedIn
        if (previous != null && previous != isLoggedIn) refresh()
    }
}

internal fun Event.toHighlightUi() = HighlightEventUi(
    id = id,
    title = title,
    dateLabel = EventDateLabel.dateLabel(startDate, endDate, startTime),
    imageUrl = imageUrl,
)
