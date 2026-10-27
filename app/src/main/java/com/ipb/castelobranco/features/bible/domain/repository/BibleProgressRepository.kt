package com.ipb.castelobranco.features.bible.domain.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.bible.domain.model.BibleProgress
import com.ipb.castelobranco.features.bible.domain.model.ProgressTarget
import kotlinx.coroutines.flow.Flow

/**
 * The signed-in account's reading and watched progress: the server state (kept on the device) with the changes
 * still waiting to be sent applied on top, so a mark shows at once, also offline.
 */
interface BibleProgressRepository {
    val progress: Flow<BibleProgress>

    /** Fetches the account's state. Does nothing when signed out; failures keep the cached state. */
    suspend fun refresh()

    /** Queues [targets] as done or not, stamped with the current time, and schedules the send. */
    suspend fun mark(targets: List<ProgressTarget>, done: Boolean)

    suspend fun queuedCount(): Int

    /** Sends one chunk of the queue. */
    suspend fun syncOnce(): ProgressSyncOutcome

    /** End of session: tries one send if possible, then forgets the queue and the cached state. */
    suspend fun flushAndClear()
}

sealed interface ProgressSyncOutcome {
    /** The chunk reached the server and left the queue (accepted or rejected item by item). */
    data object Delivered : ProgressSyncOutcome

    /** The server refused the whole chunk: it left the queue, nothing to retry. */
    data object Discarded : ProgressSyncOutcome

    /** Offline, server error or signed out: everything stays queued. */
    data class Deferred(val error: AppError?) : ProgressSyncOutcome
}
