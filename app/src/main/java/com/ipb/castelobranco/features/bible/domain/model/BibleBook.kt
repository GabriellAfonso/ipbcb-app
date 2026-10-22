package com.ipb.castelobranco.features.bible.domain.model

/**
 * Livro da Bíblia já mapeado para o domínio.
 *
 * - [abbrev] vem da API em minúsculo ("gn"). Use [displayAbbrev] para UI.
 * - [chapters] é a lista de capítulos, cada um com a lista de versículos (texto cru).
 */
data class BibleBook(
    val abbrev: String,
    val name: String,
    val chapters: List<List<String>>,
) {
    val chapterCount: Int get() = chapters.size

    /** "gn" → "Gn", "1co" → "1Co", "at" → "At". Mantém dígitos no início. */
    val displayAbbrev: String
        get() {
            val raw = abbrev.trim()
            if (raw.isEmpty()) return raw
            // Pega o primeiro caractere alfabético e capitaliza só ele.
            val sb = StringBuilder()
            var capitalizedFirstLetter = false
            for (c in raw) {
                if (!capitalizedFirstLetter && c.isLetter()) {
                    sb.append(c.uppercaseChar())
                    capitalizedFirstLetter = true
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }

    fun chapter(chapter: Int): List<String>? =
        chapters.getOrNull(chapter - 1)
}
