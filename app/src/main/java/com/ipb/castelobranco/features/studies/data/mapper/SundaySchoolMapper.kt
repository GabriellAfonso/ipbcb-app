package com.ipb.castelobranco.features.studies.data.mapper

import com.ipb.castelobranco.features.studies.data.dto.BookFactsDto
import com.ipb.castelobranco.features.studies.data.dto.LessonDto
import com.ipb.castelobranco.features.studies.data.dto.LessonSummaryDto
import com.ipb.castelobranco.features.studies.data.dto.SundaySchoolIndexDto
import com.ipb.castelobranco.features.studies.data.dto.SundaySchoolYearAdminDto
import com.ipb.castelobranco.features.studies.data.dto.SundaySchoolYearDto
import com.ipb.castelobranco.features.studies.domain.model.BookFacts
import com.ipb.castelobranco.features.studies.domain.model.Lesson
import com.ipb.castelobranco.features.studies.domain.model.LessonStatus
import com.ipb.castelobranco.features.studies.domain.model.LessonSummary
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolIndex
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolYear
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolYearAdmin
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun SundaySchoolIndexDto.toDomain() = SundaySchoolIndex(
    years = years.map { it.toDomain() },
    defaultYear = defaultYear,
    year = year?.toDomain(),
    lessons = lessons.map { it.toDomain() },
)

fun SundaySchoolYearDto.toDomain() = SundaySchoolYear(year = year, theme = theme)

fun LessonSummaryDto.toDomain() = LessonSummary(
    id = id,
    title = title,
    date = date.toLocalDate(),
    teacher = teacher,
    position = position,
    status = status?.let { LessonStatus.fromWire(it) } ?: LessonStatus.PUBLISHED,
)

fun LessonDto.toDomain() = Lesson(
    id = id,
    title = title,
    date = date.toLocalDate(),
    teacher = teacher,
    position = position,
    status = LessonStatus.fromWire(status),
    year = year.toDomain(),
    content = LessonContentCodec.decode(content),
    books = books.mapValues { (_, facts) -> facts.toDomain() },
)

fun LessonDto.toSummary() = LessonSummary(
    id = id,
    title = title,
    date = date.toLocalDate(),
    teacher = teacher,
    position = position,
    status = LessonStatus.fromWire(status),
)

fun SundaySchoolYearAdminDto.toDomain() = SundaySchoolYearAdmin(
    year = year,
    theme = theme,
    lessonCount = lessonCount,
    publishedCount = publishedCount,
)

private fun BookFactsDto.toDomain() = BookFacts(name = name, position = position, chapters = chapters, verses = verses)

private fun String?.toLocalDate(): LocalDate? = this?.let {
    try {
        LocalDate.parse(it)
    } catch (_: DateTimeParseException) {
        null
    }
}
