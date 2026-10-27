package com.ipb.castelobranco.features.studies.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** `GET api/sunday-school/[?year=N]` — also the on-device snapshot of a year. */
@Serializable
data class SundaySchoolIndexDto(
    val years: List<SundaySchoolYearDto> = emptyList(),
    @SerialName("default_year") val defaultYear: Int? = null,
    val year: SundaySchoolYearDto? = null,
    val lessons: List<LessonSummaryDto> = emptyList(),
)

@Serializable
data class SundaySchoolYearDto(
    val year: Int,
    val theme: String = "",
)

@Serializable
data class LessonSummaryDto(
    val id: Int,
    val title: String,
    val date: String? = null,
    val teacher: String = "",
    val position: Int = 0,
    val status: String? = null,
)

/**
 * `GET api/sunday-school/lessons/{id}/` and every panel lesson. [content] stays raw JSON here: the codec reads it
 * block by block so an unknown block type never fails the whole lesson.
 */
@Serializable
data class LessonDto(
    val id: Int,
    val title: String,
    val date: String? = null,
    val teacher: String = "",
    val position: Int = 0,
    val status: String = "",
    val year: SundaySchoolYearDto,
    val content: JsonObject = JsonObject(emptyMap()),
    val books: Map<String, BookFactsDto> = emptyMap(),
)

@Serializable
data class BookFactsDto(
    val name: String,
    val position: Int,
    val chapters: Int,
    val verses: Int,
)

/** `GET api/sunday-school/manage/years/`. */
@Serializable
data class SundaySchoolYearAdminDto(
    val year: Int,
    val theme: String = "",
    @SerialName("lesson_count") val lessonCount: Int = 0,
    @SerialName("published_count") val publishedCount: Int = 0,
)

/** `SUNDAY_SCHOOL_CONTENT_INVALID` adds `errors` to the error body. */
@Serializable
data class ContentErrorDto(
    val path: String = "",
    val message: String = "",
)
