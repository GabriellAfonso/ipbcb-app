package com.ipb.castelobranco.features.gallery.data.upload

import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import com.ipb.castelobranco.features.gallery.domain.upload.UploadState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The persistent upload queue: every change is saved before it is published, so the screen, the
 * worker and a process restart all see the same list. Items keep the order they were queued in.
 */
@Singleton
class GalleryUploadQueueStore @Inject constructor(
    private val snapshot: SnapshotCache<UploadQueueSnapshot>,
    private val mediaStore: GalleryMediaStore,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private var loaded = false

    private val _items = MutableStateFlow<List<UploadItem>>(emptyList())
    val items: StateFlow<List<UploadItem>> = _items.asStateFlow()

    /** Reads the queue from disk once. Idempotent. */
    suspend fun load() {
        mutate { it }
    }

    /** The oldest item still to be sent, if any. */
    suspend fun nextPending(): UploadItem? {
        load()
        return _items.value.firstOrNull { it.state != UploadState.Failed }
    }

    suspend fun pendingCount(): Int {
        load()
        return _items.value.count { it.state != UploadState.Failed }
    }

    suspend fun append(item: UploadItem) {
        mutate { it + item }
    }

    suspend fun markPrepared(uploadId: String, fileName: String, displayName: String) {
        mutate { items ->
            items.map {
                if (it.uploadId != uploadId) return@map it
                it.copy(state = UploadState.Prepared, fileName = fileName, displayName = displayName)
            }
        }
    }

    suspend fun markFailed(uploadId: String, reason: String) {
        mutate { items -> items.map { if (it.uploadId == uploadId) it.failed(reason) else it } }
    }

    /** Fails every pending item of [albumId]; returns how many. */
    suspend fun failAllOfAlbum(albumId: Long, reason: String): Int = failWhere(reason) { it.albumId == albumId }

    /** Fails every pending item; returns how many. */
    suspend fun failAllPending(reason: String): Int = failWhere(reason) { true }

    /** Drops the item; its file was moved elsewhere (it became a photo's original). */
    suspend fun remove(uploadId: String) {
        mutate { items -> items.filterNot { it.uploadId == uploadId } }
    }

    /** Drops the item and deletes its file. */
    suspend fun dismiss(uploadId: String) {
        mutate { items ->
            items.firstOrNull { it.uploadId == uploadId }?.let { mediaStore.uploadFile(it.fileName).delete() }
            items.filterNot { it.uploadId == uploadId }
        }
    }

    /** Sign-out: empties the queue on disk and in memory. Files go with the gallery folder. */
    suspend fun clear() {
        mutex.withLock {
            withContext(ioDispatcher) { snapshot.clear() }
            loaded = true
            _items.value = emptyList()
        }
    }

    private suspend fun failWhere(reason: String, predicate: (UploadItem) -> Boolean): Int {
        var count = 0
        mutate { items ->
            items.map {
                if (it.state == UploadState.Failed || !predicate(it)) return@map it
                count++
                it.failed(reason)
            }
        }
        return count
    }

    private suspend fun mutate(transform: (List<UploadItem>) -> List<UploadItem>) {
        mutex.withLock {
            withContext(ioDispatcher) {
                if (!loaded) {
                    _items.value = readFromDisk()
                    loaded = true
                }
                val next = transform(_items.value)
                if (next != _items.value) snapshot.save(UploadQueueSnapshot(next.map { it.toDto() }), null)
                _items.value = next
            }
        }
    }

    /** Items of a state this version does not know are dropped with their files. */
    private suspend fun readFromDisk(): List<UploadItem> =
        snapshot.load()?.items.orEmpty().mapNotNull { dto ->
            dto.toDomain() ?: run {
                mediaStore.uploadFile(dto.fileName).delete()
                null
            }
        }

    private fun UploadItem.failed(reason: String) = copy(state = UploadState.Failed, failure = reason)
}
