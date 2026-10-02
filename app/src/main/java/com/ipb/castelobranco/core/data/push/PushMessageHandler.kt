package com.ipb.castelobranco.core.data.push

import com.ipb.castelobranco.core.data.app.AppForegroundState
import com.ipb.castelobranco.core.domain.auth.SessionPresenceProvider
import com.ipb.castelobranco.core.domain.push.PushMessage
import com.ipb.castelobranco.core.domain.worship.WorshipAccessRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a data message into what the user sees. Network work goes to a worker; the notification needs
 * only the date in the payload, so it shows at once, even offline.
 *
 * A session ended by a failed token refresh never unregistered its push token, so every message is
 * ignored while signed out.
 */
@Singleton
class PushMessageHandler @Inject constructor(
    private val sessionPresence: SessionPresenceProvider,
    private val worshipAccess: WorshipAccessRepository,
    private val foreground: AppForegroundState,
    private val notifier: SetlistNotifier,
    private val refreshScheduler: SetlistRefreshScheduler,
) {

    suspend fun handle(data: Map<String, String>) {
        val message = PushMessage.parse(data)
        if (message == null) {
            Timber.d("Ignoring push message of unknown shape")
            return
        }
        if (!sessionPresence.isLoggedIn()) return

        when (message) {
            is PushMessage.SetlistSaved -> {
                if (!worshipAccess.current().isWorshipMember) return
                refreshScheduler.schedule()
                if (!foreground.isForeground) notifier.showSetlistSaved(message.date)
            }
            // Access to the register screen is decided on tap, with the profile of that moment.
            is PushMessage.ConfirmPlays -> notifier.showConfirmPlays(message.date)
        }
    }
}
