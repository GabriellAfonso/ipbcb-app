package com.ipb.castelobranco.core.data.snapshot

import timber.log.Timber
import com.ipb.castelobranco.core.data.local.SnapshotStorage
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache

class LocalSnapshotCache<T>(
    private val storage: SnapshotStorage,
    private val codec: SnapshotCodec<T>,
    private val key: String
) : SnapshotCache<T> {

    override suspend fun load(): T? {
        val raw = storage.loadOrNull(key) ?: return null
        return try {
            codec.decode(raw)
        } catch (e: Exception) {
            // Arquivo corrompido (ex: escrita interrompida por OOM). Apaga e retorna null
            // para forçar novo download na próxima oportunidade.
            Timber.w(e, "Corrupt snapshot '%s', deleting", key)
            storage.clear(key)
            null
        }
    }

    override suspend fun exists(): Boolean = storage.exists(key)

    override suspend fun save(dto: T, etag: String?) {
        storage.save(key, codec.encode(dto))
        etag?.let { storage.saveETag(key, it) }
    }

    override suspend fun loadETag(): String? =
        storage.loadETagOrNull(key)

    override suspend fun clear() {
        storage.clear(key)
    }
}
