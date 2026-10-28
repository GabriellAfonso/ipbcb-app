package com.ipb.castelobranco.features.worshiphub.hub

import com.ipb.castelobranco.core.domain.setlist.SetlistItem
import com.ipb.castelobranco.core.domain.setlist.SundaySetlist
import com.ipb.castelobranco.core.domain.worship.WorshipAccess
import com.ipb.castelobranco.features.worshiphub.hub.presentation.state.buildHubBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HubBandTest {

    private val wednesday = LocalDate.of(2026, 10, 28)
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val member = WorshipAccess(isWorshipMember = true, canSaveSetlist = false)
    private val setlist = SundaySetlist(
        date = LocalDate.of(2026, 11, 1),
        items = listOf(
            SetlistItem(position = 2, songId = 20, title = "Agnus Dei", artist = "Michael W. Smith", tone = "A"),
            SetlistItem(position = 1, songId = 10, title = "Porque Ele Vive", artist = "Bill Gaither", tone = "G"),
        ),
        savedByName = "Marcos Vinícius",
        savedAt = "2026-10-28T00:40:00Z",
    )

    @Test
    fun `setlist shows in position order with its chord charts and the save stamp`() {
        val band = buildHubBand(setlist, member, mapOf(10 to 100), wednesday, zone)!!

        assertEquals("Domingo, 01/11", band.dateLabel)
        assertEquals(listOf("Porque Ele Vive", "Agnus Dei"), band.songs.map { it.title })
        assertEquals(100, band.songs[0].chordChartId)
        assertNull(band.songs[1].chordChartId)
        assertEquals("Salvo por Marcos Vinícius · 27/10 às 21:40", band.savedLabel)
        assertFalse(band.canDelete)
    }

    @Test
    fun `whoever can save a setlist can remove the one shown`() {
        val saver = member.copy(canSaveSetlist = true)

        assertTrue(buildHubBand(setlist, saver, emptyMap(), wednesday, zone)!!.canDelete)
        assertFalse(buildHubBand(null, saver, emptyMap(), wednesday, zone)!!.canDelete)
    }

    @Test
    fun `without a setlist the band shows the next Sunday and whether the user can build one`() {
        val band = buildHubBand(null, member.copy(canSaveSetlist = true), emptyMap(), wednesday, zone)!!

        assertEquals("Domingo, 01/11", band.dateLabel)
        assertTrue(band.songs.isEmpty())
        assertTrue(band.canSave)
        assertNull(band.savedLabel)
    }

    @Test
    fun `deleted author and unreadable date`() {
        assertEquals(
            "Salvo em 27/10 às 21:40",
            buildHubBand(setlist.copy(savedByName = null), member, emptyMap(), wednesday, zone)!!.savedLabel,
        )
        assertNull(buildHubBand(setlist.copy(savedAt = "ontem"), member, emptyMap(), wednesday, zone)!!.savedLabel)
    }

    @Test
    fun `no band for who is not a worship member`() {
        assertNull(buildHubBand(setlist, WorshipAccess.NONE, emptyMap(), wednesday, zone))
    }
}
