package com.ipb.castelobranco.core.domain.usecase

import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.domain.startup.Preloadable
import com.ipb.castelobranco.core.domain.startup.Refreshable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PreloadDataUseCase @Inject constructor(
    private val preloadables: Set<@JvmSuppressWildcards Preloadable>,
    private val refreshables: Set<@JvmSuppressWildcards Refreshable>,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /** @param onDiskLoaded runs between the two phases: every cache is in memory, the network has not been asked. */
    suspend operator fun invoke(onDiskLoaded: () -> Unit = {}) {
        preloadCachesFromDisk()
        onDiskLoaded()
        refreshDataFromNetwork()
    }

    private suspend fun preloadCachesFromDisk() = withContext(ioDispatcher) {
        supervisorScope {
            preloadables
                .map { launch { runCatching { it.preload() } } }
                .joinAll()
        }
    }

    /** Network phase alone: after login, data the anonymous boot could not read (401) is fetched again. */
    suspend fun refreshDataFromNetwork() = withContext(ioDispatcher) {
        supervisorScope {
            refreshables
                .map { async { runCatching { it.refresh() } } }
                .forEach { it.await() }
        }
    }
}
