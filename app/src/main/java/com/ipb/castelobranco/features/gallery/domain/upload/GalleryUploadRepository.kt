package com.ipb.castelobranco.features.gallery.domain.upload

import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** The persistent upload queue. Member content: cleared at sign-out. */
interface GalleryUploadRepository {
    val items: StateFlow<List<UploadItem>>

    /** The type and size of each picked file ([sources] are content URIs as strings), before copying. */
    suspend fun inspect(sources: List<String>): List<PickedMedia>

    /**
     * Copies each picked image or video into app storage, queues it and starts the queue. A video is
     * checked and gets its poster here: a refused one enters the queue as failed, with the reason.
     * [videosWifiOnly] holds the batch's videos until WiFi. Returns once every copy is done.
     */
    suspend fun enqueue(albumId: Long, sources: List<String>, videosWifiOnly: Boolean = false)

    /** Removes a failed item and its file. */
    suspend fun dismiss(uploadId: String)

    /** One picked image copied and prepared like an upload, for a cover. The caller deletes the file. */
    suspend fun prepareCover(source: String): Result<File>

    suspend fun clear()
}
