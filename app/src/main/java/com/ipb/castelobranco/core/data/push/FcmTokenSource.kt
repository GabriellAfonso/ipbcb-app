package com.ipb.castelobranco.core.data.push

import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.domain.push.PushTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Firebase Cloud Messaging token; `Tasks.await` blocks, so it runs on the IO dispatcher. */
@Singleton
class FcmTokenSource @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PushTokenSource {

    override suspend fun currentToken(): String? = withContext(ioDispatcher) {
        try {
            Tasks.await(FirebaseMessaging.getInstance().token, TOKEN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                ?.takeIf { it.isNotBlank() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Push token unavailable")
            null
        }
    }

    private companion object {
        const val TOKEN_TIMEOUT_SECONDS = 30L
    }
}
