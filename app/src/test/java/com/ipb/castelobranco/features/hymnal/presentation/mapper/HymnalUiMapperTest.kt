package com.ipb.castelobranco.features.hymnal.presentation.mapper

import com.ipb.castelobranco.features.hymnal.domain.model.Hymn
import com.ipb.castelobranco.features.hymnal.domain.model.HymnLyric
import com.ipb.castelobranco.features.hymnal.domain.model.HymnLyricType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HymnalUiMapperTest {

    private fun hymn(number: String) = Hymn(id = null, number = number, title = "Hino $number", lyrics = emptyList())

    @Test
    fun `groupHymnsByHundred splits by hundred ranges keeping order`() {
        val groups = groupHymnsByHundred(listOf(hymn("1"), hymn("42"), hymn("100"), hymn("101"), hymn("250")))

        assertEquals(listOf("1 – 100", "101 – 200", "201 – 300"), groups.map { it.label })
        assertEquals(listOf("1", "42", "100"), groups[0].hymns.map { it.number })
        assertEquals(listOf("101"), groups[1].hymns.map { it.number })
    }

    @Test
    fun `groupHymnsByHundred puts numbers without digits in a trailing Outros group`() {
        val groups = groupHymnsByHundred(listOf(hymn("5"), hymn("A"), hymn("12a")))

        assertEquals(listOf("1 – 100", "Outros"), groups.map { it.label })
        assertEquals(listOf("5", "12a"), groups[0].hymns.map { it.number })
        assertEquals(listOf("A"), groups[1].hymns.map { it.number })
    }

    @Test
    fun `groupHymnsByHundred returns empty for no hymns`() {
        assertTrue(groupHymnsByHundred(emptyList()).isEmpty())
    }

    @Test
    fun `stanzaLabels counts only verses and names the chorus`() {
        val lyrics = listOf(
            HymnLyric(HymnLyricType.VERSE, "a"),
            HymnLyric(HymnLyricType.CHORUS, "b"),
            HymnLyric(HymnLyricType.VERSE, "c"),
            HymnLyric(HymnLyricType.OTHER, "d"),
        )

        assertEquals(listOf("1ª estrofe", "Coro", "2ª estrofe", null), stanzaLabels(lyrics))
    }
}
