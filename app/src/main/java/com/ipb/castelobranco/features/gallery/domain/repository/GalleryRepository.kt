package com.ipb.castelobranco.features.gallery.domain.repository

import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalChange
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

    /** A sync that is never skipped: see `GallerySyncer.syncAfterWrite`. */
    suspend fun syncAfterWrite(): GallerySyncResult

    /**
     * Applies a write's result to the local copy without moving the cursor. [afterApply] runs under
     * the same lock once the index lists the change. `false` when nothing was applied.
     */
    suspend fun applyLocal(change: GalleryLocalChange, afterApply: suspend () -> Unit = {}): Boolean
    suspend fun refreshLocalFiles()

    /** Deletes the index, the cursor and every gallery file. */
    suspend fun clear()
}
