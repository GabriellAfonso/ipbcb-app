package com.ipb.castelobranco.core.data.snapshot

import com.ipb.castelobranco.core.data.local.SnapshotStorage
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class SnapshotCacheFactory(
    private val storage: SnapshotStorage,
    private val json: Json
) {

    fun <T> create(
        key: String,
        serializer: KSerializer<T>
    ): SnapshotCache<T> =
        LocalSnapshotCache(
            storage = storage,
            codec = JsonSnapshotCodec(json, serializer),
            key = key
        )
}
