package com.ipb.castelobranco.features.admin.members.presentation.state

import com.ipb.castelobranco.features.admin.members.domain.model.MemberDraft
import com.ipb.castelobranco.features.admin.members.domain.model.MemberField
import com.ipb.castelobranco.features.admin.members.domain.model.MemberOptions

/**
 * @param fieldErrors local checks and server `field_errors` land here, on the same field.
 * @param generalError a server refusal that names no field.
 */
data class MemberFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val draft: MemberDraft = MemberDraft(),
    val options: MemberOptions? = null,
    val fieldErrors: Map<MemberField, String> = emptyMap(),
    val generalError: String? = null,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
)
