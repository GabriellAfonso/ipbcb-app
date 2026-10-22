package com.ipb.castelobranco.core.domain.auth

import kotlinx.coroutines.flow.SharedFlow

/**
 * Cross-feature event bus for authentication lifecycle events.
 * Lives in core so that feature modules can depend on it without
 * creating direct cross-feature dependencies.
 */
interface AuthEventBus {

    sealed class Event {
        /** Emitted immediately after a successful login or registration. */
        data object LoginSuccess : Event()

        /** A scoped endpoint refused the user (403 `PERMISSION_DENIED`): the profile may be stale. */
        data object PermissionDenied : Event()

        /**
         * The server deleted the account: the session must end locally, like a logout, but the server already
         * signed out and forgot the push device.
         */
        data object AccountDeleted : Event()
    }

    val events: SharedFlow<Event>

    fun emit(event: Event)
}