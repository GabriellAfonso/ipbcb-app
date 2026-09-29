package com.ipb.castelobranco.features.admin.panel.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.ObserveMembersUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.RefreshMembersUseCase
import com.ipb.castelobranco.features.admin.panel.domain.PanelCard
import com.ipb.castelobranco.features.admin.panel.domain.VisiblePanelCardsUseCase
import com.ipb.castelobranco.features.admin.panel.presentation.state.AdminPanelUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Follows the user's access, so a profile refresh after a refusal updates the cards in place.
 * The member count follows the in-memory roll, so changes made in the members area show here too.
 */
@HiltViewModel
class AdminPanelViewModel @Inject constructor(
    observeAccess: ObserveAccessUseCase,
    visibleCards: VisiblePanelCardsUseCase,
    observeMembers: ObserveMembersUseCase,
    private val refreshMembers: RefreshMembersUseCase,
) : ViewModel() {

    private val cards = observeAccess().map { access -> visibleCards(access) }

    val uiState: StateFlow<AdminPanelUiState> =
        combine(cards, observeMembers()) { cards, members ->
            AdminPanelUiState(
                cards = cards,
                isEmpty = cards.isEmpty(),
                memberCount = members?.size?.takeIf { PanelCard.MEMBERS in cards },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AdminPanelUiState())

    init {
        // A failure only hides the badge — the panel has no error state of its own.
        viewModelScope.launch {
            cards.first { PanelCard.MEMBERS in it }
            refreshMembers()
        }
    }
}
