package com.ipb.castelobranco.features.worshiphub.tables.data.api

internal object SuggestedSongsFixedEncoder {

    fun encode(fixedByPosition: Map<Int, Int>): String? {
        if (fixedByPosition.isEmpty()) return null

        val encoded = fixedByPosition
            .toList()
            .sortedBy { (pos, _) -> pos }
            .joinToString(separator = ",") { (pos, playedId) -> "$pos:$playedId" }
            .trim()

        return encoded.takeIf { it.isNotBlank() }
    }
}