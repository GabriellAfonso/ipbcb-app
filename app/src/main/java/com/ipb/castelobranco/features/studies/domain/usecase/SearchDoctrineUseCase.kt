package com.ipb.castelobranco.features.studies.domain.usecase

import com.ipb.castelobranco.features.studies.domain.model.DoctrineDocument
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSearchResult
import java.text.Normalizer
import javax.inject.Inject

/**
 * Search inside one document: case- and accent-insensitive, every word of the term must appear
 * (any order). In a catechism a purely numeric term puts the question with that number first.
 */
class SearchDoctrineUseCase @Inject constructor() {

    operator fun invoke(document: DoctrineDocument, term: String): List<DoctrineSearchResult> {
        val words = normalize(term).split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        return when (document) {
            is DoctrineDocument.Catechism -> searchCatechism(document, words, term.trim().toIntOrNull())
            is DoctrineDocument.Confession -> searchConfession(document, words)
        }
    }

    private fun searchCatechism(
        document: DoctrineDocument.Catechism,
        words: List<String>,
        number: Int?,
    ): List<DoctrineSearchResult> {
        val byNumber = document.questions.firstOrNull { it.number == number }
        val byText = document.questions.filter { question ->
            question !== byNumber && matches(question.question + " " + question.answer, words)
        }
        return (listOfNotNull(byNumber) + byText).map { DoctrineSearchResult.Question(it.number, it.question) }
    }

    private fun searchConfession(
        document: DoctrineDocument.Confession,
        words: List<String>,
    ): List<DoctrineSearchResult> = document.chapters.flatMap { chapter ->
        chapter.paragraphs.mapNotNull { paragraph ->
            val text = buildString {
                append(paragraph.text)
                paragraph.lists.forEach { list ->
                    append(' ').append(list.heading)
                    list.items.forEach { append(' ').append(it) }
                }
                paragraph.textAfter?.let { append(' ').append(it) }
            }
            if (!matches(text, words)) return@mapNotNull null
            DoctrineSearchResult.Paragraph(
                chapter = chapter.number,
                paragraph = paragraph.number,
                preview = paragraph.text.take(PREVIEW_LENGTH),
            )
        }
    }

    private fun matches(text: String, words: List<String>): Boolean {
        val normalized = normalize(text)
        return words.all { normalized.contains(it) }
    }

    companion object {
        private const val PREVIEW_LENGTH = 140
        private val WHITESPACE = Regex("""\s+""")
        private val COMBINING_MARKS = Regex("""\p{Mn}+""")

        fun normalize(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD).replace(COMBINING_MARKS, "").lowercase()
    }
}
