package com.ipb.castelobranco.features.gallery.data.upload

import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import com.ipb.castelobranco.features.gallery.domain.upload.UploadKind
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
    /** `photo` or `video`; absent in queues written before videos. */
    val kind: String = KIND_PHOTO,
    @SerialName("poster_file")
    val posterFile: String? = null,
    @SerialName("duration_ms")
    val durationMs: Long? = null,
    @SerialName("date_taken")
    val dateTaken: String? = null,
    @SerialName("wifi_only")
    val wifiOnly: Boolean = false,
)

private const val STATE_WAITING = "waiting"
private const val STATE_PREPARED = "prepared"
private const val STATE_FAILED = "failed"
private const val KIND_PHOTO = "photo"
private const val KIND_VIDEO = "video"

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
    kind = if (kind == UploadKind.VIDEO) KIND_VIDEO else KIND_PHOTO,
    posterFile = posterFileName,
    durationMs = durationMs,
    dateTaken = dateTaken,
    wifiOnly = wifiOnly,
)

/** `null` for a state this version does not know. */
fun UploadItemDto.toDomain(): UploadItem? {
    val state = when (state) {
        STATE_WAITING -> UploadState.Waiting
        STATE_PREPARED -> UploadState.Prepared
        STATE_FAILED -> UploadState.Failed
        else -> return null
    }
    return UploadItem(
        uploadId = uploadId,
        albumId = albumId,
        displayName = displayName,
        fileName = fileName,
        state = state,
        failure = failure,
        enqueuedAt = enqueuedAt,
        kind = if (kind == KIND_VIDEO) UploadKind.VIDEO else UploadKind.PHOTO,
        posterFileName = posterFile,
        durationMs = durationMs,
        dateTaken = dateTaken,
        wifiOnly = wifiOnly,
    )
}
