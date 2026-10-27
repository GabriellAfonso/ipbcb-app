package com.ipb.castelobranco.features.bible.data.local

import com.ipb.castelobranco.features.bible.domain.model.ProgressChange
import com.ipb.castelobranco.features.bible.domain.model.ProgressTarget
import kotlinx.serialization.Serializable

/**
 * On-disk form of a pending progress change: a state (done or not), not an event. Kept apart from the wire DTO so a
 * local-only field can never reach the request body. [changedAt] is ISO-8601 with offset.
 */
@Serializable
data class QueuedProgressChange(
    val type: String,
    val book: String? = null,
    val chapter: Int? = null,
    val videoId: Long? = null,
    val done: Boolean,
    val changedAt: String,
) {
    /** Null for an item this version cannot read (unknown type, missing field): it is dropped. */
    fun toDomain(): ProgressChange? {
        val target = when (type) {
            TYPE_CHAPTER -> if (book != null && chapter != null) ProgressTarget.chapter(book, chapter) else null
            TYPE_VIDEO -> videoId?.let { ProgressTarget.Video(it) }
            else -> null
        } ?: return null
        return ProgressChange(target, done, changedAt)
    }

    companion object {
        const val TYPE_CHAPTER = "chapter"
        const val TYPE_VIDEO = "video"
    }
}

fun ProgressChange.toQueued(): QueuedProgressChange = when (val target = target) {
    is ProgressTarget.Chapter -> QueuedProgressChange(
        type = QueuedProgressChange.TYPE_CHAPTER,
        book = target.book,
        chapter = target.chapter,
        done = done,
        changedAt = changedAt,
    )
    is ProgressTarget.Video -> QueuedProgressChange(
        type = QueuedProgressChange.TYPE_VIDEO,
        videoId = target.id,
        done = done,
        changedAt = changedAt,
    )
}
