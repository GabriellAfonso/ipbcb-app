package com.ipb.castelobranco.features.admin.panel.domain

import com.ipb.castelobranco.core.domain.access.Access
import javax.inject.Inject

class VisiblePanelCardsUseCase @Inject constructor() {
    operator fun invoke(access: Access): List<PanelCard> =
        PanelCard.entries.filter { it.requirement.isMetBy(access) }
}
