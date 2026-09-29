package com.ipb.castelobranco.features.admin.panel.domain

import com.ipb.castelobranco.core.domain.access.Access
import com.ipb.castelobranco.core.domain.access.AccessLevel.MANAGE
import com.ipb.castelobranco.core.domain.access.AccessLevel.OWNER
import com.ipb.castelobranco.core.domain.access.AccessLevel.VIEW
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.features.admin.panel.domain.PanelCard.ATTENDANCE
import com.ipb.castelobranco.features.admin.panel.domain.PanelCard.EVENTS
import com.ipb.castelobranco.features.admin.panel.domain.PanelCard.GALLERY
import com.ipb.castelobranco.features.admin.panel.domain.PanelCard.MEMBERS
import com.ipb.castelobranco.features.admin.panel.domain.PanelCard.NOTICES
import com.ipb.castelobranco.features.admin.panel.domain.PanelCard.NOTIFICATIONS
import com.ipb.castelobranco.features.admin.panel.domain.PanelCard.REPORTS
import com.ipb.castelobranco.features.admin.panel.domain.PanelCard.SCHEDULE
import com.ipb.castelobranco.features.admin.panel.domain.PanelCard.WORSHIP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The backend 012 role matrix, as the profile reports it, against `specs/admin/spec.md` §2.1. */
class VisiblePanelCardsUseCaseTest {

    private val visibleCards = VisiblePanelCardsUseCase()

    private val admin = Access(
        roles = setOf(Role.ADMIN),
        hasAnyRole = true,
        levels = Scope.entries.associateWith { OWNER },
    )

    private val leader = Access(
        roles = setOf(Role.LEADER),
        hasAnyRole = true,
        levels = mapOf(
            Scope.MEMBERS to MANAGE, Scope.SCHEDULE to MANAGE, Scope.SONGS to MANAGE,
            Scope.GALLERY to MANAGE, Scope.EVENTS to MANAGE, Scope.NOTICES to MANAGE,
            Scope.HYMNAL_HISTORY_REPORT to VIEW,
        ),
    )

    private val media = Access(
        roles = setOf(Role.MEDIA),
        hasAnyRole = true,
        levels = mapOf(
            Scope.GALLERY to MANAGE, Scope.EVENTS to MANAGE, Scope.NOTICES to MANAGE,
            Scope.HYMNAL_HISTORY_REPORT to VIEW,
        ),
    )

    @Test
    fun `admin sees every card in display order`() {
        assertEquals(PanelCard.entries.toList(), visibleCards(admin))
    }

    @Test
    fun `leader sees everything but the admin-only placeholders`() {
        assertEquals(
            listOf(WORSHIP, SCHEDULE, MEMBERS, NOTICES, REPORTS, GALLERY, EVENTS),
            visibleCards(leader),
        )
    }

    @Test
    fun `media sees only reports and the gallery, events and notices placeholders`() {
        assertEquals(listOf(NOTICES, REPORTS, GALLERY, EVENTS), visibleCards(media))
    }

    @Test
    fun `leader plus media shows what the combined levels allow`() {
        val both = leader.copy(roles = setOf(Role.LEADER, Role.MEDIA))

        assertEquals(visibleCards(leader), visibleCards(both))
    }

    @Test
    fun `no role shows nothing`() {
        assertTrue(visibleCards(Access.NONE).isEmpty())
    }

    @Test
    fun `an unknown role alone shows nothing`() {
        val unknown = Access(roles = emptySet(), hasAnyRole = true, levels = emptyMap())

        assertTrue(visibleCards(unknown).isEmpty())
    }

    @Test
    fun `members needs only view`() {
        val viewer = Access(roles = emptySet(), hasAnyRole = true, levels = mapOf(Scope.MEMBERS to VIEW))

        assertEquals(listOf(MEMBERS), visibleCards(viewer))
        assertTrue(ATTENDANCE !in visibleCards(viewer) && NOTIFICATIONS !in visibleCards(viewer))
    }
}
