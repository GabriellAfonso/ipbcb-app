package com.ipb.castelobranco.features.admin.members.presentation.state

/**
 * @param totalCount every member held, before the search — tells "the roll is empty" apart from
 *   "nobody matches the search".
 */
data class MembersListUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val query: String = "",
    val members: List<MemberCardUi> = emptyList(),
    val totalCount: Int = 0,
)

data class MemberCardUi(
    val id: Int,
    val name: String,
    val initials: String,
    val photoUrl: String?,
    val statusLabel: String,
    val isValid: Boolean,
)
