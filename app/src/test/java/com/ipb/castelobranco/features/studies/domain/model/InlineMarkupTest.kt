package com.ipb.castelobranco.features.studies.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class InlineMarkupTest {

    @Test
    fun `bold and italic become spans`() {
        assertEquals(
            listOf(
                InlineSpan("O "),
                InlineSpan("Exílio", bold = true),
                InlineSpan(" e o "),
                InlineSpan("remanescente", italic = true),
            ),
            InlineMarkup.parse("O **Exílio** e o *remanescente*"),
        )
    }

    @Test
    fun `unmatched markers stay literal`() {
        assertEquals(listOf(InlineSpan("2 * 3 = 6 e **fim")), InlineMarkup.parse("2 * 3 = 6 e **fim"))
    }

    @Test
    fun `empty markers are literal`() {
        assertEquals(listOf(InlineSpan("****")), InlineMarkup.parse("****"))
    }

    @Test
    fun `plain text is one span`() {
        assertEquals(listOf(InlineSpan("Deus é meu juiz")), InlineMarkup.parse("Deus é meu juiz"))
    }
}
