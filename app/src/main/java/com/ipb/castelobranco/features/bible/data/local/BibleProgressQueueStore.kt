package com.ipb.castelobranco.features.bible.data.local

import com.ipb.castelobranco.core.data.local.SnapshotStorage
import com.ipb.castelobranco.features.bible.domain.model.ProgressChange
import com.ipb.castelobranco.features.bible.domain.model.ProgressTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Durable queue of progress changes waiting to be sent, oldest first, holding **one state per target**: a newer
 * change of the same chapter or video replaces the older one (mark + unmark offline sends only the unmark).
 *
 * Same shape as the hymnal view queue: whole-file writes over [SnapshotStorage], every read-modify-write under one
 * [Mutex], an unreadable file counts as empty. [pending] mirrors the file so the screen can apply it on top of the
 * server state; it is null until the file is first read.
 */
@Singleton
class BibleProgressQueueStore @Inject constructor(
    private val storage: SnapshotStorage,
    private val json: Json,
) {

    private val mutex = Mutex()
    private val serializer = ListSerializer(QueuedProgressChange.serializer())
    private val _pending = MutableStateFlow<List<ProgressChange>?>(null)
    val pending: StateFlow<List<ProgressChange>?> = _pending.asStateFlow()

    /** Reads the file into [pending] when it was never read. */
    suspend fun load() {
        if (_pending.value != null) return
        mutex.withLock { if (_pending.value == null) _pending.value = readUnlocked() }
    }

    suspend fun upsert(changes: List<ProgressChange>) {
        if (changes.isEmpty()) return
        mutex.withLock {
            val targets = changes.mapTo(mutableSetOf()) { it.target }
            val merged = readUnlocked().filterNot { it.target in targets } + changes.distinctByLastTarget()
            val capped = if (merged.size > MAX_QUEUE_SIZE) {
                Timber.d("Bible progress queue full, dropping %d oldest change(s)", merged.size - MAX_QUEUE_SIZE)
                merged.takeLast(MAX_QUEUE_SIZE)
            } else {
                merged
            }
            writeUnlocked(capped)
        }
    }

    suspend fun readAll(): List<ProgressChange> = mutex.withLock { readUnlocked() }

    suspend fun count(): Int = mutex.withLock { readUnlocked().size }

    /**
     * Removes exactly [sent] (same target **and** time). A change made to the same target while the request was in
     * flight has another time and stays queued.
     */
    suspend fun remove(sent: Collection<ProgressChange>) {
        if (sent.isEmpty()) return
        val sentSet = sent.toSet()
        mutex.withLock { writeUnlocked(readUnlocked().filterNot { it in sentSet }) }
    }

    suspend fun clear() = mutex.withLock {
        runCatching { storage.clear(STORAGE_KEY) }.onFailure { Timber.w(it, "Failed to clear bible progress queue") }
        _pending.value = emptyList()
    }

    private suspend fun readUnlocked(): List<ProgressChange> {
        val raw = storage.loadOrNull(STORAGE_KEY) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw).mapNotNull { it.toDomain() } }
            .onFailure { Timber.w(it, "Bible progress queue unreadable, treating as empty") }
            .getOrDefault(emptyList())
    }

    private suspend fun writeUnlocked(changes: List<ProgressChange>) {
        runCatching { storage.save(STORAGE_KEY, json.encodeToString(serializer, changes.map { it.toQueued() })) }
            .onFailure { Timber.w(it, "Failed to persist bible progress queue") }
        _pending.value = changes
    }

    /** Within one batch the last change of a target wins, keeping the batch order otherwise. */
    private fun List<ProgressChange>.distinctByLastTarget(): List<ProgressChange> {
        val last = mutableMapOf<ProgressTarget, ProgressChange>()
        forEach { last[it.target] = it }
        return filter { last[it.target] === it }
    }

    companion object {
        const val STORAGE_KEY = "bible_progress_queue"

        /** Every chapter of the Bible is 1 189; the rest is room for videos. */
        const val MAX_QUEUE_SIZE = 5000
    }
}
