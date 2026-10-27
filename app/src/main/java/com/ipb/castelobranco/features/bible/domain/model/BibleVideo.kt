package com.ipb.castelobranco.features.bible.domain.model

enum class BibleVideoKind { VIDEO, PLAYLIST }

/**
 * Where a video shows: a chapter of a book, or the whole book when [chapter] is null. [book] is the lowercase
 * abbrev ("gn"), shared by every translation. Build it with [of] so two places of the same book always compare equal.
 */
data class BiblePlace private constructor(
    val book: String,
    val chapter: Int?,
) {
    val isWholeBook: Boolean get() = chapter == null

    companion object {
        fun of(book: String, chapter: Int?): BiblePlace = BiblePlace(book.trim().lowercase(), chapter)
    }
}

/** A place of one video, with the video's [position] inside that place's list (0 first). */
data class BibleVideoPlace(
    val place: BiblePlace,
    val position: Int,
)

/** A YouTube video or playlist linked to Bible places. Title and thumbnail come from YouTube, read by the server. */
data class BibleVideo(
    val id: Long,
    val kind: BibleVideoKind,
    val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val places: List<BibleVideoPlace>,
) {
    fun positionIn(place: BiblePlace): Int? = places.firstOrNull { it.place == place }?.position

    fun isIn(place: BiblePlace): Boolean = places.any { it.place == place }
}

/**
 * What the videos sheet shows for one chapter: the chapter's videos and the whole book's, each in its place's order.
 * A video in both shows only in [chapter].
 */
data class BibleVideoSections(
    val chapter: List<BibleVideo>,
    val book: List<BibleVideo>,
) {
    val isEmpty: Boolean get() = chapter.isEmpty() && book.isEmpty()

    companion object {
        val EMPTY = BibleVideoSections(emptyList(), emptyList())
    }
}

fun List<BibleVideo>.sectionsFor(book: String, chapter: Int): BibleVideoSections {
    val chapterPlace = BiblePlace.of(book, chapter)
    val bookPlace = BiblePlace.of(book, null)
    val inChapter = inPlace(chapterPlace)
    val chapterIds = inChapter.mapTo(mutableSetOf()) { it.id }
    val inBook = inPlace(bookPlace).filterNot { it.id in chapterIds }
    return BibleVideoSections(chapter = inChapter, book = inBook)
}

/** The videos of [place] in its order; ties (should not happen) by id so the list is stable. */
fun List<BibleVideo>.inPlace(place: BiblePlace): List<BibleVideo> =
    mapNotNull { video -> video.positionIn(place)?.let { video to it } }
        .sortedWith(compareBy({ it.second }, { it.first.id }))
        .map { it.first }

/** "Gênesis 1" or "Gênesis (livro inteiro)", with the book's name from the index (the abbrev when unknown). */
fun BiblePlace.label(books: List<BibleBook>): String {
    val name = books.firstOrNull { it.abbrev.equals(book, ignoreCase = true) }?.name ?: book
    return if (chapter == null) "$name (livro inteiro)" else "$name $chapter"
}
