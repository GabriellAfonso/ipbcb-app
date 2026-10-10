// app/src/main/java/com/gabrielafonso/ipb/castelobranco/data/local/JsonSnapshotStorage.kt
package com.ipb.castelobranco.core.data.local

import android.content.Context
import com.ipb.castelobranco.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonSnapshotStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SnapshotStorage {

    private val dirName = "snapshots"

    private fun safeKey(key: String): String {
        val safe = key
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9_-]+"), "_")
            .trim('_')

        require(safe.isNotBlank()) { "Snapshot key inválida" }
        return safe
    }

    private fun dir(): File = File(context.filesDir, dirName).apply { mkdirs() }

    private fun jsonFileForKey(key: String): File =
        File(dir(), "${safeKey(key)}.json")

    private fun etagFileForKey(key: String): File =
        File(dir(), "${safeKey(key)}_etag.txt")

    override suspend fun save(key: String, json: String) = withContext(ioDispatcher) {
        jsonFileForKey(key).writeText(json)
    }

    override suspend fun loadOrNull(key: String): String? = withContext(ioDispatcher) {
        jsonFileForKey(key).takeIf { it.exists() }?.readText()
    }

    override suspend fun exists(key: String): Boolean = withContext(ioDispatcher) {
        jsonFileForKey(key).exists()
    }

    override suspend fun clear(key: String) = withContext(ioDispatcher) {
        val json = jsonFileForKey(key)
        if (json.exists()) json.delete()

        val etag = etagFileForKey(key)
        if (etag.exists()) etag.delete()
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        val dir = File(context.filesDir, dirName)
        if (dir.exists()) dir.deleteRecursively()
    }
    override suspend fun loadETagOrNull(key: String): String? = withContext(ioDispatcher) {
        etagFileForKey(key)
            .takeIf { it.exists() }
            ?.readText()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    override suspend fun saveETag(key: String, etag: String) = withContext(ioDispatcher) {
        etagFileForKey(key).writeText(etag)
    }

    override fun getAbsolutePathForDebug(key: String): String =
        jsonFileForKey(key).absolutePath
}