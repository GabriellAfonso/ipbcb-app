package com.ipb.castelobranco.features.admin.members.data.mapper

import com.ipb.castelobranco.features.admin.members.data.dto.HistoryEntryDto
import com.ipb.castelobranco.features.admin.members.data.dto.MemberRecordDto
import com.ipb.castelobranco.features.admin.members.data.dto.MemberSummaryDto
import com.ipb.castelobranco.features.admin.members.data.dto.NamedRefDto
import com.ipb.castelobranco.features.admin.members.domain.model.Gender
import com.ipb.castelobranco.features.admin.members.domain.model.MemberChanges
import com.ipb.castelobranco.features.admin.members.domain.model.MemberField
import com.ipb.castelobranco.features.admin.members.domain.model.NamedRef
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MemberMapperTest {

    /** Same configuration as `SerializationModule` — the one that drops null properties. */
    private val appJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `record dto maps every field`() {
        val dto = MemberRecordDto(
            id = 12,
            name = "Ana Souza",
            firstName = "Ana",
            lastName = "Souza",
            birthDate = "1990-04-02",
            gender = "F",
            status = NamedRefDto(1, "Ativo"),
            role = null,
            ministries = listOf(NamedRefDto(2, "Louvor")),
            baptismDate = "0001-06-12",
            isActive = false,
            photoUrl = "https://host/ipbcb/media/members/a.jpg",
            createdAt = "2026-09-25T14:02:11Z",
        )

        val record = dto.toDomain()

        assertEquals(LocalDate.of(1990, 4, 2), record.birthDate)
        assertEquals(Gender.FEMALE, record.gender)
        assertEquals(NamedRef(1, "Ativo"), record.status)
        assertNull(record.role)
        assertEquals(listOf(NamedRef(2, "Louvor")), record.ministries)
        assertEquals(LocalDate.of(1, 6, 12), record.baptismDate)
        assertFalse(record.isValid)
        assertEquals(Instant.parse("2026-09-25T14:02:11Z"), record.createdAt)
    }

    @Test
    fun `record dto with unknown gender and empty dates maps to nulls`() {
        val record = MemberRecordDto(
            id = 1, name = "X", gender = "?", isActive = true, createdAt = "2026-01-01T00:00:00Z",
        ).toDomain()

        assertNull(record.gender)
        assertNull(record.birthDate)
        assertNull(record.baptismDate)
        assertEquals("", record.firstName)
    }

    @Test
    fun `timestamps with a non-UTC offset map to the same instant`() {
        val record = MemberRecordDto(
            id = 1, name = "X", isActive = true, createdAt = "2026-02-23T21:21:35.359000-03:00",
        ).toDomain()
        val entry = HistoryEntryDto(
            id = 1, field = "created", changedAt = "2026-02-23T21:21:35.359000-03:00",
        ).toDomain()

        assertEquals(Instant.parse("2026-02-24T00:21:35.359Z"), record.createdAt)
        assertEquals(Instant.parse("2026-02-24T00:21:35.359Z"), entry.changedAt)
    }

    @Test
    fun `summary dto maps validity from is_active`() {
        val summary = MemberSummaryDto(3, "Bruno", null, null, isActive = false).toDomain()

        assertFalse(summary.isValid)
        assertNull(summary.status)
    }

    @Test
    fun `changes keep cleared fields as explicit nulls on the wire`() {
        val changes = MemberChanges(
            mapOf(
                MemberField.ROLE to null,
                MemberField.BIRTH_DATE to null,
                MemberField.STATUS to 2,
            )
        )

        val body = changes.toJsonObject()
        val encoded = appJson.decodeFromString<JsonObject>(appJson.encodeToString(JsonObject.serializer(), body))

        assertEquals(JsonNull, encoded["role_id"])
        assertEquals(JsonNull, encoded["birth_date"])
        assertEquals("2", encoded["status_id"]!!.jsonPrimitive.content)
        assertFalse(encoded.containsKey("name"))
    }

    @Test
    fun `changes encode dates gender ministries and validity in the api shape`() {
        val body = MemberChanges(
            mapOf(
                MemberField.NAME to "Ana",
                MemberField.BIRTH_DATE to LocalDate.of(1, 4, 2),
                MemberField.GENDER to Gender.MALE,
                MemberField.MINISTRIES to setOf(5, 2),
                MemberField.IS_VALID to false,
            )
        ).toJsonObject()

        assertEquals("Ana", body["name"]!!.jsonPrimitive.content)
        assertEquals("0001-04-02", body["birth_date"]!!.jsonPrimitive.content)
        assertEquals("M", body["gender"]!!.jsonPrimitive.content)
        assertEquals(listOf("2", "5"), body["ministry_ids"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("false", body["is_active"]!!.jsonPrimitive.content)
    }

    @Test
    fun `history entry without editor maps to null editor`() {
        val entry = HistoryEntryDto(
            id = 39, editor = null, field = "created", changedAt = "2026-09-25T14:02:11Z",
        ).toDomain()

        assertNull(entry.editor)
        assertTrue(entry.oldValue == null && entry.newValue == null)
    }
}
