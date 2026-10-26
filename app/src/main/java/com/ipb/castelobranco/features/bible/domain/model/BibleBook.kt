package com.ipb.castelobranco.features.bible.domain.model

/**
 * Entrada do índice da Bíblia: o livro sem o texto, sempre em memória.
 *
 * - [abbrev] vem da API em minúsculo ("gn"). Use [displayAbbrev] para UI.
 * - [verseCounts] tem a quantidade de versículos de cada capítulo (posição `i` = capítulo `i + 1`).
 */
data class BibleBook(
    val abbrev: String,
    val name: String,
    val verseCounts: List<Int>,
) {
    val chapterCount: Int get() = verseCounts.size

    fun verseCount(chapter: Int): Int? = verseCounts.getOrNull(chapter - 1)

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
}

/**
 * Texto de um livro: [chapters] é a lista de capítulos, cada um com a lista de versículos.
 * [headings] é paralelo a [chapters] (títulos de seção de cada capítulo); vazio quando a tradução no disco não tem.
 */
data class BibleBookText(
    val abbrev: String,
    val chapters: List<List<String>>,
    val headings: List<List<BibleHeading>> = emptyList(),
) {
    fun chapter(chapter: Int): List<String>? = chapters.getOrNull(chapter - 1)

    fun headings(chapter: Int): List<BibleHeading> = headings.getOrNull(chapter - 1).orEmpty()
}

/** Título de seção mostrado antes do versículo [verse]. [level] vai de 1 (seção) a 3 (subseção da subseção). */
data class BibleHeading(
    val verse: Int,
    val level: Int,
    val title: String,
)

enum class Testament { OLD, NEW }

/** Gênesis a Malaquias: os primeiros livros do índice, que vem na ordem canônica. */
const val OLD_TESTAMENT_BOOK_COUNT = 39

/** O índice dividido por testamento, na ordem canônica; um testamento sem livros fica de fora. */
fun List<BibleBook>.byTestament(): List<Pair<Testament, List<BibleBook>>> =
    listOf(Testament.OLD to take(OLD_TESTAMENT_BOOK_COUNT), Testament.NEW to drop(OLD_TESTAMENT_BOOK_COUNT))
        .filter { (_, books) -> books.isNotEmpty() }

/** Livro de [abbrev] (sem diferenciar maiúsculas); se não existir, o primeiro do índice. */
fun List<BibleBook>.resolve(abbrev: String): BibleBook? =
    firstOrNull { it.abbrev.equals(abbrev, ignoreCase = true) } ?: firstOrNull()
