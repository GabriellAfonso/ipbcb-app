package com.ipb.castelobranco.features.bible.domain.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.bible.domain.model.BiblePlace
import com.ipb.castelobranco.features.bible.domain.model.BibleVideo
import kotlinx.coroutines.flow.StateFlow

/**
 * Videos and playlists linked to Bible places. The list is public and kept on the device (snapshot + ETag), so the
 * reader decides the play icon and opens the sheet offline. Writes need the network and refresh the list after.
 */
interface BibleVideoRepository {
    /** Every video with its places; empty while nothing was ever fetched. */
    val videos: StateFlow<List<BibleVideo>>

    /** Loads the cached list into [videos]. */
    suspend fun preload()

    /** Conditional fetch of the list; failures keep the cached list. */
    suspend fun refresh()

    /** Adds [url] with [place] as its first place. */
    suspend fun addVideo(url: String, place: BiblePlace): AddVideoResult

    /** Replaces every place of the video; an empty list deletes it. */
    suspend fun setPlaces(videoId: Long, places: List<BiblePlace>): Result<Unit>

    /** Removes one place; the server deletes the video when it was the last. */
    suspend fun removePlace(videoId: Long, place: BiblePlace): Result<Unit>

    /** Deletes the video from every place. */
    suspend fun deleteVideo(videoId: Long): Result<Unit>

    /** Sets the order of [place]: [videoIds] must be exactly its videos. */
    suspend fun reorder(place: BiblePlace, videoIds: List<Long>): Result<Unit>
}

sealed interface AddVideoResult {
    data class Added(val video: BibleVideo) : AddVideoResult

    /** The link was registered before. [video] is the existing one when the server sent it. */
    data class AlreadyExists(val video: BibleVideo?) : AddVideoResult

    /** Invalid link, unavailable video, lookup or network failure: [error] carries the message to show. */
    data class Failed(val error: AppError) : AddVideoResult
}
