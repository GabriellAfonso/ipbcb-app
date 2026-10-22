package com.ipb.castelobranco.features.admin.schedule.data.mapper

import com.ipb.castelobranco.features.admin.schedule.data.dto.MemberItemDto
import org.junit.Assert.assertEquals
import org.junit.Test

class MemberMapperTest {

    @Test
    fun `toDomain maps id and name correctly`() {
        val dto = MemberItemDto(id = 7, name = "João Silva")

        val result = dto.toDomain()

        assertEquals(7, result.id)
        assertEquals("João Silva", result.name)
    }

    @Test
    fun `toDomain with empty name`() {
        val dto = MemberItemDto(id = 1, name = "")

        val result = dto.toDomain()

        assertEquals("", result.name)
    }

    @Test
    fun `toDomain preserves special characters in name`() {
        val dto = MemberItemDto(id = 3, name = "José André da Conceição")

        val result = dto.toDomain()

        assertEquals("José André da Conceição", result.name)
    }
}
