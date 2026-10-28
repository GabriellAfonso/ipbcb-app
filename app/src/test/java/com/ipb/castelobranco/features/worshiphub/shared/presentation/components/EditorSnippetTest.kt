package com.ipb.castelobranco.features.worshiphub.shared.presentation.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorSnippetTest {

    private val section = EditorSnippet("+ Refrão", "{chorus: Refrão}", block = true)
    private val chord = EditorSnippet("[ ] Acorde", "[]", block = false, cursorBack = 1)

    @Test
    fun `a section goes on its own line, with the cursor after it`() {
        val value = TextFieldValue("[G]Santo", TextRange(8))

        val result = value.insert(section)

        assertEquals("[G]Santo\n{chorus: Refrão}\n", result.text)
        assertEquals(TextRange(result.text.length), result.selection)
    }

    @Test
    fun `a section at the start of a line adds no blank line`() {
        val result = TextFieldValue("", TextRange(0)).insert(section)

        assertEquals("{chorus: Refrão}\n", result.text)
    }

    @Test
    fun `a chord goes in place, with the cursor between the brackets`() {
        val result = TextFieldValue("Santo", TextRange(0)).insert(chord)

        assertEquals("[]Santo", result.text)
        assertEquals(TextRange(1), result.selection)
    }
}
