package com.ipb.castelobranco.features.worshiphub.tables.domain.usecase

import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySet
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySetItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSundaysUseCaseTest {

    private val search = SearchSundaysUseCase()

    private val april = SundaySet(
        date = "07/04/2024",
        songs = listOf(SundaySetItem(1, "Grandioso És Tu", "Cantor Cristão", "C#")),
    )
    private val may = SundaySet(
        date = "12/05/2024",
        songs = listOf(SundaySetItem(1, "Oceans", "Hillsong", "Bb")),
    )
    private val index = search.index(listOf(april, may))

    @Test
    fun `blank query returns every sunday in order`() {
        assertEquals(listOf(april, may), search(index, "  "))
    }

    @Test
    fun `matches title ignoring accents and case`() {
        assertEquals(listOf(april), search(index, "grandioso es"))
    }

    @Test
    fun `matches artist`() {
        assertEquals(listOf(may), search(index, "hillsong"))
    }

    @Test
    fun `matches tone`() {
        assertEquals(listOf(may), search(index, "bb"))
    }

    @Test
    fun `matches date typed with slashes`() {
        assertEquals(listOf(april), search(index, "07/04"))
    }

    @Test
    fun `no match returns empty list`() {
        assertTrue(search(index, "inexistente").isEmpty())
    }
}
