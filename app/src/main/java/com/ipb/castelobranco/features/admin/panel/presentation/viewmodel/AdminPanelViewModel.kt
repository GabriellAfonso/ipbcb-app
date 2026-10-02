package com.ipb.castelobranco.features.admin.panel.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.ObserveMembersUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.RefreshMembersUseCase
import com.ipb.castelobranco.features.admin.panel.domain.PanelCard
import com.ipb.castelobranco.features.admin.panel.domain.VisiblePanelCardsUseCase
import com.ipb.castelobranco.features.admin.panel.presentation.state.AdminPanelUiState
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.admin.panel.presentation.state.PendingConfirmationsUi
import com.ipb.castelobranco.features.admin.register.domain.usecase.GetPendingConfirmationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val getPendingConfirmations: GetPendingConfirmationsUseCase,
) : ViewModel() {

    private val access = observeAccess()
    private val cards = access.map { access -> visibleCards(access) }
    private val canConfirmPlays = access.map { it.allows(Scope.SONGS, AccessLevel.MANAGE) }

    private val pending = MutableStateFlow<PendingConfirmationsUi>(PendingConfirmationsUi.Hidden)

    val uiState: StateFlow<AdminPanelUiState> =
        combine(cards, observeMembers(), canConfirmPlays, pending) { cards, members, canConfirm, pending ->
            AdminPanelUiState(
                cards = cards,
                isEmpty = cards.isEmpty(),
                memberCount = members?.size?.takeIf { PanelCard.MEMBERS in cards },
                pending = if (canConfirm) pending else PendingConfirmationsUi.Hidden,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AdminPanelUiState())

    /**
     * Re-read every time the panel is shown, so a Sunday registered meanwhile leaves the card. Only
     * the first read shows loading; later ones keep the dates on screen until the answer.
     */
    fun refreshPending() {
        viewModelScope.launch {
            if (!canConfirmPlays.first()) {
                pending.value = PendingConfirmationsUi.Hidden
                return@launch
            }
            if (pending.value !is PendingConfirmationsUi.Dates) pending.value = PendingConfirmationsUi.Loading
            pending.value = getPendingConfirmations().fold(
                onSuccess = { dates ->
                    if (dates.isEmpty()) PendingConfirmationsUi.Hidden else PendingConfirmationsUi.Dates(dates)
                },
                onFailure = { error ->
                    val appError = error.toAppError()
                    if (appError is AppError.Auth) {
                        PendingConfirmationsUi.Hidden
                    } else {
                        PendingConfirmationsUi.Failed(appError.toUserMessage())
                    }
                },
            )
        }
    }

    init {
        // A failure only hides the badge — the panel has no error state of its own.
        viewModelScope.launch {
            cards.first { PanelCard.MEMBERS in it }
            refreshMembers()
        }
    }
}
