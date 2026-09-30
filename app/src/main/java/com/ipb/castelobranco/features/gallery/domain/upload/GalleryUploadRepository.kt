package com.ipb.castelobranco.features.gallery.domain.upload

import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** The persistent upload queue. Member content: cleared at sign-out. */
interface GalleryUploadRepository {
    val items: StateFlow<List<UploadItem>>

    /**
     * Copies each picked image ([sources] are content URIs as strings) into app storage, queues it
     * and starts the queue. Returns once every copy is done.
     */
    suspend fun enqueue(albumId: Long, sources: List<String>)

    /** Removes a failed item and its file. */
    suspend fun dismiss(uploadId: String)

    /** One picked image copied and prepared like an upload, for a cover. The caller deletes the file. */
    suspend fun prepareCover(source: String): Result<File>

    suspend fun clear()
}
