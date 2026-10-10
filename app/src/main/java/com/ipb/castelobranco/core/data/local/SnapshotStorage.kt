package com.ipb.castelobranco.core.data.local

interface SnapshotStorage {
    suspend fun save(key: String, json: String)
    suspend fun loadOrNull(key: String): String?
    suspend fun exists(key: String): Boolean = loadOrNull(key) != null
    suspend fun clear(key: String)
    suspend fun clearAll()

    suspend fun loadETagOrNull(key: String): String?
    suspend fun saveETag(key: String, etag: String)

    fun getAbsolutePathForDebug(key: String): String
}