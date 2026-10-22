package com.ipb.castelobranco.core.data.snapshot

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class JsonSnapshotCodecTest {

    @Serializable
    data class SampleDto(val id: Int, val name: String)

    private lateinit var codec: JsonSnapshotCodec<SampleDto>

    @Before
    fun setUp() {
        codec = JsonSnapshotCodec(Json, SampleDto.serializer())
    }

    @Test
    fun `encode serializes dto to JSON string`() {
        val dto = SampleDto(id = 1, name = "Test")

        val result = codec.encode(dto)

        assertEquals("""{"id":1,"name":"Test"}""", result)
    }

    @Test
    fun `decode deserializes JSON string to dto`() {
        val raw = """{"id":42,"name":"Hello"}"""

        val result = codec.decode(raw)

        assertEquals(SampleDto(id = 42, name = "Hello"), result)
    }

    @Test
    fun `encode and decode round-trip preserves data`() {
        val original = SampleDto(id = 7, name = "Round-trip")

        val result = codec.decode(codec.encode(original))

        assertEquals(original, result)
    }

    @Test(expected = SerializationException::class)
    fun `decode invalid JSON throws SerializationException`() {
        codec.decode("not valid json {{")
    }
}
