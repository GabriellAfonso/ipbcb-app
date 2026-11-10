package com.ipb.castelobranco.features.gallery.domain.upload

import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** The persistent upload queue. Member content: cleared at sign-out. */
interface GalleryUploadRepository {
    val items: StateFlow<List<UploadItem>>

    /**
     * The type and size of each picked file ([sources] are content URIs as strings), and whether a video
     * may open the editor. Nothing is copied.
     */
    suspend fun inspect(sources: List<String>): List<PickedMedia>

    /**
     * Queues [entries] in order and starts the queue. A picked file is copied into app storage first; a
     * cut is already there. A video is checked and gets its poster here: a refused one enters the queue
     * as failed, with the reason. [videosWifiOnly] holds the batch's videos until WiFi. Returns once
     * every entry is queued.
     */
    suspend fun enqueue(albumId: Long, entries: List<UploadEntry>, videosWifiOnly: Boolean = false)

    /** Removes a failed item and its file. */
    suspend fun dismiss(uploadId: String)

    /** Deletes cut files no queue item points to: left by a process killed mid-cut. At app start only. */
    suspend fun sweepOrphanCuts()

    /** One picked image copied and prepared like an upload, for a cover. The caller deletes the file. */
    suspend fun prepareCover(source: String): Result<File>

    suspend fun clear()
}
