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
    /** "Novo membro": `manage` on `members`. */
    val canAdd: Boolean = false,
    /** "Todos" first, then one per status the members carry, as the server names it. */
    val statusFilters: List<StatusFilterUi> = emptyList(),
)

/** A filter of the band: [status] `null` is "Todos". */
data class StatusFilterUi(
    val status: String?,
    val label: String,
    val isSelected: Boolean,
)

data class MemberCardUi(
    val id: Int,
    val name: String,
    val initials: String,
    val photoUrl: String?,
    val statusLabel: String,
    val isValid: Boolean,
    /** Bumps when the device copy of the photo changed, so it loads again. */
    val photoRevision: Int = 0,
)
