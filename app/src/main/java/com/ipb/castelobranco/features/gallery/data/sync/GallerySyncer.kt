package com.ipb.castelobranco.features.gallery.data.sync

import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.domain.auth.SessionPresenceProvider
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.gallery.data.api.GalleryApi
import com.ipb.castelobranco.features.gallery.data.dto.GalleryChangesDto
import com.ipb.castelobranco.features.gallery.data.dto.toDelta
import com.ipb.castelobranco.features.gallery.data.dto.toDomain
import com.ipb.castelobranco.features.gallery.data.dto.toSnapshot
import com.ipb.castelobranco.features.gallery.data.local.GalleryLegacyMigration
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.data.snapshot.GalleryIndexSnapshot
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalChange
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the device's gallery in step with the server's change feed.
 *
 * One sync at a time: a sync requested while another runs is [GallerySyncResult.Skipped] — the
 * running one serves it. Every write (snapshot, files) happens under the lock, after a session check,
 * so a sync that outlives sign-out writes nothing. [clear] cancels the running sync first.
 *
 * After every successful sync the disk is reconciled against the index: originals and covers no
 * longer in it are deleted, and missing covers are downloaded. That single rule covers deletions,
 * `full_sync_required`, replaced covers and the orphans left by the legacy migration.
 */
