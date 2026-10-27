package com.ipb.castelobranco.features.studies.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentErrorsTest {

    @Test
    fun `block paths point to the block`() {
        assertEquals(
            EditorLocation.Block(2, 1),
            EditorLocation.of("sections.2.blocks.1.list.items.3.proof_texts.0.reference"),
        )
    }

    @Test
    fun `section paths point to the section`() {
        assertEquals(EditorLocation.Section(0), EditorLocation.of("sections.0.title"))
        assertEquals(EditorLocation.Section(4), EditorLocation.of("sections.4.blocks"))
    }

    @Test
    fun `anything else goes to the top of the form`() {
        assertEquals(EditorLocation.Form, EditorLocation.of("sections"))
        assertEquals(EditorLocation.Form, EditorLocation.of(""))
        assertEquals(EditorLocation.Form, EditorLocation.of("content"))
    }
}
