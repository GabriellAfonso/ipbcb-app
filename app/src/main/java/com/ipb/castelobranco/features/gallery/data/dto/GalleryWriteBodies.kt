package com.ipb.castelobranco.features.gallery.data.dto

import com.ipb.castelobranco.features.gallery.domain.manage.AlbumDraft
import com.ipb.castelobranco.features.gallery.domain.manage.AlbumEdit
import com.ipb.castelobranco.features.gallery.domain.manage.Field
import com.ipb.castelobranco.features.gallery.domain.manage.PhotoEdit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/*
 * Request bodies of the gallery writes, built by hand: the project's `Json` has
 * `explicitNulls = false`, so a data class would drop `"parent_id": null` (move to the root) or
 * `"event_date": null` (clear the date) and the server would read "no change".
 */

private const val NAME = "name"
private const val DESCRIPTION = "description"
private const val EVENT_DATE = "event_date"
private const val PARENT_ID = "parent_id"
private const val DATE_TAKEN = "date_taken"
private const val ALBUM_ID = "album_id"
private const val IDS = "ids"

fun AlbumDraft.toCreateBody(): JsonObject = buildJsonObject {
    put(NAME, name)
    parentId?.let { put(PARENT_ID, it) }
    if (description.isNotBlank()) put(DESCRIPTION, description)
    eventDate?.let { put(EVENT_DATE, it) }
}

fun AlbumEdit.toPatchBody(): JsonObject = buildJsonObject {
    putField(NAME, name) { JsonPrimitive(it) }
    putField(DESCRIPTION, description) { JsonPrimitive(it) }
    putField(EVENT_DATE, eventDate) { it.toJson() }
    putField(PARENT_ID, parentId) { it.toJson() }
}

fun PhotoEdit.toPatchBody(): JsonObject = buildJsonObject {
    putField(NAME, name) { JsonPrimitive(it) }
    putField(DESCRIPTION, description) { JsonPrimitive(it) }
    putField(DATE_TAKEN, dateTaken) { it.toJson() }
    putField(ALBUM_ID, albumId) { JsonPrimitive(it) }
}

/** `parent_id` is always present: `null` orders the root albums. */
fun albumOrderBody(parentId: Long?, ids: List<Long>): JsonObject = buildJsonObject {
    put(PARENT_ID, parentId.toJson())
    put(IDS, ids.toJsonArray())
}

fun photoOrderBody(ids: List<Long>): JsonObject = buildJsonObject {
    put(IDS, ids.toJsonArray())
}

private fun <T> JsonObjectBuilder.putField(
    key: String,
    field: Field<T>,
    toJson: (T) -> JsonElement,
) {
    if (field is Field.Set) put(key, toJson(field.value))
}

private fun String?.toJson() = this?.let(::JsonPrimitive) ?: JsonNull

private fun Long?.toJson() = this?.let(::JsonPrimitive) ?: JsonNull

private fun List<Long>.toJsonArray() = JsonArray(map(::JsonPrimitive))
