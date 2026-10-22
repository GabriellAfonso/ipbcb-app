package com.ipb.castelobranco.features.bible.domain.model

/**
 * Referência única para um versículo dentro do capítulo atual.
 * Usada para manter a seleção (copiar/compartilhar) na ViewModel.
 */
data class VerseRef(
    val bookAbbrev: String,
    val chapter: Int,
    val verse: Int,
)
