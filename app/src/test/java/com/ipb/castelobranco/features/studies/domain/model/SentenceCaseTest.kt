package com.ipb.castelobranco.features.studies.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SentenceCaseTest {

    @Test
    fun `keeps divine names and key nouns capitalized`() {
        assertEquals("Da ceia do Senhor", "DA CEIA DO SENHOR".toSentenceCase())
        assertEquals("De Deus e da Santíssima Trindade", "DE DEUS E DA SANTÍSSIMA TRINDADE".toSentenceCase())
        assertEquals("De Cristo, o Mediador", "DE CRISTO, O MEDIADOR".toSentenceCase())
        assertEquals("Da Lei de Deus", "DA LEI DE DEUS".toSentenceCase())
        assertEquals("Da Igreja", "DA IGREJA".toSentenceCase())
        assertEquals("O Antigo Testamento", "O ANTIGO TESTAMENTO".toSentenceCase())
    }

    @Test
    fun `lowercases everything else, including hyphenated and accented words`() {
        assertEquals("Do livre-arbítrio", "DO LIVRE-ARBÍTRIO".toSentenceCase())
        assertEquals("Da comunhão dos santos", "DA COMUNHÃO DOS SANTOS".toSentenceCase())
        assertEquals("Do culto religioso e do domingo", "DO CULTO RELIGIOSO E DO DOMINGO".toSentenceCase())
    }
}
