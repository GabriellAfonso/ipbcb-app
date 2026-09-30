package com.ipb.castelobranco.features.profile.data.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards `GET /api/me/profile/` decoding across backend changes.
 *
 * - `active` was vestigial on both sides and the backend stopped sending it; a non-nullable field
 *   without default made decoding throw, so the app dropped it.
 * - Backend 012 removed `is_admin` and added `roles` / `permissions`. A profile saved on disk in the
 *   old shape must still decode — as "no role" — and an unknown value in the new fields must never
 *   throw, because a throw deletes the cached file and loses the whole profile.
 */
class MeProfileDtoBackwardCompatibilityTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `decodes a payload without active`() {
        val raw = """
            {"name":"Joao Silva","is_member":true,"roles":[],"permissions":{},"photo_url":null}
        """.trimIndent()

        val dto = json.decodeFromString<MeProfileDto>(raw)

        assertEquals("Joao Silva", dto.name)
        assertTrue(dto.isMember)
        assertNull(dto.photoUrl)
    }

    @Test
    fun `decodes a payload still carrying active`() {
        val raw = """
            {"name":"Joao Silva","active":true,"is_member":true,
             "photo_url":"https://example.com/photo.jpg"}
        """.trimIndent()

        val dto = json.decodeFromString<MeProfileDto>(raw)

        assertEquals("Joao Silva", dto.name)
        assertEquals("https://example.com/photo.jpg", dto.photoUrl)
    }

    @Test
    fun `decodes a payload without photo_url`() {
        val raw = """{"name":"Joao Silva","is_member":false}"""

        val dto = json.decodeFromString<MeProfileDto>(raw)

        assertNull(dto.photoUrl)
    }

    @Test
    fun `an old cache with is_admin decodes as no role`() {
        val raw = """
            {"name":"Ana","is_member":true,"is_admin":true,"photo_url":"https://example.com/a.png"}
        """.trimIndent()

        val dto = json.decodeFromString<MeProfileDto>(raw)

        assertEquals("Ana", dto.name)
        assertEquals("https://example.com/a.png", dto.photoUrl)
        assertTrue(dto.roles.isEmpty())
        assertTrue(dto.permissions.isEmpty())
    }

    @Test
    fun `decodes the backend 012 shape`() {
        val raw = """
            {"name":"Ana Paula","is_member":true,"photo_url":null,
             "roles":[{"id":"leader","name":"Liderança"},{"id":"media","name":"Mídia"}],
             "permissions":{"members":"manage","schedule":"manage","songs":"manage","gallery":"manage",
                            "events":"manage","notices":"manage","reports.hymnal_history":"view"}}
        """.trimIndent()

        val dto = json.decodeFromString<MeProfileDto>(raw)

        assertEquals(listOf("leader", "media"), dto.roles.map { it.id })
        assertEquals(7, dto.permissions.size)
        assertEquals("view", dto.permissions["reports.hymnal_history"])
    }

    @Test
    fun `keeps null levels and never throws on unknown values`() {
        val raw = """
            {"name":"Ana","is_member":false,
             "roles":[{"id":"treasurer","name":"Tesouraria"}],
             "permissions":{"members":null,"songs":"superowner","reports.attendance":"view"}}
        """.trimIndent()

        val dto = json.decodeFromString<MeProfileDto>(raw)

        assertTrue(dto.permissions.containsKey("members"))
        assertNull(dto.permissions["members"])
        assertEquals("superowner", dto.permissions["songs"])
        assertEquals("treasurer", dto.roles.single().id)
    }

    @Test
    fun `member_id absent or null means not linked`() {
        val absent = json.decodeFromString<MeProfileDto>("""{"name":"Ana","is_member":true}""")
        val nulled = json.decodeFromString<MeProfileDto>("""{"name":"Ana","is_member":true,"member_id":null}""")

        assertNull(absent.memberId)
        assertNull(nulled.memberId)
    }

    @Test
    fun `decodes the linked member id`() {
        val dto = json.decodeFromString<MeProfileDto>("""{"name":"Ana","is_member":true,"member_id":12}""")

        assertEquals(12L, dto.memberId)
    }
}
