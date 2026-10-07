package com.ipb.castelobranco.features.admin.panel.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.ObserveMembersUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.RefreshMembersUseCase
import com.ipb.castelobranco.features.admin.panel.domain.PanelCard
import com.ipb.castelobranco.features.admin.panel.domain.VisiblePanelCardsUseCase
import com.ipb.castelobranco.features.admin.panel.presentation.state.AdminPanelEvent
import com.ipb.castelobranco.features.admin.panel.presentation.state.AdminPanelUiState
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.domain.worship.ObserveWorshipAccessUseCase
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.admin.panel.presentation.state.PendingConfirmationsUi
import com.ipb.castelobranco.features.admin.register.domain.usecase.DeletePendingSetlistUseCase
import com.ipb.castelobranco.features.admin.register.domain.usecase.GetPendingConfirmationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L
private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

/**
 * Follows the user's access, so a profile refresh after a refusal updates the cards in place.
 * The member count follows the in-memory roll, so changes made in the members area show here too.
 */
@HiltViewModel
class AdminPanelViewModel @Inject constructor(
    observeAccess: ObserveAccessUseCase,
    visibleCards: VisiblePanelCardsUseCase,
    observeMembers: ObserveMembersUseCase,
    observeWorshipAccess: ObserveWorshipAccessUseCase,
    private val refreshMembers: RefreshMembersUseCase,
    private val getPendingConfirmations: GetPendingConfirmationsUseCase,
    private val deletePendingSetlist: DeletePendingSetlistUseCase,
) : ViewModel() {

    private val access = observeAccess()
    private val cards = access.map { access -> visibleCards(access) }
    private val canConfirmPlays = access.map { it.allows(Scope.SONGS, AccessLevel.MANAGE) }

    private val canDeleteSetlist = observeWorshipAccess().map { it.canSaveSetlist }

    private val pending = MutableStateFlow<PendingConfirmationsUi>(PendingConfirmationsUi.Hidden)

    private val _events = MutableSharedFlow<AdminPanelEvent>()
    val events: SharedFlow<AdminPanelEvent> = _events.asSharedFlow()

    val uiState: StateFlow<AdminPanelUiState> =
        combine(
            cards,
            observeMembers(),
            canConfirmPlays,
            canDeleteSetlist,
            pending,
        ) { cards, members, canConfirm, canDelete, pending ->
            AdminPanelUiState(
                cards = cards,
                isEmpty = cards.isEmpty(),
                memberCount = members?.size?.takeIf { PanelCard.MEMBERS in cards },
                pending = when {
                    !canConfirm -> PendingConfirmationsUi.Hidden
                    pending is PendingConfirmationsUi.Dates -> pending.copy(canDelete = canDelete)
                    else -> pending
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AdminPanelUiState())

    /**
     * Re-read every time the panel is shown, so a Sunday registered meanwhile leaves the card. Whatever is
     * on screen stays until the answer, so the card never appears just to vanish.
     */
    fun refreshPending() {
        viewModelScope.launch {
            if (!canConfirmPlays.first()) {
                pending.value = PendingConfirmationsUi.Hidden
                return@launch
            }
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

    /** The date leaves the card once the server no longer has its setlist; a failure keeps it. */
    fun deleteSetlist(date: LocalDate) {
        viewModelScope.launch {
            deletePendingSetlist(date)
                .onSuccess { pending.update { it.without(date) } }
                .onFailure { _events.emit(AdminPanelEvent.ShowMessage(deleteFailedMessage(date))) }
        }
    }

    private fun deleteFailedMessage(date: LocalDate) =
        "Não foi possível remover o repertório de ${date.format(DAY_MONTH)}."

    private fun PendingConfirmationsUi.without(date: LocalDate): PendingConfirmationsUi {
        if (this !is PendingConfirmationsUi.Dates) return this
        val remaining = dates - date
        return if (remaining.isEmpty()) PendingConfirmationsUi.Hidden else copy(dates = remaining)
    }

    init {
        // A failure only hides the badge — the panel has no error state of its own.
        viewModelScope.launch {
            cards.first { PanelCard.MEMBERS in it }
            refreshMembers()
        }
    }
}
