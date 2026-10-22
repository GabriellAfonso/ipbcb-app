package com.ipb.castelobranco.features.hymnal.domain.usecase

import com.ipb.castelobranco.core.domain.util.normalize
import com.ipb.castelobranco.features.hymnal.domain.model.Hymn
import javax.inject.Inject

class SearchHymnsUseCase @Inject constructor() {
    operator fun invoke(hymns: List<Hymn>, query: String): List<Hymn> {
        val q = query.trim()
        if (q.isBlank()) return hymns
        val nq = q.normalize()
        return hymns.filter { hymn ->
            hymn.number.contains(nq, ignoreCase = true) ||
                hymn.title.normalize().contains(nq, ignoreCase = true) ||
                hymn.lyrics.any { it.text.normalize().contains(nq, ignoreCase = true) }
        }
    }
}
