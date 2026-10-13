package com.ipb.castelobranco.core.domain.snapshot

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

abstract class BaseSnapshotRepository<Dto, Domain>(
    private val cache: SnapshotCache<Dto>,
    private val fetcher: SnapshotFetcher<Dto>,
    private val mapper: (Dto) -> Domain,
    private val logger: Logger = Logger.Noop,
    private val tag: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    // Estado quente que mantém o dado vivo enquanto o App estiver aberto
    private val _state = MutableStateFlow<SnapshotState<Domain>>(SnapshotState.Loading)

    fun observe(): StateFlow<SnapshotState<Domain>> = _state.asStateFlow()

    // Método vital para o MainViewModel carregar o cache logo no boot
    suspend fun preload() {
        withContext(ioDispatcher) {
            val cached = cache.load()
            if (cached != null) {
                _state.value = SnapshotState.Data(mapper(cached))
            }
        }
    }

    fun getCurrentState(): SnapshotState<Domain> = _state.value

    /**
     * The state now, loading the cache from disk when nothing is in memory yet — a process started by
     * a worker or a push never ran the boot preload.
     */
    suspend fun loadCurrentState(): SnapshotState<Domain> {
        if (_state.value !is SnapshotState.Data) preload()
        return _state.value
    }

    protected fun emitError(error: AppError) {
        _state.value = SnapshotState.Error(error)
    }

    suspend fun clearCache() {
        withContext(ioDispatcher) { cache.clear() }
        _state.value = SnapshotState.Loading
    }

    suspend fun refresh(): RefreshResult {
        return try {
            val etag = withContext(ioDispatcher) { cache.loadETag() }
            when (val result = fetcher.fetch(etag)) {
                is NetworkResult.NotModified -> {
                    if (_state.value is SnapshotState.Loading) preload()
                    RefreshResult.NotModified
                }
                is NetworkResult.Success -> {
                    withContext(ioDispatcher) { cache.save(result.body, result.etag) }
                    val newData = mapper(result.body)
                    _state.value = SnapshotState.Data(newData)
                    RefreshResult.Updated
                }
                is NetworkResult.Failure -> {
                    val error = result.throwable.toAppError()
                    if (error is AppError.Auth) {
                        withContext(ioDispatcher) { cache.clear() }
                        _state.value = SnapshotState.Error(error)
                        RefreshResult.Error(error)
                    } else {
                        val cached = withContext(ioDispatcher) { cache.load() }
                        if (cached != null) {
                            _state.value = SnapshotState.Data(mapper(cached))
                            RefreshResult.CacheUsed
                        } else {
                            _state.value = SnapshotState.Error(error)
                            RefreshResult.Error(error)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Timber.w(t, "Snapshot refresh failed for %s", tag)
            RefreshResult.Error(t)
        }
    }
}