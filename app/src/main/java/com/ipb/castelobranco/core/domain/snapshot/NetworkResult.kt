package com.ipb.castelobranco.core.domain.snapshot

sealed class NetworkResult<out T> {
    data class Success<T>(
        val body: T,
        val etag: String?
    ) : NetworkResult<T>()

    data object NotModified : NetworkResult<Nothing>()
    data class Failure(val throwable: Throwable) : NetworkResult<Nothing>()
}
