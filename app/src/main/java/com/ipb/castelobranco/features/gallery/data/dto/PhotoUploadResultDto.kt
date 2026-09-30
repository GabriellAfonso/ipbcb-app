package com.ipb.castelobranco.features.gallery.data.dto

import kotlinx.serialization.Serializable

/** Answer of `POST api/photos/` (201 and 207); a 400 carries [rejected] in the error body's extras. */
@Serializable
data class PhotoUploadResultDto(
    val accepted: List<GalleryPhotoDto> = emptyList(),
    val rejected: List<RejectedFileDto> = emptyList(),
)

/** [reason] is Portuguese, written for the user. */
@Serializable
data class RejectedFileDto(
    val filename: String = "",
    val reason: String = "",
)
