package com.ipb.castelobranco.features.worshiphub.shared.domain

import com.ipb.castelobranco.core.domain.setlist.SetlistItem
import com.ipb.castelobranco.core.domain.setlist.SundaySetlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SundaySectionTest {

    private val sunday = LocalDate.of(2026, 10, 4)
    private val setlist = SundaySetlist(
        date = sunday,
        items = listOf(
            SetlistItem(position = 3, songId = 7, title = "C", artist = "", tone = "E"),
            SetlistItem(position = 1, songId = 5, title = "A", artist = "", tone = "G"),
            SetlistItem(position = 2, songId = 6, title = "B", artist = "", tone = "A#"),
        ),
        savedByName = null,
        savedAt = "",
    )

    @Test
    fun `entries follow the positions and carry the key`() {
        val section = buildSundaySection(setlist, mapOf(5 to 50, 6 to 60, 7 to 70))!!

        assertEquals(sunday, section.date)
        assertEquals(listOf(50, 60, 70), section.entries.map { it.contentId })
        assertEquals(listOf("G", "A#", "E"), section.entries.map { it.tone })
    }

    @Test
    fun `songs without content in this list are left out`() {
        val section = buildSundaySection(setlist, mapOf(6 to 60))!!

        assertEquals(listOf(6), section.entries.map { it.songId })
    }

    @Test
    fun `no setlist or no song with content means no section`() {
        assertNull(buildSundaySection(null, mapOf(5 to 50)))
        assertNull(buildSundaySection(setlist, emptyMap()))
    }
}
