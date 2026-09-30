package com.ipb.castelobranco.features.gallery.domain.tags

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NameSearchTest {

    @Test
    fun `ignores case and accents on both sides`() {
        assertTrue(NameSearch.matches("João Lima", "joao"))
        assertTrue(NameSearch.matches("JOÃO LIMA", "lima"))
        assertTrue(NameSearch.matches("Conceição", "CONCEICAO"))
        assertTrue(NameSearch.matches("Jose", "josé"))
    }

    @Test
    fun `matches anywhere in the name`() {
        assertTrue(NameSearch.matches("Maria Souza", "souz"))
        assertFalse(NameSearch.matches("Maria Souza", "pedro"))
    }

    @Test
    fun `blank query matches everyone`() {
        assertTrue(NameSearch.matches("Maria", ""))
        assertTrue(NameSearch.matches("Maria", "  "))
    }
}
