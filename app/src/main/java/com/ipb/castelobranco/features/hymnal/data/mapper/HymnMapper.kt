package com.ipb.castelobranco.features.hymnal.data.mapper

import com.ipb.castelobranco.features.hymnal.data.dto.HymnDto
import com.ipb.castelobranco.features.hymnal.domain.model.Hymn
import com.ipb.castelobranco.features.hymnal.domain.model.HymnLyric
import com.ipb.castelobranco.features.hymnal.domain.model.HymnLyricType

fun HymnDto.toDomain(): Hymn =
    Hymn(
        id = id,
        number = number,
        title = title,
        lyrics = lyrics.map { l ->
            HymnLyric(
                type = lyricTypeOf(l.type),
                text = l.text
            )
        }
    )

/**
 * Orders by the leading digits of the number, then by the suffix after them, so "110-A" sits right after "110"
 * instead of at the end. Numbers without leading digits go last, alphabetically.
 */
fun List<HymnDto>.toDomain(): List<Hymn> =
    map { it.toDomain() }.sortedWith(
        compareBy(
            { leadingDigits(it.number).toIntOrNull() ?: Int.MAX_VALUE },
            { it.number.trim().removePrefix(leadingDigits(it.number)) },
            { it.number }
        )
    )

private fun leadingDigits(number: String): String = number.trim().takeWhile { it.isDigit() }

private fun lyricTypeOf(raw: String): HymnLyricType =
    when (raw.trim().lowercase()) {
        "verse" -> HymnLyricType.VERSE
        "chorus" -> HymnLyricType.CHORUS
        else -> HymnLyricType.OTHER
    }
