package com.ipb.castelobranco.features.bible.data.repository

import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.domain.auth.SessionPresenceProvider
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.util.WallClock
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.bible.data.api.BibleProgressApi
import com.ipb.castelobranco.features.bible.data.dto.BibleProgressDto
import com.ipb.castelobranco.features.bible.data.dto.ProgressSyncRequestDto
import com.ipb.castelobranco.features.bible.data.dto.toDomain
import com.ipb.castelobranco.features.bible.data.dto.toWireDto
import com.ipb.castelobranco.features.bible.data.local.BibleProgressQueueStore
import com.ipb.castelobranco.features.bible.data.local.toQueued
import com.ipb.castelobranco.features.bible.domain.model.BibleProgress
import com.ipb.castelobranco.features.bible.domain.model.ProgressChange
import com.ipb.castelobranco.features.bible.domain.model.ProgressTarget
import com.ipb.castelobranco.features.bible.domain.repository.BibleProgressRepository
import com.ipb.castelobranco.features.bible.domain.repository.ProgressSyncOutcome
import com.ipb.castelobranco.features.bible.domain.sync.BibleProgressSyncScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BibleProgressRepositoryImpl @Inject constructor(
    private val api: BibleProgressApi,
    private val cache: SnapshotCache<BibleProgressDto>,
    private val queue: BibleProgressQueueStore,
    private val scheduler: BibleProgressSyncScheduler,
    private val session: SessionPresenceProvider,
    private val clock: WallClock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BibleProgressRepository {

    /** Server state as last fetched; null until the cache is read. */
    private val server = MutableStateFlow<BibleProgress?>(null)

    override val progress: Flow<BibleProgress> =
        combine(server, queue.pending) { state, pending ->
            (state ?: BibleProgress.EMPTY).applying(pending.orEmpty())
        }.onStart { loadLocal() }

    private suspend fun loadLocal() {
        queue.load()
        if (server.value == null) {
            server.value = withContext(ioDispatcher) { cache.load() }?.toDomain() ?: BibleProgress.EMPTY
        }
    }

    override suspend fun refresh() {
        if (!session.isLoggedIn()) return
        try {
            val response = api.getProgress()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                withContext(ioDispatcher) { cache.save(body, null) }
                server.value = body.toDomain()
            } else {
                Timber.d("Bible progress fetch failed: HTTP %d", response.code())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Timber.d("Bible progress fetch failed: %s", t.javaClass.simpleName)
        }
    }

    override suspend fun mark(targets: List<ProgressTarget>, done: Boolean) {
        if (targets.isEmpty()) return
        val changedAt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(clock.nowMillis()), ZoneId.systemDefault())
            .toString()
        queue.upsert(targets.map { ProgressChange(it, done, changedAt) })
        scheduler.scheduleSync()
    }

    override suspend fun queuedCount(): Int = queue.count()

    override suspend fun syncOnce(): ProgressSyncOutcome {
        if (!session.isLoggedIn()) return ProgressSyncOutcome.Deferred(null)
        val chunk = queue.readAll().take(MAX_BATCH_SIZE)
        if (chunk.isEmpty()) return ProgressSyncOutcome.Delivered

        return try {
            val response = api.sync(ProgressSyncRequestDto(chunk.map { it.toQueued().toWireDto() }))
            when {
                response.isSuccessful -> {
                    response.body()?.rejected?.forEach { Timber.d("Bible progress change rejected: %s", it.reason) }
                    // Every item sent leaves the queue, accepted or not: a rejected one would be rejected again.
                    queue.remove(chunk)
                    ProgressSyncOutcome.Delivered
                }
                response.code() == HTTP_BAD_REQUEST -> {
                    Timber.w("Bible progress chunk rejected wholesale; discarding %d change(s)", chunk.size)
                    queue.remove(chunk)
                    ProgressSyncOutcome.Discarded
                }
                else -> ProgressSyncOutcome.Deferred(response.toAppError())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Timber.d("Bible progress sync deferred: %s", t.javaClass.simpleName)
            ProgressSyncOutcome.Deferred(t.toAppError())
        }
    }

    override suspend fun flushAndClear() {
        // Best effort: the session is ending and another account may sign in on this device next.
        withTimeoutOrNull(FLUSH_TIMEOUT_MS) {
            while (queue.count() > 0 && syncOnce() !is ProgressSyncOutcome.Deferred) Unit
        }
        queue.clear()
        withContext(ioDispatcher) { runCatching { cache.clear() } }
        server.value = BibleProgress.EMPTY
    }

    companion object {
        const val MAX_BATCH_SIZE = 500
        private const val HTTP_BAD_REQUEST = 400
        private const val FLUSH_TIMEOUT_MS = 5_000L
    }
}
