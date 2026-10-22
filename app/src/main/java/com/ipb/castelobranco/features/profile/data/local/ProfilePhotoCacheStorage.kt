package com.ipb.castelobranco.features.profile.data.local

import android.content.Context
import com.ipb.castelobranco.core.data.local.StorageDirConstants
import com.ipb.castelobranco.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfilePhotoCacheStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val dirName = StorageDirConstants.PROFILE

    private fun dir(): File = File(context.filesDir, dirName).apply { mkdirs() }

    private fun etagFile(): File = File(dir(), "profile_photo_etag.txt")
    private fun urlFile(): File = File(dir(), "profile_photo_url.txt")

    suspend fun loadETagOrNull(): String? = withContext(ioDispatcher) {
        val f = etagFile()
        if (!f.exists()) return@withContext null
        f.readText().trim().takeIf { it.isNotBlank() }
    }

    suspend fun saveETag(etag: String) = withContext(ioDispatcher) {
        etagFile().writeText(etag.trim())
    }

    suspend fun clearETag() = withContext(ioDispatcher) {
        etagFile().takeIf { it.exists() }?.delete()
    }

    suspend fun loadLastUrlOrNull(): String? = withContext(ioDispatcher) {
        val f = urlFile()
        if (!f.exists()) return@withContext null
        f.readText().trim().takeIf { it.isNotBlank() }
    }

    suspend fun saveLastUrl(url: String) = withContext(ioDispatcher) {
        urlFile().writeText(url.trim())
    }

    suspend fun clearLastUrl() = withContext(ioDispatcher) {
        urlFile().takeIf { it.exists() }?.delete()
    }

    suspend fun clearAll() = withContext(ioDispatcher) {
        clearETag()
        clearLastUrl()
    }
}