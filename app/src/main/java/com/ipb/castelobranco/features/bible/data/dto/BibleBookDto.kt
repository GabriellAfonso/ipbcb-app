package com.ipb.castelobranco.features.bible.data.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Resposta crua de `GET /api/bible/{translation}/`.
 *
 * - [abbrev]: vem em minúsculo ("gn", "at", "1co").
 * - [chapters]: chapters[i] = lista de versículos (strings) do capítulo i+1.
 * - [headings]: headings[i] = títulos de seção do capítulo i+1; ausente = sem títulos.
 *
 * Total: 66 livros, 1.189 capítulos, 31.104 versículos, ~4,2 MB por tradução.
 */
@Serializable
data class BibleBookDto(
    val abbrev: String,
    val name: String,
    val chapters: List<List<@Serializable(with = VerseSerializer::class) String>>,
    val headings: List<List<BibleHeadingDto>> = emptyList(),
)

/** Título de seção: aparece antes do versículo [verse] (1-based); [level] de 1 a 3. */
@Serializable
data class BibleHeadingDto(
    val verse: Int,
    val level: Int,
    val title: String,
)

// Some translations have a verse encoded as ["part a","part b"] instead of "part a part b".
// Accepts both and joins array elements with a space.
private object VerseSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Verse", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("VerseSerializer requires JSON")
        return when (val el = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> el.content
            is JsonArray -> el.joinToString(" ") { it.jsonPrimitive.content }
            else -> throw SerializationException("Unexpected verse element: $el")
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        (encoder as JsonEncoder).encodeJsonElement(JsonPrimitive(value))
    }
}
