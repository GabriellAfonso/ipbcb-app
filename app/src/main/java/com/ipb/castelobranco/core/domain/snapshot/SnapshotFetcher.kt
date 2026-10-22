package com.ipb.castelobranco.core.domain.snapshot

interface SnapshotFetcher<Dto> {
    suspend fun fetch(etag: String?): NetworkResult<Dto>
}
