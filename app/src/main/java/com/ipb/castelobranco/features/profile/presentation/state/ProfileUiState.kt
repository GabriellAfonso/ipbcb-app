package com.ipb.castelobranco.features.profile.presentation.state

import com.ipb.castelobranco.features.profile.domain.model.isDeletionConfirmationTyped

data class ProfileUiState(
    val isUploading: Boolean = false,
    val isRemovingPhoto: Boolean = false,
    val photoUrl: String? = null,
    val error: String? = null,
    val localPhotoPath: String? = null,
    val localPhotoVersion: Int = 0,
    val userName: String? = null,
    /** "março de 2025"; `null` while unknown (the line is hidden). */
    val joinedSince: String? = null,
    /** `null` while unknown: the top bar menu ("Trocar senha") hidden and "Excluir conta" disabled. */
    val hasPassword: Boolean? = null,
    val showRemovePhotoConfirm: Boolean = false,
    val nameDialog: NameDialogState? = null,
    val passwordDialog: PasswordDialogState? = null,
    val deleteDialog: DeleteDialogState? = null,
) {
    val isPhotoBusy: Boolean get() = isUploading || isRemovingPhoto
    val canDeleteAccount: Boolean get() = hasPassword != null
}

data class NameDialogState(
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val error: String? = null,
)

data class PasswordDialogState(
    val isSaving: Boolean = false,
    val currentError: String? = null,
    val newError: String? = null,
    val confirmError: String? = null,
    val error: String? = null,
)

data class DeleteDialogState(
    /** The account has a password: it confirms with it; otherwise with a fresh Google sign-in. */
    val requiresPassword: Boolean,
    val isDeleting: Boolean = false,
    val passwordError: String? = null,
    val error: String? = null,
)

/** Saving is offered only for a trimmed, non-blank name that differs from the current one. */
fun canSaveName(input: String, current: String?): Boolean {
    val trimmed = input.trim()
    return trimmed.isNotEmpty() && trimmed != current?.trim()
}

fun canChangePassword(current: String, new: String, confirm: String): Boolean =
    current.isNotBlank() && new.isNotBlank() && confirm.isNotBlank()

fun canDeleteAccount(requiresPassword: Boolean, password: String, confirmation: String): Boolean =
    isDeletionConfirmationTyped(confirmation) && (!requiresPassword || password.isNotBlank())
