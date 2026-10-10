package com.ipb.castelobranco.core.domain.snapshot

interface SnapshotCache<Dto> {
    suspend fun load(): Dto?

    /** Whether a snapshot is stored, without decoding it. */
    suspend fun exists(): Boolean = load() != null

    suspend fun save(dto: Dto, etag: String?)
    suspend fun loadETag(): String?
    suspend fun clear()
}
