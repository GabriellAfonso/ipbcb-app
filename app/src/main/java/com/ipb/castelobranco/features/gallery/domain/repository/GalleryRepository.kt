package com.ipb.castelobranco.features.gallery.domain.repository

import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncStatus
import kotlinx.coroutines.flow.StateFlow

interface GalleryRepository {
    val localState: StateFlow<GalleryLocalState>
    val syncStatus: StateFlow<GallerySyncStatus>

    /** Reads the index and files from disk (migrating the legacy layout first). Idempotent. */
    suspend fun preload()
    suspend fun sync(): GallerySyncResult
    suspend fun refreshLocalFiles()

    /** Deletes the index, the cursor and every gallery file. */
    suspend fun clear()
}
