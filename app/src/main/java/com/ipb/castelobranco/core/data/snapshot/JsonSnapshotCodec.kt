package com.ipb.castelobranco.core.data.snapshot

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class JsonSnapshotCodec<T>(
    private val json: Json,
    private val serializer: KSerializer<T>
) : SnapshotCodec<T> {

    override fun encode(value: T): String =
        json.encodeToString(serializer, value)

    override fun decode(raw: String): T =
        json.decodeFromString(serializer, raw)
}
