package com.ipb.castelobranco.features.worshiphub.tables.data.api

/** Encodes `exclude=position:songId,...` — Song ids, unlike `fixed`, which takes Played ids. */
internal object SuggestedSongsExcludeEncoder {

    fun encode(excludedByPosition: Map<Int, Set<Int>>): String? =
        excludedByPosition
            .toSortedMap()
            .flatMap { (position, songIds) -> songIds.sorted().map { songId -> "$position:$songId" } }
            .joinToString(separator = ",")
            .takeIf { it.isNotBlank() }
}
