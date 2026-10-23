package com.ipb.castelobranco.features.admin.profiles.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime

class ProfilesFormattingTest {

    private val grantedAt = OffsetDateTime.parse("2026-10-22T17:03:00Z")

    @Test
    fun `the grant note names the leader and the moment in Brasilia time`() {
        assertEquals("Liberado por Pr. João em 22/10/2026 às 14:03", grantedLabel("Pr. João", grantedAt))
    }

    @Test
    fun `the grant note keeps what is known`() {
        assertEquals("Liberado por Pr. João", grantedLabel("Pr. João", null))
        assertEquals("Liberado em 22/10/2026 às 14:03", grantedLabel(null, grantedAt))
        assertNull(grantedLabel(null, null))
    }
}
