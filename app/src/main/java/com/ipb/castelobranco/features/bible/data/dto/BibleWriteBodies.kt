package com.ipb.castelobranco.features.bible.data.dto

import com.ipb.castelobranco.features.bible.domain.model.BiblePlace
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/*
 * Request bodies of the Bible video writes, built by hand: the project's `Json` has `explicitNulls = false`, so a
 * data class would drop `"chapter": null` and the server could not tell a whole-book place from a missing field.
 */

private const val URL = "url"
private const val PLACES = "places"
private const val BOOK = "book"
private const val CHAPTER = "chapter"
private const val VIDEO_IDS = "video_ids"

fun BiblePlace.toBody(): JsonObject = buildJsonObject {
    put(BOOK, book)
    put(CHAPTER, chapter?.let(::JsonPrimitive) ?: JsonNull)
}

fun List<BiblePlace>.toBody(): JsonArray = buildJsonArray { forEach { add(it.toBody()) } }

fun addVideoBody(url: String, places: List<BiblePlace>): JsonObject = buildJsonObject {
    put(URL, url)
    put(PLACES, places.toBody())
}

fun videoOrderBody(place: BiblePlace, videoIds: List<Long>): JsonObject = buildJsonObject {
    put(BOOK, place.book)
    put(CHAPTER, place.chapter?.let(::JsonPrimitive) ?: JsonNull)
    put(VIDEO_IDS, buildJsonArray { videoIds.forEach { add(JsonPrimitive(it)) } })
}
