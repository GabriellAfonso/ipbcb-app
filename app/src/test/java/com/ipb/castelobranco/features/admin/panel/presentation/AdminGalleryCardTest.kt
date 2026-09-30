package com.ipb.castelobranco.features.admin.panel.presentation

import com.ipb.castelobranco.features.admin.panel.domain.PanelCard
import com.ipb.castelobranco.features.admin.panel.presentation.navigation.AdminNav
import com.ipb.castelobranco.features.admin.panel.presentation.screens.toAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminGalleryCardTest {

    @Test
    fun `the Galeria card is enabled and opens the gallery`() {
        var opened = 0
        val nav = AdminNav(back = {}, register = {}, schedule = {}, reports = {}, members = {}, gallery = { opened++ })

        val action = PanelCard.GALLERY.toAction(nav, memberCount = null)
        action.onClick()

        assertTrue(action.enabled)
        assertEquals("Galeria", action.label)
        assertEquals(1, opened)
    }
}
