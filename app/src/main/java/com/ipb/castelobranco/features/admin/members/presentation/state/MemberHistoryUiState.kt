package com.ipb.castelobranco.features.admin.members.presentation.state

data class MemberHistoryUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val lines: List<HistoryLineUi> = emptyList(),
)

data class HistoryLineUi(
    val editor: String,
    val text: String,
    val time: String,
)
