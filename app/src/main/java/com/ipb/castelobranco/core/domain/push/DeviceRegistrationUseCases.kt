package com.ipb.castelobranco.core.domain.push

import com.ipb.castelobranco.core.domain.auth.SessionPresenceProvider
import com.ipb.castelobranco.core.domain.error.AppError
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

/** What a registration attempt asks of its caller. */
enum class RegistrationOutcome {
    /** Registered, or nothing to do (no session, no token). */
    DONE,

    /** Network or server trouble: try again later. */
    RETRY,

    /** Refused by the server: trying again now would be refused too. */
    FAILED,
}

/**
 * Sends this device's push token to the server for the signed-in account. Idempotent on the server, so
 * it runs on every start, after every login and on every token rotation.
 */
class RegisterDeviceUseCase @Inject constructor(
    private val sessionPresence: SessionPresenceProvider,
    private val tokenSource: PushTokenSource,
    private val devices: DevicesRepository,
    private val store: RegisteredTokenStore,
) {
    suspend operator fun invoke(): RegistrationOutcome {
        if (!sessionPresence.isLoggedIn()) return RegistrationOutcome.DONE
        val token = tokenSource.currentToken() ?: return RegistrationOutcome.DONE
        return devices.register(token).fold(
            onSuccess = {
                store.set(token)
                RegistrationOutcome.DONE
            },
            onFailure = { error ->
                Timber.w(error, "Device registration failed")
                when (error) {
                    is AppError.Network -> RegistrationOutcome.RETRY
                    is AppError.Server -> if (error.code >= HTTP_SERVER_ERROR) {
                        RegistrationOutcome.RETRY
                    } else {
                        RegistrationOutcome.FAILED
                    }
                    else -> RegistrationOutcome.FAILED
                }
            },
        )
    }

    private companion object {
        const val HTTP_SERVER_ERROR = 500
    }
}

/**
 * Asks the server to forget this device. Runs at logout, before the session is cleared, so the call is
 * still authenticated. Nothing here may stop the logout: failures and slowness are swallowed.
 */
class UnregisterDeviceUseCase @Inject constructor(
    private val scheduler: PushRegistrationScheduler,
    private val devices: DevicesRepository,
    private val store: RegisteredTokenStore,
) {
    suspend operator fun invoke() {
        runCatching { scheduler.cancel() }
        val token = runCatching { store.get() }.getOrNull()
        if (token != null) {
            val result = withTimeoutOrNull(UNREGISTER_TIMEOUT_MS) {
                runCatching { devices.unregister(token) }.getOrElse { Result.failure(it) }
            }
            when {
                result == null -> Timber.w("Device unregister timed out")
                result.isFailure -> Timber.w(result.exceptionOrNull(), "Device unregister failed")
            }
        }
        runCatching { store.clear() }
    }

    companion object {
        const val UNREGISTER_TIMEOUT_MS = 5_000L
    }
}
