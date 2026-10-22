package com.ipb.castelobranco.features.hymnal.domain.model

data class Hymn(
    /** Server primary key. Null for hymns loaded from a snapshot cached before it was exposed. */
    val id: Int?,
    val number: String,
    val title: String,
    val lyrics: List<HymnLyric>
)

data class HymnLyric(
    val type: HymnLyricType,
    val text: String
)

enum class HymnLyricType {
    VERSE,
    CHORUS,
    OTHER
}
