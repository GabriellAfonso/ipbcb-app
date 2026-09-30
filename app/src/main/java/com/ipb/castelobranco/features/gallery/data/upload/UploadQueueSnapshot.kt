package com.ipb.castelobranco.features.gallery.data.upload

import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import com.ipb.castelobranco.features.gallery.domain.upload.UploadState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The upload queue on disk (snapshot `gallery_upload_queue`). */
@Serializable
data class UploadQueueSnapshot(
    val items: List<UploadItemDto> = emptyList(),
)

@Serializable
data class UploadItemDto(
    @SerialName("upload_id")
    val uploadId: String,
    @SerialName("album_id")
    val albumId: Long,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("file_name")
    val fileName: String,
    val state: String,
    val failure: String? = null,
    @SerialName("enqueued_at")
    val enqueuedAt: Long,
)

private const val STATE_WAITING = "waiting"
private const val STATE_PREPARED = "prepared"
private const val STATE_FAILED = "failed"

fun UploadItem.toDto() = UploadItemDto(
    uploadId = uploadId,
    albumId = albumId,
    displayName = displayName,
    fileName = fileName,
    state = when (state) {
        UploadState.Waiting -> STATE_WAITING
        UploadState.Prepared -> STATE_PREPARED
        UploadState.Failed -> STATE_FAILED
    },
    failure = failure,
    enqueuedAt = enqueuedAt,
)

/** `null` for a state this version does not know. */
fun UploadItemDto.toDomain(): UploadItem? {
    val state = when (state) {
        STATE_WAITING -> UploadState.Waiting
        STATE_PREPARED -> UploadState.Prepared
        STATE_FAILED -> UploadState.Failed
        else -> return null
    }
    return UploadItem(uploadId, albumId, displayName, fileName, state, failure, enqueuedAt)
}
