package com.ipb.castelobranco.features.studies.data.mapper

import com.ipb.castelobranco.features.studies.data.dto.BibleReferenceDto
import com.ipb.castelobranco.features.studies.data.dto.CatechismQuestionDto
import com.ipb.castelobranco.features.studies.data.dto.ConfessionChapterDto
import com.ipb.castelobranco.features.studies.data.dto.ConfessionParagraphDto
import com.ipb.castelobranco.features.studies.data.dto.DoctrineDocumentDto
import com.ipb.castelobranco.features.studies.data.dto.DoctrineListDto
import com.ipb.castelobranco.features.studies.data.dto.DoctrineSummaryDto
import com.ipb.castelobranco.features.studies.data.dto.ProofTextDto
import com.ipb.castelobranco.features.studies.domain.model.BibleReference
import com.ipb.castelobranco.features.studies.domain.model.CatechismQuestion
import com.ipb.castelobranco.features.studies.domain.model.ConfessionChapter
import com.ipb.castelobranco.features.studies.domain.model.ConfessionParagraph
import com.ipb.castelobranco.features.studies.domain.model.DoctrineDocument
import com.ipb.castelobranco.features.studies.domain.model.DoctrineKind
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSummary
import com.ipb.castelobranco.features.studies.domain.model.ParagraphList
import com.ipb.castelobranco.features.studies.domain.model.ProofText
import com.ipb.castelobranco.features.studies.domain.model.VerseSpan
import com.ipb.castelobranco.features.studies.domain.model.doctrineShortTitle
import com.ipb.castelobranco.features.studies.domain.model.toSentenceCase

/** Server order kept; documents of a kind the app cannot render are dropped. */
fun DoctrineListDto.toDomain(): List<DoctrineSummary> = documents.mapNotNull { it.toDomain() }

fun DoctrineSummaryDto.toDomain(): DoctrineSummary? {
    val kind = DoctrineKind.fromWire(kind) ?: return null
    return DoctrineSummary(
        slug = slug,
        title = title,
        shortTitle = doctrineShortTitle(slug, title),
        kind = kind,
        itemCount = itemCount,
    )
}

/** `null` when the document's kind is unknown. */
fun DoctrineDocumentDto.toDomain(): DoctrineDocument? = when (DoctrineKind.fromWire(kind)) {
    DoctrineKind.CATECHISM -> DoctrineDocument.Catechism(
        slug = slug,
        title = title,
        questions = questions.orEmpty().sortedBy { it.number }.map { it.toDomain() },
    )
    DoctrineKind.CONFESSION -> DoctrineDocument.Confession(
        slug = slug,
        title = title,
        chapters = chapters.orEmpty().sortedBy { it.number }.map { it.toDomain() },
    )
    null -> null
}

private fun CatechismQuestionDto.toDomain() = CatechismQuestion(
    number = number,
    question = question,
    answer = answer,
    proofTexts = proofTexts.map { it.toDomain() },
)

private fun ProofTextDto.toDomain() = ProofText(printed = printed, reference = reference?.toDomain())

private fun BibleReferenceDto.toDomain() = BibleReference(
    book = book,
    chapter = chapter,
    verses = verses.map { VerseSpan(it.start, it.end) },
)

private fun ConfessionChapterDto.toDomain() = ConfessionChapter(
    number = number,
    title = title.toSentenceCase(),
    paragraphs = paragraphs.sortedBy { it.number }.map { it.toDomain() },
)

private fun ConfessionParagraphDto.toDomain() = ConfessionParagraph(
    number = number,
    text = text,
    lists = lists.orEmpty().map { ParagraphList(heading = it.heading.toSentenceCase(), items = it.items) },
    textAfter = textAfter,
)
