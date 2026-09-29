package com.ipb.castelobranco.features.admin.panel.presentation.state

import com.ipb.castelobranco.features.admin.panel.domain.PanelCard

/**
 * @param isEmpty the user reached the panel but no card is allowed — show a message, not a blank grid.
 * @param memberCount size of the roll for the "Membros" badge; null while unknown, so no badge.
 */
data class AdminPanelUiState(
    val cards: List<PanelCard> = emptyList(),
    val isEmpty: Boolean = false,
    val memberCount: Int? = null,
)
