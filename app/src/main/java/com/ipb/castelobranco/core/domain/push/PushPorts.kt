package com.ipb.castelobranco.core.domain.push

/** This installation's push token, as the push provider gives it. */
fun interface PushTokenSource {
    /** `null` when the provider cannot give one (no Google Play services, provider failure). */
    suspend fun currentToken(): String?
}

/** Runs the token registration in the background, retrying until the server has it. */
interface PushRegistrationScheduler {
    fun schedule()
    fun cancel()
}

/** The server's list of this account's devices (`api/me/devices/`). */
interface DevicesRepository {
    suspend fun register(token: String): Result<Unit>
    suspend fun unregister(token: String): Result<Unit>
}

/** The last token the server accepted for this device, so logout knows what to forget. */
interface RegisteredTokenStore {
    suspend fun get(): String?
    suspend fun set(token: String)
    suspend fun clear()
}
