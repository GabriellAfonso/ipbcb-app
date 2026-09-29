package com.ipb.castelobranco.features.admin.panel.domain

import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.features.admin.panel.domain.CardRequirement.AtLeast
import com.ipb.castelobranco.features.admin.panel.domain.CardRequirement.HasRole

/**
 * The panel's cards in display order, each with what the user needs to see it
 * (`specs/admin/spec.md` §2.1). Whether a card is enabled or grey is presentation, not here.
 */
enum class PanelCard(val requirement: CardRequirement) {
    WORSHIP(AtLeast(Scope.SONGS, AccessLevel.MANAGE)),
    ATTENDANCE(HasRole(Role.ADMIN)),
    SCHEDULE(AtLeast(Scope.SCHEDULE, AccessLevel.MANAGE)),
    MEMBERS(AtLeast(Scope.MEMBERS, AccessLevel.VIEW)),
    NOTICES(AtLeast(Scope.NOTICES, AccessLevel.MANAGE)),
    REPORTS(AtLeast(Scope.HYMNAL_HISTORY_REPORT, AccessLevel.VIEW)),
    GALLERY(AtLeast(Scope.GALLERY, AccessLevel.MANAGE)),
    EVENTS(AtLeast(Scope.EVENTS, AccessLevel.MANAGE)),
    NOTIFICATIONS(HasRole(Role.ADMIN)),
}
