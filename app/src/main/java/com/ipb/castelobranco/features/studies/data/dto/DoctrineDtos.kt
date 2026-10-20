package com.ipb.castelobranco.features.studies.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET api/doctrine/` — also the on-device snapshot of the list. */
@Serializable
data class DoctrineListDto(
    val documents: List<DoctrineSummaryDto> = emptyList(),
)

@Serializable
data class DoctrineSummaryDto(
    val slug: String,
    val title: String,
    val kind: String,
    @SerialName("item_count") val itemCount: Int,
)

/**
 * `GET api/doctrine/{slug}/` for both kinds: a catechism fills [questions], a confession fills
 * [chapters]. Nullable fields instead of a polymorphic serializer, so an unknown `kind` still
 * decodes and the mapper can drop it.
 */
@Serializable
data class DoctrineDocumentDto(
    val slug: String,
    val title: String,
    val kind: String,
    val questions: List<CatechismQuestionDto>? = null,
    val chapters: List<ConfessionChapterDto>? = null,
)

@Serializable
data class CatechismQuestionDto(
    val number: Int,
    val question: String,
    val answer: String,
    @SerialName("proof_texts") val proofTexts: List<ProofTextDto> = emptyList(),
)

@Serializable
data class ProofTextDto(
    val printed: String,
    val reference: BibleReferenceDto? = null,
)

@Serializable
data class BibleReferenceDto(
    val book: String,
    val chapter: Int,
    val verses: List<VerseSpanDto> = emptyList(),
)

@Serializable
data class VerseSpanDto(
    val start: Int,
    val end: Int,
)

@Serializable
data class ConfessionChapterDto(
    val number: Int,
    val title: String,
    val paragraphs: List<ConfessionParagraphDto> = emptyList(),
)

@Serializable
data class ConfessionParagraphDto(
    val number: Int,
    val text: String,
    val lists: List<ParagraphListDto>? = null,
    @SerialName("text_after") val textAfter: String? = null,
)

@Serializable
data class ParagraphListDto(
    val heading: String,
    val items: List<String> = emptyList(),
)
