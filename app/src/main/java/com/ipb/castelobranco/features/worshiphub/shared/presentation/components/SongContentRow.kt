package com.ipb.castelobranco.features.worshiphub.shared.presentation.components

import androidx.compose.ui.graphics.Color

/**
 * Linha de uma lista de conteúdo de música. Cifras e letras são registros ligados a uma música
 * ([songId] diferente de [id]); em listas da própria música os dois coincidem, por isso o
 * default. [isPinned] só é lido quando a tela passa `onTogglePin`.
 */
data class SongContentRow(
    val id: Int,
    val songId: Int = id,
    val songName: String,
    val isPinned: Boolean = false,
    val chips: List<SongContentChip> = emptyList(),
    /** Matching lyrics line from "Buscar na letra", shown under the name. */
    val snippet: String? = null,
)

data class SongContentChip(val text: String, val color: Color)
