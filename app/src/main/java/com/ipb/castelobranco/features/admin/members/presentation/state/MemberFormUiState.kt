package com.ipb.castelobranco.features.admin.members.presentation.state

import com.ipb.castelobranco.features.admin.members.domain.model.MemberDraft
import com.ipb.castelobranco.features.admin.members.domain.model.MemberField
import com.ipb.castelobranco.features.admin.members.domain.model.MemberOptions

/**
 * @param fieldErrors local checks and server `field_errors` land here, on the same field.
 * @param generalError a server refusal that names no field.
 * @param initials the saved name's initials; on a new member, the typed name's.
 * @param photoUrl the member's current photo; edit only.
 * @param pickedPhoto a photo picked in the form, uploaded only when the leader saves.
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
    val initials: String = "",
    val photoUrl: String? = null,
    val pickedPhoto: ByteArray? = null,
    /** Bumps when the device copy of [photoUrl] changed, so it loads again. */
    val photoRevision: Int = 0,
) {
    /** The band's title: the typed name, or "Novo membro" while it is empty. */
    val bandTitle: String get() = draft.name.trim().ifEmpty { NEW_MEMBER_TITLE }

    /** "Ativo · Diaconisa": whatever of status and role is chosen; null when neither is. */
    val bandSubtitle: String?
        get() {
            val status = options?.statuses?.firstOrNull { it.id == draft.statusId }?.name
            val role = options?.roles?.firstOrNull { it.id == draft.roleId }?.name
            return listOfNotNull(status, role).joinToString(" · ").ifEmpty { null }
        }
}

private const val NEW_MEMBER_TITLE = "Novo membro"
