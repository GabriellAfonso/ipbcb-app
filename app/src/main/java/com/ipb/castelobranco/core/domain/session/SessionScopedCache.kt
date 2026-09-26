package com.ipb.castelobranco.core.domain.session

/**
 * Data that lives only in memory and only for the current session — for example the member
 * records a leader opened. Anything registered here is wiped when the session ends, whether by an
 * explicit logout or by the token refresh failing.
 *
 * Features contribute implementations with `@IntoSet`; `core/` clears them without importing the
 * feature, the same way `Preloadable`/`Refreshable` drive the startup.
 */
fun interface SessionScopedCache {
    suspend fun clear()
}
