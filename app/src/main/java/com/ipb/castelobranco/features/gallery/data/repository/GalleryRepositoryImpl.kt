package com.ipb.castelobranco.features.gallery.data.repository

import com.ipb.castelobranco.features.gallery.data.api.GalleryApi
import com.ipb.castelobranco.features.gallery.data.dto.GalleryPhotoDto
import com.ipb.castelobranco.features.gallery.data.download.GalleryDownloadRun
import com.ipb.castelobranco.features.gallery.data.download.GalleryPhotoDownloader
import com.ipb.castelobranco.features.gallery.data.local.GalleryPhotoStorage
import com.ipb.castelobranco.features.gallery.domain.model.Album
import com.ipb.castelobranco.core.domain.download.DownloadProgress
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.network.error.toAppError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.update

@Singleton
class GalleryRepositoryImpl @Inject constructor(
    private val api: GalleryApi,
    private val storage: GalleryPhotoStorage,
    private val downloader: GalleryPhotoDownloader,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GalleryRepository {

    private val _albumsFlow = MutableStateFlow<List<Album>>(emptyList())
    override val albumsFlow: StateFlow<List<Album>> = _albumsFlow.asStateFlow()

    private val _thumbnailsFlow = MutableStateFlow<Map<Long, File?>>(emptyMap())
    override val thumbnailsFlow: StateFlow<Map<Long, File?>> = _thumbnailsFlow.asStateFlow()

    // Cache de fotos: Map de AlbumId para Lista de Arquivos
    private val _photosFlow = MutableStateFlow<Map<Long, List<File>>>(emptyMap())
    override val photosFlow: StateFlow<Map<Long, List<File>>> = _photosFlow.asStateFlow()

    override suspend fun preload() = withContext(ioDispatcher) {
        val rawAlbums = storage.listAlbums()
        _albumsFlow.value = rawAlbums.map { Album(it.first, it.second) }
        _thumbnailsFlow.value = rawAlbums.associate { (id, _) ->
            id to storage.getThumbnailFile(id)
        }
        // Não carregamos as fotos aqui para manter o preload leve
    }

    override suspend fun getLocalPhotos(albumId: Long): List<File> = withContext(ioDispatcher) {
        // Se já estiver no cache, retorna direto
        _photosFlow.value[albumId]?.let { return@withContext it }

        // Se não, busca no storage e salva no cache
        val photos = storage.listPhotos(albumId)
        _photosFlow.update { it + (albumId to photos) }
        photos
    }

    override fun downloadAlbum(albumId: Long): Flow<DownloadProgress> = flow {
        val photos = api.getAlbumPhotos(albumId)
        emitAll(processDownload(photos))
        // Limpa o cache desse álbum para forçar recarga após download
        _photosFlow.update { it - albumId }
    }

    override fun downloadAllPhotos(): Flow<DownloadProgress> = flow {
        val response = api.getAllPhotos()

        if (!response.isSuccessful) throw response.toAppError()

        val photos = response.body() ?: emptyList()
        emitAll(processDownload(photos))
        _photosFlow.value = emptyMap()
        preload()
    }

    private fun processDownload(photos: List<GalleryPhotoDto>): Flow<DownloadProgress> = flow {
        val run = downloader.download(
            photos = photos,
            onProgress = { downloaded, total -> emit(DownloadProgress(downloaded, total)) },
        )
        if (run is GalleryDownloadRun.Stopped) throw run.error
    }.flowOn(ioDispatcher)

    override suspend fun clearAlbum(albumId: Long) = withContext(ioDispatcher) {
        storage.clearAlbum(albumId)
        _photosFlow.update { it - albumId } // Remove do cache
    }

    override suspend fun clearAllPhotos() = withContext(ioDispatcher) {
        storage.clearAll()
        _photosFlow.value = emptyMap() // Limpa todo o cache
        preload()
    }

    // Métodos delegados (mantidos)
    override suspend fun getAllLocalPhotos(): List<File> = withContext(ioDispatcher) { storage.listAllPhotos() }
    override suspend fun getLocalAlbums(): List<Album> = withContext(ioDispatcher) {
        storage.listAlbums().map { Album(it.first, it.second) }
    }

    override suspend fun getThumbnailForAlbum(albumId: Long): File? = withContext(ioDispatcher) {
        storage.getThumbnailFile(albumId)
    }

    override suspend fun getPhotoName(albumId: Long, photoId: Long): String? = withContext(ioDispatcher) {
        storage.getPhotoName(albumId, photoId)
    }
}