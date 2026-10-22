package com.ipb.castelobranco.core.domain.snapshot

sealed class RefreshResult {
    data object Updated : RefreshResult()
    data object NotModified : RefreshResult()
    data object CacheUsed : RefreshResult()
    data class Error(val throwable: Throwable) : RefreshResult()
}
