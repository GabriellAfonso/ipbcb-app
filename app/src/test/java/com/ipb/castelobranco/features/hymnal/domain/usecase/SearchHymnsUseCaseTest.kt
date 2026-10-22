package com.ipb.castelobranco.features.hymnal.domain.usecase

import com.ipb.castelobranco.features.hymnal.domain.model.Hymn
import com.ipb.castelobranco.features.hymnal.domain.model.HymnLyric
import com.ipb.castelobranco.features.hymnal.domain.model.HymnLyricType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchHymnsUseCaseTest {

    private lateinit var useCase: SearchHymnsUseCase

    // region fixtures

    private val hymnSanto = Hymn(
        id = null,
        number = "1",
        title = "Santo, Santo, Santo",
        lyrics = listOf(
            HymnLyric(HymnLyricType.VERSE, "Santo, Santo, Santo! Senhor Deus todo-poderoso!"),
            HymnLyric(HymnLyricType.CHORUS, "Glória, glória ao Pai!")
        )
    )

    private val hymnPastor = Hymn(
        id = null,
        number = "23",
        title = "O Senhor é Meu Pastor",
        lyrics = listOf(
            HymnLyric(HymnLyricType.VERSE, "O Senhor é o meu pastor e nada me faltará")
        )
    )

    private val hymnGrande = Hymn(
        id = null,
        number = "100",
        title = "Grande é o Senhor",
        lyrics = listOf(
            HymnLyric(HymnLyricType.VERSE, "Grande é o Senhor e mui digno de louvor")
        )
    )

    private val allHymns = listOf(hymnSanto, hymnPastor, hymnGrande)

    // endregion

    @Before
    fun setup() {
        useCase = SearchHymnsUseCase()
    }

    // region empty / blank query

    @Test
    fun `empty query returns all hymns`() {
        val result = useCase(allHymns, "")

        assertEquals(allHymns, result)
    }

    @Test
    fun `blank query with spaces returns all hymns`() {
        val result = useCase(allHymns, "   ")

        assertEquals(allHymns, result)
    }

    @Test
    fun `blank query with tab returns all hymns`() {
        val result = useCase(allHymns, "\t")

        assertEquals(allHymns, result)
    }

    @Test
    fun `empty hymn list with non-blank query returns empty`() {
        val result = useCase(emptyList(), "santo")

        assertTrue(result.isEmpty())
    }

    // endregion

    // region search by number

    @Test
    fun `exact number match returns correct hymn`() {
        val result = useCase(allHymns, "23")

        assertEquals(listOf(hymnPastor), result)
    }

    @Test
    fun `partial number match returns all hymns whose number contains query`() {
        // "1" is contained in "1" and "100"
        val result = useCase(allHymns, "1")

        assertEquals(listOf(hymnSanto, hymnGrande), result)
    }

    @Test
    fun `number query with no match returns empty`() {
        val result = useCase(allHymns, "999")

        assertTrue(result.isEmpty())
    }

    // endregion

    // region search by title

    @Test
    fun `title exact word match returns correct hymn`() {
        val result = useCase(allHymns, "Pastor")

        assertEquals(listOf(hymnPastor), result)
    }

    @Test
    fun `title partial match returns correct hymn`() {
        val result = useCase(allHymns, "Grande")

        assertEquals(listOf(hymnGrande), result)
    }

    @Test
    fun `title search is case insensitive`() {
        val result = useCase(allHymns, "santo")

        assertEquals(listOf(hymnSanto), result)
    }

    @Test
    fun `title query matching multiple hymns returns all matches`() {
        // "é o" normalized to "e o" appears in title of hymnPastor and hymnGrande
        val result = useCase(allHymns, "é o")

        assertEquals(listOf(hymnPastor, hymnGrande), result)
    }

    // endregion

    // region search by lyrics

    @Test
    fun `lyrics match returns correct hymn`() {
        val result = useCase(allHymns, "louvor")

        assertEquals(listOf(hymnGrande), result)
    }

    @Test
    fun `lyrics search is case insensitive`() {
        val result = useCase(allHymns, "glória")

        assertEquals(listOf(hymnSanto), result)
    }

    @Test
    fun `lyrics match across multiple stanzas returns hymn once`() {
        // hymnSanto has "Senhor" in verse; should appear only once even with chorus present
        val result = useCase(allHymns, "todo-poderoso")

        assertEquals(listOf(hymnSanto), result)
    }

    // endregion

    // region cross-field matching

    @Test
    fun `query matching title in one hymn and lyrics in another returns both`() {
        // "Senhor" is in hymnSanto's lyrics, hymnPastor's title, and hymnGrande's title+lyrics
        val result = useCase(allHymns, "Senhor")

        assertEquals(listOf(hymnSanto, hymnPastor, hymnGrande), result)
    }

    @Test
    fun `query with no match in any field returns empty`() {
        val result = useCase(allHymns, "aleluia")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `query matching only number does not require title or lyrics match`() {
        val result = useCase(allHymns, "100")

        assertEquals(listOf(hymnGrande), result)
    }

    // endregion

    // region query trimming

    @Test
    fun `query with leading and trailing spaces is trimmed before search`() {
        val result = useCase(allHymns, "  Pastor  ")

        assertEquals(listOf(hymnPastor), result)
    }

    // endregion

    // region accent and punctuation insensitive search

    @Test
    fun `title search ignores accents`() {
        val result = useCase(allHymns, "e o Senhor")

        assertEquals(listOf(hymnGrande), result)
    }

    @Test
    fun `lyrics search ignores accents`() {
        val result = useCase(allHymns, "gloria")

        assertEquals(listOf(hymnSanto), result)
    }

    @Test
    fun `lyrics search ignores punctuation`() {
        val result = useCase(allHymns, "Santo Santo Santo")

        assertEquals(listOf(hymnSanto), result)
    }

    // endregion
}
