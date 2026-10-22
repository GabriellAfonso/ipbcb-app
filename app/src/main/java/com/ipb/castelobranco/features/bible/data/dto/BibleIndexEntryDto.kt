package com.ipb.castelobranco.features.bible.data.dto

import kotlinx.serialization.Serializable

/** Entrada de `index.json` gravada pelo app: o livro sem o texto. */
@Serializable
data class BibleIndexEntryDto(
    val abbrev: String,
    val name: String,
    val verseCounts: List<Int>,
)
