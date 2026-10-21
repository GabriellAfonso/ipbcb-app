package com.ipb.castelobranco.features.admin.events.domain.repository

import com.ipb.castelobranco.features.admin.events.domain.model.EventChanges
import com.ipb.castelobranco.features.admin.events.domain.model.EventRecord
import com.ipb.castelobranco.features.admin.events.domain.model.EventStatusFilter

/**
 * Events management. Nothing is kept: the list is read again on every visit, which at tens of events a
 * year is cheaper than keeping a copy in step. Failures are `AppError`; a vanished event is a 404 whose
 * `userMessage` is [EVENT_GONE].
 */
interface EventsAdminRepository {
    suspend fun list(status: EventStatusFilter): Result<List<EventRecord>>

    suspend fun get(id: Int): Result<EventRecord>

    /** @param image the cropped JPEG; required to create. */
    suspend fun create(changes: EventChanges, image: ByteArray): Result<EventRecord>

    /** @param image a new JPEG, or null to keep the current one. */
    suspend fun update(id: Int, changes: EventChanges, image: ByteArray?): Result<EventRecord>

    suspend fun delete(id: Int): Result<Unit>

    companion object {
        const val EVENT_GONE = "Este evento não existe mais."
    }
}

/** Reads an image's size without decoding it; null when the bytes are not an image. */
fun interface ImageBoundsReader {
    fun read(bytes: ByteArray): ImageBounds?
}

data class ImageBounds(val width: Int, val height: Int)
