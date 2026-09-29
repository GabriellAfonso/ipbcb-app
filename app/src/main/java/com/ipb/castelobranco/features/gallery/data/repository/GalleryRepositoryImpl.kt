package com.ipb.castelobranco.features.gallery.data.repository

import com.ipb.castelobranco.features.gallery.data.sync.GallerySyncer
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncStatus
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepositoryImpl @Inject constructor(
    private val syncer: GallerySyncer,
) : GalleryRepository {

    override val localState: StateFlow<GalleryLocalState> = syncer.state
    override val syncStatus: StateFlow<GallerySyncStatus> = syncer.status

    override suspend fun preload() = syncer.load()

    override suspend fun sync(): GallerySyncResult = syncer.sync()

    override suspend fun refreshLocalFiles() = syncer.refreshLocalFiles()

    override suspend fun clear() = syncer.clear()
}
