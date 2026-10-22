package com.ipb.castelobranco.features.hymnal.data.mapper

import com.ipb.castelobranco.features.hymnal.data.dto.HymnDto
import com.ipb.castelobranco.features.hymnal.data.dto.HymnLyricDto
import com.ipb.castelobranco.features.hymnal.domain.model.HymnLyricType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HymnMapperTest {

    // region HymnDto.toDomain()

    @Test
    fun `toDomain maps number, title and lyrics correctly`() {
        val dto = HymnDto(
            number = "42",
            title = "Castelo Forte",
            lyrics = listOf(
                HymnLyricDto(type = "verse", text = "Estrofe 1"),
                HymnLyricDto(type = "chorus", text = "Refrão"),
            )
        )

        val hymn = dto.toDomain()

        assertEquals("42", hymn.number)
        assertEquals("Castelo Forte", hymn.title)
        assertEquals(2, hymn.lyrics.size)
        assertEquals(HymnLyricType.VERSE, hymn.lyrics[0].type)
        assertEquals("Estrofe 1", hymn.lyrics[0].text)
        assertEquals(HymnLyricType.CHORUS, hymn.lyrics[1].type)
        assertEquals("Refrão", hymn.lyrics[1].text)
    }

    // endregion

    // region lyricTypeOf (tested via toDomain)

    @Test
    fun `lyric type verse maps to VERSE`() {
        val dto = HymnDto("1", "T", listOf(HymnLyricDto("verse", "x")))
        assertEquals(HymnLyricType.VERSE, dto.toDomain().lyrics[0].type)
    }

    @Test
    fun `lyric type chorus maps to CHORUS`() {
        val dto = HymnDto("1", "T", listOf(HymnLyricDto("chorus", "x")))
        assertEquals(HymnLyricType.CHORUS, dto.toDomain().lyrics[0].type)
    }

    @Test
    fun `lyric type unknown value maps to OTHER`() {
        val dto = HymnDto("1", "T", listOf(HymnLyricDto("bridge", "x")))
        assertEquals(HymnLyricType.OTHER, dto.toDomain().lyrics[0].type)
    }

    @Test
    fun `lyric type is case insensitive - Verse maps to VERSE`() {
        val dto = HymnDto("1", "T", listOf(HymnLyricDto("Verse", "x")))
        assertEquals(HymnLyricType.VERSE, dto.toDomain().lyrics[0].type)
    }

    @Test
    fun `lyric type is case insensitive - CHORUS maps to CHORUS`() {
        val dto = HymnDto("1", "T", listOf(HymnLyricDto("CHORUS", "x")))
        assertEquals(HymnLyricType.CHORUS, dto.toDomain().lyrics[0].type)
    }

    @Test
    fun `lyric type with surrounding spaces is trimmed - space verse space maps to VERSE`() {
        val dto = HymnDto("1", "T", listOf(HymnLyricDto(" verse ", "x")))
        assertEquals(HymnLyricType.VERSE, dto.toDomain().lyrics[0].type)
    }

    // endregion

    // region List<HymnDto>.toDomain() ordering

    @Test
    fun `list toDomain sorts numerically not lexicographically`() {
        val dtos = listOf(
            HymnDto("10", "Dez", emptyList()),
            HymnDto("2", "Dois", emptyList()),
            HymnDto("1", "Um", emptyList()),
        )

        val result = dtos.toDomain()

        assertEquals(listOf("1", "2", "10"), result.map { it.number })
    }

    @Test
    fun `list toDomain places non-numeric numbers at the end`() {
        val dtos = listOf(
            HymnDto("ABC", "Letra", emptyList()),
            HymnDto("3", "Três", emptyList()),
            HymnDto("1", "Um", emptyList()),
        )

        val result = dtos.toDomain()

        assertEquals(listOf("1", "3", "ABC"), result.map { it.number })
    }

    @Test
    fun `list toDomain places empty number string at the end`() {
        val dtos = listOf(
            HymnDto("", "Sem número", emptyList()),
            HymnDto("5", "Cinco", emptyList()),
        )

        val result = dtos.toDomain()

        assertEquals(listOf("5", ""), result.map { it.number })
    }

    @Test
    fun `list toDomain with mix of numeric and non-numeric puts numerics first in ascending order`() {
        val dtos = listOf(
            HymnDto("XYZ", "Letra", emptyList()),
            HymnDto("20", "Vinte", emptyList()),
            HymnDto("", "Vazio", emptyList()),
            HymnDto("3", "Três", emptyList()),
        )

        val result = dtos.toDomain()
        val numbers = result.map { it.number }

        assertEquals("3", numbers[0])
        assertEquals("20", numbers[1])
        // non-numerics at the end (order among them by string)
        assertTrue(numbers.indexOf("3") < numbers.indexOf("XYZ"))
        assertTrue(numbers.indexOf("20") < numbers.indexOf(""))
    }

    // endregion
}
