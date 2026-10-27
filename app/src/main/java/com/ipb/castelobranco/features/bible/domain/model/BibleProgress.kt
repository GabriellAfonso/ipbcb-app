package com.ipb.castelobranco.features.bible.domain.model

/**
 * What the signed-in account has read and watched. Only what is done is kept; a book is read when every chapter is
 * (derived, never stored). Books are keyed by lowercase abbrev.
 */
data class BibleProgress(
    val chapters: Map<String, Set<Int>>,
    val videos: Set<Long>,
) {
    fun isRead(book: String, chapter: Int): Boolean = chapter in chapters[book.lowercase()].orEmpty()

    fun readCount(book: String): Int = chapters[book.lowercase()]?.size ?: 0

    fun isBookRead(book: String, chapterCount: Int): Boolean =
        chapterCount > 0 && (1..chapterCount).all { isRead(book, it) }

    fun isWatched(videoId: Long): Boolean = videoId in videos

    /** This state with [changes] applied in order: what the screen shows while they wait to be sent. */
    fun applying(changes: List<ProgressChange>): BibleProgress {
        if (changes.isEmpty()) return this
        val chapters = chapters.mapValuesTo(mutableMapOf()) { it.value.toMutableSet() }
        val videos = videos.toMutableSet()
        changes.forEach { change ->
            when (val target = change.target) {
                is ProgressTarget.Chapter -> {
                    val read = chapters.getOrPut(target.book) { mutableSetOf() }
                    if (change.done) read += target.chapter else read -= target.chapter
                }
                is ProgressTarget.Video -> if (change.done) videos += target.id else videos -= target.id
            }
        }
        return BibleProgress(chapters.filterValues { it.isNotEmpty() }, videos)
    }

    companion object {
        val EMPTY = BibleProgress(emptyMap(), emptySet())
    }
}

/** What a progress change is about. Build chapters with [chapter] so the book is always lowercase. */
sealed interface ProgressTarget {
    data class Chapter(val book: String, val chapter: Int) : ProgressTarget
    data class Video(val id: Long) : ProgressTarget

    companion object {
        fun chapter(book: String, chapter: Int): Chapter = Chapter(book.trim().lowercase(), chapter)
    }
}

/**
 * A state the user set: [target] is (or is no longer) done. [changedAt] is when the user did it, ISO-8601 with
 * offset; the server keeps the latest per target by that time, so a stale offline change does not win.
 */
data class ProgressChange(
    val target: ProgressTarget,
    val done: Boolean,
    val changedAt: String,
)
