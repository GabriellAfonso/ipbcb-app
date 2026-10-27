package com.ipb.castelobranco.features.bible.data.dto

import com.ipb.castelobranco.features.bible.domain.model.BiblePlace
import com.ipb.castelobranco.features.bible.domain.model.BibleVideo
import com.ipb.castelobranco.features.bible.domain.model.BibleVideoKind
import com.ipb.castelobranco.features.bible.domain.model.BibleVideoPlace
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One video of `GET api/bible/videos/` (also the body of a created video and of a `409 BIBLE_VIDEO_EXISTS`). */
@Serializable
data class BibleVideoDto(
    val id: Long,
    val kind: String = KIND_VIDEO,
    @SerialName("youtube_id") val youtubeId: String = "",
    val url: String,
    val title: String = "",
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val places: List<BibleVideoPlaceDto> = emptyList(),
)

/** `chapter == null` = the whole book. */
@Serializable
data class BibleVideoPlaceDto(
    val book: String,
    val chapter: Int? = null,
    val position: Int = 0,
)

private const val KIND_VIDEO = "video"
private const val KIND_PLAYLIST = "playlist"

fun BibleVideoDto.toDomain(): BibleVideo = BibleVideo(
    id = id,
    kind = if (kind == KIND_PLAYLIST) BibleVideoKind.PLAYLIST else BibleVideoKind.VIDEO,
    url = url,
    title = title,
    thumbnailUrl = thumbnailUrl?.ifBlank { null },
    places = places.map { BibleVideoPlace(BiblePlace.of(it.book, it.chapter), it.position) },
)

fun List<BibleVideoDto>.toDomain(): List<BibleVideo> = map { it.toDomain() }
