package com.ipb.castelobranco.features.hymnal.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class HymnDto(
    val number: String,
    val title: String,
    val lyrics: List<HymnLyricDto>,
    // Nullable with a default on purpose: snapshots cached before this field existed have no
    // "id" key, and a non-null field without a default would throw MissingFieldException when
    // decoding them — breaking the offline hymnal on upgrade.
    val id: Int? = null,
)

@Serializable
data class HymnLyricDto(
    val type: String, // "verse" | "chorus" (importante p/ UI)
    val text: String
)
