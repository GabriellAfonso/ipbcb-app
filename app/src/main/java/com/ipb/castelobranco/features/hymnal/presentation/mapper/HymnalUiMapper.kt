package com.ipb.castelobranco.features.hymnal.presentation.mapper

import com.ipb.castelobranco.features.hymnal.domain.model.Hymn
import com.ipb.castelobranco.features.hymnal.domain.model.HymnLyric
import com.ipb.castelobranco.features.hymnal.domain.model.HymnLyricType

private const val GROUP_SIZE = 100
private const val OTHERS_LABEL = "Outros"
const val CHORUS_LABEL = "Coro"

/** Hymns of one hundred-range ("1 – 100") of the list. */
data class HymnGroup(val label: String, val hymns: List<Hymn>)

/**
 * Splits [hymns] (already ordered) into hundred-ranges by the leading digits of the number. Numbers without
 * digits go to a trailing "Outros" group. Order inside each group is kept.
 */
fun groupHymnsByHundred(hymns: List<Hymn>): List<HymnGroup> {
    val (numbered, others) = hymns.partition { leadingNumber(it.number) != null }
    val groups = numbered
        .groupBy { (leadingNumber(it.number)!! - 1).coerceAtLeast(0) / GROUP_SIZE }
        .toSortedMap()
        .map { (bucket, items) ->
            val start = bucket * GROUP_SIZE + 1
            HymnGroup(label = "$start – ${start + GROUP_SIZE - 1}", hymns = items)
        }
    return if (others.isEmpty()) groups else groups + HymnGroup(OTHERS_LABEL, others)
}

private fun leadingNumber(number: String): Int? = number.trim().takeWhile { it.isDigit() }.toIntOrNull()

/** Card label per stanza: "1ª estrofe"… counting only verses, "Coro" for choruses, null for other parts. */
fun stanzaLabels(lyrics: List<HymnLyric>): List<String?> {
    var verse = 0
    return lyrics.map { lyric ->
        when (lyric.type) {
            HymnLyricType.VERSE -> "${++verse}ª estrofe"
            HymnLyricType.CHORUS -> CHORUS_LABEL
            HymnLyricType.OTHER -> null
        }
    }
}