@Singleton
class GallerySyncer @Inject constructor(
    private val api: GalleryApi,
    private val snapshotCache: SnapshotCache<GalleryIndexSnapshot>,
    private val mediaStore: GalleryMediaStore,
    private val migration: GalleryLegacyMigration,
    private val session: SessionPresenceProvider,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    private val mutex = Mutex()
    private var running: Deferred<GallerySyncResult>? = null
    private val rerunRequested = AtomicBoolean(false)
    private var loaded = false

    private val _state = MutableStateFlow(GalleryLocalState.EMPTY)
    val state: StateFlow<GalleryLocalState> = _state.asStateFlow()

    private val _status = MutableStateFlow(GallerySyncStatus())
    val status: StateFlow<GallerySyncStatus> = _status.asStateFlow()

    /** Migrates the legacy layout if needed and reads the index and the files from disk. Idempotent. */
    suspend fun load() {
        mutex.withLock { withContext(ioDispatcher) { ensureLoaded() } }
    }

    suspend fun sync(): GallerySyncResult {
        if (!mutex.tryLock()) return GallerySyncResult.Skipped
        return runLocked()
    }

    /**
     * A sync that must see a write the server just accepted. Unlike [sync] it is never lost: when a
     * sync is already running (it may have read the feed before the write), that sync runs once more
     * before releasing the lock. Several writes in a row collapse into one extra sync.
     */
    suspend fun syncAfterWrite(): GallerySyncResult {
        rerunRequested.set(true)
        if (!mutex.tryLock()) return GallerySyncResult.Skipped
        return runLocked()
    }

    /**
     * Applies a write's result to the index and the disk, keeping the cursor. Waits for a running
     * sync instead of racing it: that sync read the index before this write, and saving after it
     * would drop the change. [afterApply] runs under the same lock, after the index is saved and
     * before files are pruned — the upload queue uses it to place an original the index now lists.
     *
     * @return `false` when nothing was applied: no index yet (the next sync brings everything) or no
     * session.
     */
    suspend fun applyLocal(change: GalleryLocalChange, afterApply: suspend () -> Unit = {}): Boolean =
        mutex.withLock {
            withContext(ioDispatcher) {
                ensureLoaded()
                val current = _state.value.index
                if (current == null || !session.isLoggedIn()) return@withContext false
                val next = current.apply(change)
                snapshotCache.save(next.toSnapshot(), null)
                afterApply()
                prune(next)
                _state.value = withFiles(next)
                true
            }
        }

    /** Runs syncs under the lock the caller took, as long as writes keep asking for one more. */
    private suspend fun runLocked(): GallerySyncResult {
        _status.update { it.copy(isRunning = true) }
        val result = try {
            var last: GallerySyncResult
            do {
                rerunRequested.set(false)
                last = syncOnce()
            } while (rerunRequested.get() && last !is GallerySyncResult.Skipped)
            last
        } finally {
            running = null
            mutex.unlock()
            _status.update { it.copy(isRunning = false) }
        }
        // A write that asked between the last check and the unlock: nobody else will serve it.
        if (rerunRequested.get() && mutex.tryLock()) return runLocked()
        return result
    }

    private suspend fun syncOnce(): GallerySyncResult = coroutineScope {
        val job = async(ioDispatcher) { syncLocked() }
        running = job
        try {
            job.await()
        } catch (e: CancellationException) {
            // Cancelled by clear(), not by our caller: the caller just gets "nothing done".
            currentCoroutineContext().ensureActive()
            GallerySyncResult.Skipped
        }
    }

    /** Re-reads which files are on disk — after the download worker saved originals. */
    suspend fun refreshLocalFiles() = withContext(ioDispatcher) {
        _state.update { current -> withFiles(current.index) }
    }

    /** Sign-out: cancels the running sync, then deletes the index, the cursor and every file. */
    suspend fun clear() {
        rerunRequested.set(false)
        running?.cancel()
        mutex.withLock {
            withContext(ioDispatcher) {
                snapshotCache.clear()
                mediaStore.clearAll()
            }
            loaded = true
            _state.value = GalleryLocalState.EMPTY
            _status.value = GallerySyncStatus()
        }
    }

    private suspend fun syncLocked(): GallerySyncResult {
        ensureLoaded()
        if (!session.isLoggedIn()) return finish(GallerySyncResult.Skipped)

        val current = _state.value.index
        val first = fetch(current?.cursor).getOrElse { return fail(it) }
        val next = when {
            first.fullSyncRequired -> {
                val full = fetch(since = null).getOrElse { return fail(it) }
                GalleryIndex.fromFullRead(full.toDelta())
            }
            current == null || current.cursor == null -> GalleryIndex.fromFullRead(first.toDelta())
            else -> current.applyDelta(first.toDelta())
        }

        currentCoroutineContext().ensureActive()
        if (!session.isLoggedIn()) return finish(GallerySyncResult.Skipped)

        snapshotCache.save(next.toSnapshot(), null)
        reconcile(next)
        val local = withFiles(next)
        _state.value = local
        val missing = next.photos.values.count { it.id !in local.originals }
        return finish(GallerySyncResult.Synced(missing))
    }

    private suspend fun fetch(since: String?): Result<GalleryChangesDto> = try {
        val response = api.getChanges(since)
        val body = response.body()
        when {
            !response.isSuccessful -> Result.failure(response.toAppError())
            body == null -> Result.failure(AppError.Unknown(message = EMPTY_BODY_MESSAGE))
            else -> Result.success(body)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toAppError())
    }

    /** disk ⊆ index: drops what the index no longer has, then fetches the covers it lacks. */
    private suspend fun reconcile(index: GalleryIndex) {
        prune(index)
        index.albums.values
            .mapNotNull { it.coverUrl }
            .toSet()
            .filterNot { mediaStore.coverFile(it).exists() }
            .forEach { url -> downloadCover(url) }
    }

    /** Deletes the originals and covers the index no longer uses. No network. */
    private fun prune(index: GalleryIndex) {
        mediaStore.originalFiles()
            .filterKeys { it !in index.photos }
            .values
            .forEach(mediaStore::delete)

        val wantedNames = index.albums.values.mapNotNull { it.coverUrl }.map(mediaStore::coverName).toSet()
        mediaStore.coverFiles()
            .filter { it.name !in wantedNames }
            .forEach(mediaStore::delete)
    }

    /** A cover that fails is skipped: its tile stays black until a later sync gets it. */
    private suspend fun downloadCover(url: String) {
        try {
            val response = api.downloadFile(url)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                body.byteStream().use { mediaStore.saveCover(url, it) }
            } else {
                Timber.w("Gallery cover skipped: HTTP %d", response.code())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Gallery cover skipped")
        }
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        migration.runIfNeeded()
        val index = snapshotCache.load()?.toDomain()
        _state.value = withFiles(index)
        loaded = true
    }

    private fun withFiles(index: GalleryIndex?): GalleryLocalState {
        if (index == null) return GalleryLocalState.EMPTY
        val covers = index.albums.values
            .mapNotNull { it.coverUrl }
            .distinct()
            .mapNotNull { url -> mediaStore.coverFile(url).takeIf { it.exists() }?.let { url to it } }
            .toMap()
        return GalleryLocalState(index = index, originals = mediaStore.originalFiles(), covers = covers)
    }

    private fun fail(error: Throwable): GallerySyncResult {
        val appError = error.toAppError()
        Timber.w(appError, "Gallery sync failed")
        _status.update { it.copy(lastError = appError, hasAnswered = true) }
        return GallerySyncResult.Failed(appError)
    }

    private fun finish(result: GallerySyncResult): GallerySyncResult {
        _status.update {
            it.copy(
                lastError = if (result is GallerySyncResult.Synced) null else it.lastError,
                hasAnswered = true,
            )
        }
        return result
    }

    private companion object {
        const val EMPTY_BODY_MESSAGE = "Resposta vazia do feed da galeria"
    }
}
