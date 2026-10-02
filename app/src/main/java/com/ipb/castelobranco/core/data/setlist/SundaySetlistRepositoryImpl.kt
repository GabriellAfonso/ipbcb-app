package com.ipb.castelobranco.core.data.setlist

import com.ipb.castelobranco.core.data.local.SnapshotStorage
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.mapError
import com.ipb.castelobranco.core.domain.setlist.SundaySetlist
import com.ipb.castelobranco.core.domain.setlist.SundaySetlistRepository
import com.ipb.castelobranco.core.network.error.toAppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the current Sunday setlist in memory and on disk (the server's JSON as received), so the
 * worship hub shows it offline from boot. The endpoint is `no-store`: there is no ETag to keep.
 */
@Singleton
class SundaySetlistRepositoryImpl @Inject constructor(
    private val api: SetlistApi,
    private val storage: SnapshotStorage,
    private val json: Json,
) : SundaySetlistRepository {

    private val state = MutableStateFlow<SundaySetlist?>(null)
    private val mutex = Mutex()

    override val stored: StateFlow<SundaySetlist?> = state.asStateFlow()

    suspend fun preload() {
        val raw = runCatching { storage.loadOrNull(STORAGE_KEY) }.getOrNull() ?: return
        val setlist = runCatching { json.decodeFromString(SetlistDto.serializer(), raw).toDomain() }
            .onFailure { Timber.w(it, "Stored Sunday setlist unreadable; discarding") }
            .getOrNull()
        mutex.withLock {
            if (setlist == null) {
                runCatching { storage.clear(STORAGE_KEY) }
            } else if (state.value == null) {
                state.value = setlist
            }
        }
    }

    override suspend fun refreshCurrent(): Result<Unit> = runCatching {
        val response = api.getCurrent()
        if (!response.isSuccessful) {
            val error = response.toAppError()
            if (error is AppError.Auth && error.code == HTTP_FORBIDDEN) clear()
            throw error
        }
        val current = response.body()?.setlist
        if (current == null) clear() else store(current.toDomain())
    }.mapError()

    override suspend fun store(setlist: SundaySetlist) {
        mutex.withLock {
            state.value = setlist
            runCatching { storage.save(STORAGE_KEY, json.encodeToString(SetlistDto.serializer(), setlist.toDto())) }
                .onFailure { Timber.w(it, "Failed to persist Sunday setlist") }
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            state.value = null
            runCatching { storage.clear(STORAGE_KEY) }
                .onFailure { Timber.w(it, "Failed to delete stored Sunday setlist") }
        }
    }

    private companion object {
        const val STORAGE_KEY = "sunday_setlist"
        const val HTTP_FORBIDDEN = 403
    }
}
