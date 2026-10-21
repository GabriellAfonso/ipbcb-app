package com.ipb.castelobranco.features.admin.events.testing

import com.ipb.castelobranco.features.admin.events.domain.model.EventChanges
import com.ipb.castelobranco.features.admin.events.domain.model.EventRecord
import com.ipb.castelobranco.features.admin.events.domain.model.EventStatusFilter
import com.ipb.castelobranco.features.admin.events.domain.model.EventVisibility
import com.ipb.castelobranco.features.admin.events.domain.repository.EventsAdminRepository
import java.time.LocalDate
import java.time.LocalTime

class FakeEventsAdminRepository : EventsAdminRepository {
    var listResult: Result<List<EventRecord>> = Result.success(emptyList())
    var getResult: Result<EventRecord> = Result.success(record())
    var writeResult: Result<EventRecord>? = null
    var deleteResult: Result<Unit> = Result.success(Unit)

    val listCalls = mutableListOf<EventStatusFilter>()
    val creates = mutableListOf<Pair<EventChanges, ByteArray>>()
    val updates = mutableListOf<Triple<Int, EventChanges, ByteArray?>>()
    val deletes = mutableListOf<Int>()

    override suspend fun list(status: EventStatusFilter): Result<List<EventRecord>> {
        listCalls += status
        return listResult
    }

    override suspend fun get(id: Int): Result<EventRecord> = getResult

    override suspend fun create(changes: EventChanges, image: ByteArray): Result<EventRecord> {
        creates += changes to image
        return writeResult ?: Result.success(record(id = NEW_ID))
    }

    override suspend fun update(id: Int, changes: EventChanges, image: ByteArray?): Result<EventRecord> {
        updates += Triple(id, changes, image)
        return writeResult ?: getResult
    }

    override suspend fun delete(id: Int): Result<Unit> {
        deletes += id
        return deleteResult
    }

    companion object {
        const val NEW_ID = 99
    }
}

fun record(
    id: Int = 1,
    title: String = "Congresso",
    start: LocalDate = LocalDate.of(2026, 11, 14),
    end: LocalDate? = null,
    time: LocalTime? = LocalTime.of(19, 30),
    location: String? = "Templo",
    visibility: EventVisibility = EventVisibility.PUBLIC,
) = EventRecord(
    id = id,
    title = title,
    startDate = start,
    endDate = end,
    startTime = time,
    location = location,
    description = null,
    imageUrl = "https://host/ipbcb/media/events/$id.jpg",
    visibility = visibility,
    createdByName = "Ana",
    createdAt = null,
    updatedAt = null,
)
