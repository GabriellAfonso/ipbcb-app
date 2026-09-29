package com.ipb.castelobranco.features.admin.panel.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.features.admin.panel.domain.VisiblePanelCardsUseCase
import com.ipb.castelobranco.features.admin.panel.presentation.state.AdminPanelUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/** Follows the user's access, so a profile refresh after a refusal updates the cards in place. */
@HiltViewModel
class AdminPanelViewModel @Inject constructor(
    observeAccess: ObserveAccessUseCase,
    visibleCards: VisiblePanelCardsUseCase,
) : ViewModel() {

    val uiState: StateFlow<AdminPanelUiState> = observeAccess()
        .map { access ->
            val cards = visibleCards(access)
            AdminPanelUiState(cards = cards, isEmpty = cards.isEmpty())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AdminPanelUiState())
}
