package com.ipb.castelobranco.features.gallery.domain.upload

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class EnqueueUploadsUseCase @Inject constructor(
    private val repository: GalleryUploadRepository,
) {
    suspend operator fun invoke(albumId: Long, entries: List<UploadEntry>, videosWifiOnly: Boolean = false) =
        repository.enqueue(albumId, entries, videosWifiOnly)

    suspend fun inspect(sources: List<String>): List<PickedMedia> = repository.inspect(sources)
}

class DismissUploadUseCase @Inject constructor(
    private val repository: GalleryUploadRepository,
) {
    suspend operator fun invoke(uploadId: String) = repository.dismiss(uploadId)
}

class ObserveUploadsUseCase @Inject constructor(
    private val repository: GalleryUploadRepository,
) {
    operator fun invoke(): StateFlow<List<UploadItem>> = repository.items
}
