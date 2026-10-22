package com.ipb.castelobranco.core.data.local

interface SnapshotStorage {
    suspend fun save(key: String, json: String)
    suspend fun loadOrNull(key: String): String?
    suspend fun exists(key: String): Boolean = loadOrNull(key) != null
    suspend fun clear(key: String)
    suspend fun clearAll()

    /** Bytes the snapshot of [key] takes on disk (JSON + ETag); 0 when absent. */
    suspend fun sizeBytes(key: String): Long

    suspend fun loadETagOrNull(key: String): String?
    suspend fun saveETag(key: String, etag: String)

    fun getAbsolutePathForDebug(key: String): String
}