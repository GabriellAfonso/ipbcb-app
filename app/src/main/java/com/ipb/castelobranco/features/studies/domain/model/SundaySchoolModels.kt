package com.ipb.castelobranco.features.studies.domain.model

import java.time.LocalDate

/** A Sunday school year; [theme] is blank when the year has none. */
data class SundaySchoolYear(
    val year: Int,
    val theme: String,
) {
    /** "2026 — O Drama das Escrituras", or just "2026". */
    val label: String get() = if (theme.isBlank()) year.toString() else "$year — $theme"
}

/** What the EBD screen shows for one year: the tabs ([years]) and the lessons of [year]. */
data class SundaySchoolIndex(
    val years: List<SundaySchoolYear>,
    val defaultYear: Int?,
    val year: SundaySchoolYear?,
    val lessons: List<LessonSummary>,
)

data class LessonSummary(
    val id: Int,
    val title: String,
    val date: LocalDate?,
    /** Blank when unknown. */
    val teacher: String,
    val position: Int,
    /** Only the panel list carries it; the public list is published-only. */
    val status: LessonStatus = LessonStatus.PUBLISHED,
)

enum class LessonStatus(val wireName: String) {
    DRAFT("draft"),
    PUBLISHED("published");

    companion object {
        fun fromWire(value: String): LessonStatus = entries.firstOrNull { it.wireName == value } ?: DRAFT
    }
}

data class Lesson(
    val id: Int,
    val title: String,
    val date: LocalDate?,
    val teacher: String,
    val position: Int,
    val status: LessonStatus,
    val year: SundaySchoolYear,
    val content: LessonContent,
    /** Facts of each book named by a `book_facts` block, computed by the server. */
    val books: Map<String, BookFacts>,
)

data class BookFacts(
    val name: String,
    /** Number of the book in the Bible, 1-based. */
    val position: Int,
    val chapters: Int,
    val verses: Int,
)

/** A year as the panel lists it. */
data class SundaySchoolYearAdmin(
    val year: Int,
    val theme: String,
    val lessonCount: Int,
    val publishedCount: Int,
)

/** What the lesson form edits and sends. */
data class LessonDraft(
    val title: String,
    val year: Int,
    val date: LocalDate?,
    val teacher: String,
    val status: LessonStatus,
    val content: LessonContent,
) {
    companion object {
        const val MIN_YEAR = 2000
        const val MAX_YEAR = 2100

        fun new(year: Int) = LessonDraft(
            title = "",
            year = year,
            date = null,
            teacher = "",
            status = LessonStatus.DRAFT,
            content = StandardSections.template(),
        )

        fun of(lesson: Lesson) = LessonDraft(
            title = lesson.title,
            year = lesson.year.year,
            date = lesson.date,
            teacher = lesson.teacher,
            status = lesson.status,
            content = lesson.content,
        )
    }
}
