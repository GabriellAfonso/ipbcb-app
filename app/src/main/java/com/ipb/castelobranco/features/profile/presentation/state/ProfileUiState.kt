package com.ipb.castelobranco.features.profile.presentation.state

import com.ipb.castelobranco.core.domain.access.MemberAccess
import com.ipb.castelobranco.core.presentation.components.access.AccessRequestDialogState
import com.ipb.castelobranco.features.profile.domain.model.RecoveryEmailStatus
import com.ipb.castelobranco.features.profile.domain.model.isDeletionConfirmationTyped

const val RECOVERY_CODE_LENGTH = 6

data class ProfileUiState(
    val isUploading: Boolean = false,
    val isRemovingPhoto: Boolean = false,
    val photoUrl: String? = null,
    /** No profile on the device and reading it failed: the card with "Tentar novamente". */
    val loadFailed: Boolean = false,
    val localPhotoPath: String? = null,
    val localPhotoVersion: Int = 0,
    val userName: String? = null,
    /** First word of the profile name, for the home greeting; `null` while unknown or blank. */
    val firstName: String? = null,
    /** The member record linked to the user (`MeProfile.memberId`); `null` while unknown or not linked. */
    val memberId: Long? = null,
    /** "março de 2025"; `null` while unknown (the line is hidden). */
    val joinedSince: String? = null,
    /** `null` while unknown: the top bar menu ("Trocar senha") hidden and "Excluir conta" disabled. */
    val hasPassword: Boolean? = null,
    val showRemovePhotoConfirm: Boolean = false,
    val nameDialog: NameDialogState? = null,
    val passwordDialog: PasswordDialogState? = null,
    val deleteDialog: DeleteDialogState? = null,
    /** Drives the access card and which request buttons exist. */
    val access: MemberAccess = MemberAccess.NONE,
    /** "Meus dados na igreja": memory only, never saved. */
    val ownMember: OwnMemberSection = OwnMemberSection.Hidden,
    val requestDialog: AccessRequestDialogState? = null,
    val recoveryEmail: RecoveryEmailStatus = RecoveryEmailStatus.Unknown,
    /** `null` until read from disk: the notice must not flash for someone who already answered it. */
    val recoveryNoticeAnswered: Boolean? = null,
    val recoveryEmailForm: RecoveryEmailFormState? = null,
    val recoveryCodeDialog: RecoveryCodeDialogState? = null,
) {
    /** The one-time home card: a known password account with a known absence of an e-mail. */
    val showRecoveryNotice: Boolean
        get() = hasPassword == true && recoveryEmail == RecoveryEmailStatus.None && recoveryNoticeAnswered == false

    /** Whether [showRecoveryNotice] is already decided for this account: a later notice waits for this. */
    val isRecoveryNoticeSettled: Boolean
        get() = hasPassword == false ||
            (hasPassword == true && recoveryEmail != RecoveryEmailStatus.Unknown && recoveryNoticeAnswered != null)

    val isPhotoBusy: Boolean get() = isUploading || isRemovingPhoto
    val canDeleteAccount: Boolean get() = hasPassword != null
}

/** The roll section's states are exclusive: hidden (not linked), loading, failed or shown. */
sealed interface OwnMemberSection {
    data object Hidden : OwnMemberSection
    data object Loading : OwnMemberSection
    data class Error(val message: String) : OwnMemberSection

    /** [lines] are label → value, unknown values already left out. */
    data class Loaded(val lines: List<Pair<String, String>>) : OwnMemberSection
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

data class RecoveryEmailFormState(
    val isSaving: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val error: String? = null,
)

data class RecoveryCodeDialogState(
    val isSending: Boolean = false,
    val isConfirming: Boolean = false,
    val codeError: String? = null,
    val error: String? = null,
    val resendSecondsLeft: Int = 0,
) {
    val canResend: Boolean get() = resendSecondsLeft == 0 && !isSending && !isConfirming
}

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

fun canSaveRecoveryEmail(email: String, currentPassword: String): Boolean =
    email.isNotBlank() && currentPassword.isNotBlank()

fun canDeleteAccount(requiresPassword: Boolean, password: String, confirmation: String): Boolean =
    isDeletionConfirmationTyped(confirmation) && (!requiresPassword || password.isNotBlank())
