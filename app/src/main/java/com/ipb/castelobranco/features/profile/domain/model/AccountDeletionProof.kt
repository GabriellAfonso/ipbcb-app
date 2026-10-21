package com.ipb.castelobranco.features.profile.domain.model

/** How the person proves who they are before the account is deleted; it depends on whether it has a password. */
sealed interface AccountDeletionProof {
    data class Password(val password: String) : AccountDeletionProof

    /** A fresh Google ID token of the same account, for an account without a password. */
    data class Google(val idToken: String) : AccountDeletionProof
}

/** What the person types to confirm a deletion; checked only in the app, never sent. */
const val DELETION_CONFIRMATION_WORD = "EXCLUIR"

fun isDeletionConfirmationTyped(text: String): Boolean =
    text.trim().equals(DELETION_CONFIRMATION_WORD, ignoreCase = true)
