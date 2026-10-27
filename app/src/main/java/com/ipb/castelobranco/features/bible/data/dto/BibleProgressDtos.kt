package com.ipb.castelobranco.features.bible.data.dto

import com.ipb.castelobranco.features.bible.data.local.QueuedProgressChange
import com.ipb.castelobranco.features.bible.domain.model.BibleProgress
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET api/bible/progress/`: only what is done. Also the on-disk cache of the server state. */
@Serializable
data class BibleProgressDto(
    val chapters: Map<String, List<Int>> = emptyMap(),
    val videos: List<Long> = emptyList(),
)

fun BibleProgressDto.toDomain(): BibleProgress = BibleProgress(
    chapters = chapters
        .mapKeys { it.key.lowercase() }
        .mapValues { it.value.toSet() }
        .filterValues { it.isNotEmpty() },
    videos = videos.toSet(),
)

/**
 * One change as the server reads it. Separate from [QueuedProgressChange] on purpose: the server rejects unknown
 * fields, so nothing local may leak into the body. Absent fields are left out (`explicitNulls = false`).
 */
@Serializable
data class ProgressChangeWireDto(
    val type: String,
    val book: String? = null,
    val chapter: Int? = null,
    @SerialName("video_id") val videoId: Long? = null,
    val done: Boolean,
    @SerialName("changed_at") val changedAt: String,
)

@Serializable
data class ProgressSyncRequestDto(
    val changes: List<ProgressChangeWireDto>,
)

/** Indexes refer to the request's `changes`. */
@Serializable
data class ProgressSyncResponseDto(
    val accepted: List<Int> = emptyList(),
    val rejected: List<RejectedProgressChangeDto> = emptyList(),
)

@Serializable
data class RejectedProgressChangeDto(
    val index: Int,
    val reason: String = "",
)

fun QueuedProgressChange.toWireDto(): ProgressChangeWireDto = ProgressChangeWireDto(
    type = type,
    book = book,
    chapter = chapter,
    videoId = videoId,
    done = done,
    changedAt = changedAt,
)
