package com.ipb.castelobranco.core.data.security

import com.ipb.castelobranco.core.di.SessionCipher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What happens when the encrypted session cannot be opened (key lost, file corrupt): the key is
 * destroyed so the next sign-in gets a fresh one, and [pendingWipe] asks `CoreViewModel` to clear
 * the session-scoped caches. The session itself is already empty by then — the app simply starts
 * signed out, with no message.
 *
 * A flag rather than an event: the store can be opened before anyone collects, and injecting the
 * caches here would close a DI cycle through the authenticated client.
 */
@Singleton
class SessionRecovery @Inject constructor(
    @param:SessionCipher private val cipher: AeadCipher,
) {
    private val _pendingWipe = MutableStateFlow(false)
    val pendingWipe: StateFlow<Boolean> = _pendingWipe.asStateFlow()

    fun onSessionUnreadable() {
        Timber.w("Session unreadable; signed out")
        cipher.destroyKey()
        _pendingWipe.value = true
    }

    fun acknowledgeWipe() {
        _pendingWipe.value = false
    }
}
