package com.ipb.castelobranco.features.admin.panel.presentation.state

import com.ipb.castelobranco.features.admin.panel.domain.PanelCard
import java.time.LocalDate

/**
 * @param isEmpty the user reached the panel but no card is allowed — show a message, not a blank grid.
 * @param memberCount size of the roll for the "Membros" badge; null while unknown, so no badge.
 */
data class AdminPanelUiState(
    val cards: List<PanelCard> = emptyList(),
    val isEmpty: Boolean = false,
    val memberCount: Int? = null,
    val pending: PendingConfirmationsUi = PendingConfirmationsUi.Hidden,
)

/** The "Confirmar músicas de domingo" card: Sundays whose played songs are still to be registered. */
sealed interface PendingConfirmationsUi {
    /**
     * No `manage` on `songs`, nothing pending, access refused, or the first read still running — the list is
     * usually empty, so a loading card would flash in and out on every visit.
     */
    data object Hidden : PendingConfirmationsUi
    data class Failed(val message: String) : PendingConfirmationsUi

    /**
     * @param dates never empty, newest first.
     * @param canDelete the profile says the user can save a setlist — the server refuses the delete otherwise.
     */
    data class Dates(val dates: List<LocalDate>, val canDelete: Boolean = false) : PendingConfirmationsUi
}

/** One-shot effects of the panel. */
sealed interface AdminPanelEvent {
    data class ShowMessage(val message: String) : AdminPanelEvent
}
