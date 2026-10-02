package com.ipb.castelobranco.core.data.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ipb.castelobranco.core.domain.push.PushRegistrationScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Entry point for Firebase Cloud Messaging. The second `@AndroidEntryPoint` besides `CoreActivity`:
 * Hilt needs it to inject a service. It only hands over to injected classes.
 */
@AndroidEntryPoint
class IpbMessagingService : FirebaseMessagingService() {

    @Inject lateinit var registrationScheduler: PushRegistrationScheduler
    @Inject lateinit var messageHandler: PushMessageHandler

    override fun onNewToken(token: String) {
        registrationScheduler.schedule()
    }

    /**
     * Runs on a Firebase background thread with a few seconds of budget. Blocking here is fine: the
     * handler only reads local state and enqueues work.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        runBlocking { messageHandler.handle(message.data) }
    }
}
