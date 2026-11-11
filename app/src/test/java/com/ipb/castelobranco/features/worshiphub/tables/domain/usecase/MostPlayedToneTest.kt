package com.ipb.castelobranco.features.worshiphub.tables.domain.usecase

import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySet
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySetItem
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** A use case over a repository whose played-Sundays snapshot is [sundays]. */
fun observePlayedSundaysOf(sundays: List<SundaySet> = emptyList()): ObservePlayedSundaysUseCase =
    ObservePlayedSundaysUseCase(
        mockk<SongsRepository> { every { observeSongsBySunday() } returns flowOf(SnapshotState.Data(sundays)) },
    )

class MostPlayedToneTest {

    private val oceans = Song(id = 1, title = "Oceans", artist = "Hillsong", categoryName = "Louvor")

    private fun sunday(vararg items: SundaySetItem) = SundaySet(date = "01/01/2026", songs = items.toList())

    private fun item(tone: String, songId: Int = 1, title: String = "Oceans", artist: String = "Hillsong") =
        SundaySetItem(position = 1, title = title, artist = artist, tone = tone, songId = songId)

    @Test
    fun `picks the tone played most often`() {
        val sundays = listOf(sunday(item("D")), sunday(item("G")), sunday(item("G")))

        assertEquals("G", sundays.mostPlayedToneOf(oceans))
    }

    @Test
    fun `never played song has no tone`() {
        val sundays = listOf(sunday(item("G", songId = 2, title = "Way Maker", artist = "Sinach")))

        assertEquals("", sundays.mostPlayedToneOf(oceans))
        assertEquals("", emptyList<SundaySet>().mostPlayedToneOf(oceans))
    }

    @Test
    fun `tie goes to the most recent sunday`() {
        val sundays = listOf(sunday(item("A")), sunday(item("D")), sunday(item("D")), sunday(item("A")))

        assertEquals("A", sundays.mostPlayedToneOf(oceans))
    }

    @Test
    fun `matches by id even when the title changed`() {
        val sundays = listOf(sunday(item("E", title = "Oceanos")))

        assertEquals("E", sundays.mostPlayedToneOf(oceans))
    }

    @Test
    fun `entry without id falls back to title and artist`() {
        val sundays = listOf(sunday(item("C", songId = 0)), sunday(item("F", songId = 0, artist = "Outro")))

        assertEquals("C", sundays.mostPlayedToneOf(oceans))
    }

    @Test
    fun `use case skips loading and errors`() = runTest {
        val sundays = listOf(sunday(item("G")))
        val repository = mockk<SongsRepository> {
            every { observeSongsBySunday() } returns flowOf(SnapshotState.Loading, SnapshotState.Data(sundays))
        }

        assertEquals(listOf(sundays), ObservePlayedSundaysUseCase(repository)().toList())
    }
}
