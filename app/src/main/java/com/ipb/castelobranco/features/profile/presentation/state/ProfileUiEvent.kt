package com.ipb.castelobranco.features.profile.presentation.state

sealed interface ProfileUiEvent {
    /** A short confirmation ("Nome atualizado"). */
    data class ShowMessage(val text: String) : ProfileUiEvent

    /** The deletion of an account without a password needs a fresh Google ID token from the picker. */
    data object RequestGoogleIdToken : ProfileUiEvent

    /** The account is gone; the session cleanup already runs app-wide. Leave the screen. */
    data object AccountDeleted : ProfileUiEvent
}
