package com.ipb.castelobranco.features.studies.data.api

import com.ipb.castelobranco.core.network.ApiConstants

/** `backend/specs/026-sunday-school/contracts/api.md`. */
object SundaySchoolEndpoints {
    private const val ROOT = "${ApiConstants.BASE_PATH}sunday-school/"

    /** GET {baseUrl}/api/sunday-school/[?year=N] */
    const val INDEX_PATH = ROOT

    /** GET {baseUrl}/api/sunday-school/lessons/{id}/ */
    const val LESSON_PATH = "${ROOT}lessons/{id}/"

    /** GET (?year=N) / POST {baseUrl}/api/sunday-school/manage/lessons/ */
    const val MANAGE_LESSONS_PATH = "${ROOT}manage/lessons/"

    /** GET / PATCH / DELETE {baseUrl}/api/sunday-school/manage/lessons/{id}/ */
    const val MANAGE_LESSON_PATH = "${ROOT}manage/lessons/{id}/"

    /** PUT {baseUrl}/api/sunday-school/manage/lessons/order/ */
    const val MANAGE_ORDER_PATH = "${ROOT}manage/lessons/order/"

    /** GET {baseUrl}/api/sunday-school/manage/years/ */
    const val MANAGE_YEARS_PATH = "${ROOT}manage/years/"

    /** PATCH {baseUrl}/api/sunday-school/manage/years/{year}/ */
    const val MANAGE_YEAR_PATH = "${ROOT}manage/years/{year}/"

    const val QUERY_YEAR = "year"

    const val ERROR_CONTENT_INVALID = "SUNDAY_SCHOOL_CONTENT_INVALID"
    const val ERROR_ORDER_MISMATCH = "SUNDAY_SCHOOL_ORDER_MISMATCH"
    const val ERROR_LESSON_NOT_FOUND = "SUNDAY_SCHOOL_LESSON_NOT_FOUND"
    const val ERROR_YEAR_NOT_FOUND = "SUNDAY_SCHOOL_YEAR_NOT_FOUND"

    /** Extra field of a `SUNDAY_SCHOOL_CONTENT_INVALID` body. */
    const val EXTRA_ERRORS = "errors"

    const val FIELD_YEAR = "year"
    const val FIELD_TITLE = "title"
    const val FIELD_DATE = "date"
    const val FIELD_TEACHER = "teacher"
    const val FIELD_STATUS = "status"
    const val FIELD_CONTENT = "content"
    const val FIELD_LESSON_IDS = "lesson_ids"
    const val FIELD_THEME = "theme"
}
