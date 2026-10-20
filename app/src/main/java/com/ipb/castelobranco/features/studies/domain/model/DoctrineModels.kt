package com.ipb.castelobranco.features.studies.domain.model

enum class DoctrineKind(val wireName: String) {
    CATECHISM("catechism"),
    CONFESSION("confession");

    companion object {
        fun fromWire(value: String): DoctrineKind? = entries.firstOrNull { it.wireName == value }
    }
}

/** Short names for the Doutrina list and the reader's top bar; a slug not listed here keeps [title]. */
private val SHORT_TITLES = mapOf(
    "westminster-shorter-catechism" to "Breve Catecismo",
    "westminster-larger-catechism" to "Catecismo Maior",
    "westminster-confession-of-faith" to "Confissão de Fé",
)

fun doctrineShortTitle(slug: String, title: String): String = SHORT_TITLES[slug] ?: title

data class DoctrineSummary(
    val slug: String,
    val title: String,
    val shortTitle: String,
    val kind: DoctrineKind,
    val itemCount: Int,
) {
    /** "107 perguntas e respostas" / "33 capítulos". */
    val itemLabel: String
        get() = when (kind) {
            DoctrineKind.CATECHISM ->
                if (itemCount == 1) "1 pergunta e resposta" else "$itemCount perguntas e respostas"
            DoctrineKind.CONFESSION ->
                if (itemCount == 1) "1 capítulo" else "$itemCount capítulos"
        }
}

sealed interface DoctrineDocument {
    val slug: String
    val title: String

    data class Catechism(
        override val slug: String,
        override val title: String,
        val questions: List<CatechismQuestion>,
    ) : DoctrineDocument

    data class Confession(
        override val slug: String,
        override val title: String,
        val chapters: List<ConfessionChapter>,
    ) : DoctrineDocument
}

data class CatechismQuestion(
    val number: Int,
    val question: String,
    /** May contain line breaks (numbered rules) that the screen keeps. */
    val answer: String,
    val proofTexts: List<ProofText>,
)

/** A proof text as printed; it is a link only when the server interpreted it ([reference] not null). */
data class ProofText(
    val printed: String,
    val reference: BibleReference?,
)

/** [book] is the Bible feature's `abbrev`; empty [verses] means the whole chapter. */
data class BibleReference(
    val book: String,
    val chapter: Int,
    val verses: List<VerseSpan>,
) {
    val firstVerse: Int get() = verses.firstOrNull()?.start ?: 1
}

data class VerseSpan(val start: Int, val end: Int)

data class ConfessionChapter(
    val number: Int,
    val title: String,
    val paragraphs: List<ConfessionParagraph>,
)

data class ConfessionParagraph(
    val number: Int,
    val text: String,
    val lists: List<ParagraphList>,
    val textAfter: String?,
)

data class ParagraphList(
    val heading: String,
    val items: List<String>,
)

sealed interface DoctrineSearchResult {
    data class Question(val number: Int, val question: String) : DoctrineSearchResult

    data class Paragraph(val chapter: Int, val paragraph: Int, val preview: String) : DoctrineSearchResult
}
