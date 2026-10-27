package com.ipb.castelobranco.core.presentation.navigation

import java.net.URLEncoder
import java.time.LocalDate

object AppRoutes {
    const val CORE = "core"
    const val SCHEDULE = "schedule"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val LOG_VIEWER = "log_viewer"
    const val BIRTHDAYS = "birthdays"

    // An event opened from the home carousel (`specs/events/spec.md` §3).
    const val ARG_EVENT_ID = "eventId"
    const val EVENT_DETAIL = "events/{$ARG_EVENT_ID}"

    fun eventDetail(eventId: Int): String = "events/$eventId"

    // "Registrar domingo" (`specs/worshiphub/spec.md` §2.6). Registered in worshipHubGraph; administration opens it
    // too, pre-filled, for a pending Sunday. Optional `YYYY-MM-DD` date.
    const val ARG_SUNDAY_DATE = "date"
    private const val SUNDAY_REGISTER_PATH = "sunday_register"
    const val SUNDAY_REGISTER = "$SUNDAY_REGISTER_PATH?$ARG_SUNDAY_DATE={$ARG_SUNDAY_DATE}"

    fun sundayRegister(date: LocalDate? = null): String =
        if (date == null) SUNDAY_REGISTER_PATH else "$SUNDAY_REGISTER_PATH?$ARG_SUNDAY_DATE=$date"

    // Nested graph parent routes
    const val AUTH_GRAPH        = "graph/auth"
    const val ADMIN_GRAPH       = "graph/admin"
    const val WORSHIP_HUB_GRAPH = "graph/worship_hub"
    const val GALLERY_GRAPH     = "graph/gallery"
    const val HYMNAL_GRAPH      = "graph/hymnal"
    const val BIBLE_GRAPH       = "graph/bible"
    const val STUDIES_GRAPH     = "graph/studies"
    const val SETTINGS_GRAPH    = "graph/settings"

    // Bible reader opened at a passage from another feature (doctrine proof texts). Registered in bibleGraph.
    const val BIBLE_PASSAGE_BOOK    = "book"
    const val BIBLE_PASSAGE_CHAPTER = "chapter"
    const val BIBLE_PASSAGE_VERSE   = "verse"
    const val BIBLE_PASSAGE_VERSES  = "verses"
    const val BIBLE_PASSAGE =
        "bible_passage?$BIBLE_PASSAGE_BOOK={$BIBLE_PASSAGE_BOOK}" +
            "&$BIBLE_PASSAGE_CHAPTER={$BIBLE_PASSAGE_CHAPTER}&$BIBLE_PASSAGE_VERSE={$BIBLE_PASSAGE_VERSE}" +
            "&$BIBLE_PASSAGE_VERSES={$BIBLE_PASSAGE_VERSES}"

    private const val VERSE_RANGE_SEPARATOR = ","
    private const val VERSE_RANGE_DASH = "-"

    /** [book] is the Bible `abbrev` (e.g. "1Tn"); [highlight] are the cited verses, highlighted in the reader. */
    fun biblePassage(book: String, chapter: Int, verse: Int, highlight: List<IntRange> = emptyList()): String =
        "bible_passage?$BIBLE_PASSAGE_BOOK=${URLEncoder.encode(book, Charsets.UTF_8.name())}" +
            "&$BIBLE_PASSAGE_CHAPTER=$chapter&$BIBLE_PASSAGE_VERSE=$verse" +
            "&$BIBLE_PASSAGE_VERSES=${encodeVerseRanges(highlight)}"

    /** `[3..5, 7..7]` → `"3-5,7"`. */
    fun encodeVerseRanges(ranges: List<IntRange>): String =
        ranges.joinToString(VERSE_RANGE_SEPARATOR) { range ->
            if (range.first == range.last) "${range.first}" else "${range.first}$VERSE_RANGE_DASH${range.last}"
        }

    /** Inverse of [encodeVerseRanges]; malformed or non-positive parts are dropped. */
    fun parseVerseRanges(value: String?): Set<Int> =
        value.orEmpty()
            .split(VERSE_RANGE_SEPARATOR)
            .flatMap { part ->
                val bounds = part.trim().split(VERSE_RANGE_DASH).map { it.trim().toIntOrNull() }
                val start = bounds.firstOrNull() ?: return@flatMap emptyList()
                val end = when (bounds.size) {
                    1 -> start
                    2 -> bounds[1] ?: return@flatMap emptyList()
                    else -> return@flatMap emptyList()
                }
                if (start < 1 || end < start) emptyList() else (start..end).toList()
            }
            .toSet()
}
