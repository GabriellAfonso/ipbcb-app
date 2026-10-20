package com.ipb.castelobranco.core.presentation.navigation

import java.net.URLEncoder

object AppRoutes {
    const val CORE = "core"
    const val SCHEDULE = "schedule"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val LOG_VIEWER = "log_viewer"
    const val BIRTHDAYS = "birthdays"

    // Nested graph parent routes
    const val AUTH_GRAPH        = "graph/auth"
    const val ADMIN_GRAPH       = "graph/admin"
    const val WORSHIP_HUB_GRAPH = "graph/worship_hub"
    const val GALLERY_GRAPH     = "graph/gallery"
    const val HYMNAL_GRAPH      = "graph/hymnal"
    const val BIBLE_GRAPH       = "graph/bible"
    const val STUDIES_GRAPH     = "graph/studies"

    // Bible reader opened at a passage from another feature (doctrine proof texts). Registered in bibleGraph.
    const val BIBLE_PASSAGE_BOOK    = "book"
    const val BIBLE_PASSAGE_CHAPTER = "chapter"
    const val BIBLE_PASSAGE_VERSE   = "verse"
    const val BIBLE_PASSAGE =
        "bible_passage?$BIBLE_PASSAGE_BOOK={$BIBLE_PASSAGE_BOOK}" +
            "&$BIBLE_PASSAGE_CHAPTER={$BIBLE_PASSAGE_CHAPTER}&$BIBLE_PASSAGE_VERSE={$BIBLE_PASSAGE_VERSE}"

    /** [book] is the Bible `abbrev` (e.g. "1Tn"). */
    fun biblePassage(book: String, chapter: Int, verse: Int): String =
        "bible_passage?$BIBLE_PASSAGE_BOOK=${URLEncoder.encode(book, Charsets.UTF_8.name())}" +
            "&$BIBLE_PASSAGE_CHAPTER=$chapter&$BIBLE_PASSAGE_VERSE=$verse"
}
