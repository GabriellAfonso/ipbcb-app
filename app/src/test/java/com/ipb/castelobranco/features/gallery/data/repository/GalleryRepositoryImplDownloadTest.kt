package com.ipb.castelobranco.features.gallery.data.repository

import app.cash.turbine.test
import com.ipb.castelobranco.core.domain.download.DownloadProgress
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.tempDirContext
import com.ipb.castelobranco.features.gallery.data.api.FakeGalleryApi
import com.ipb.castelobranco.features.gallery.data.download.GalleryPhotoDownloader
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.data.local.GalleryPhotoStorage
import com.ipb.castelobranco.features.gallery.data.photoUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response

/** Download paths of [GalleryRepositoryImpl], against [FakeGalleryApi] and a real storage. */
class GalleryRepositoryImplDownloadTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var api: FakeGalleryApi
    private lateinit var storage: GalleryPhotoStorage
    private lateinit var repository: GalleryRepositoryImpl

    private val photos = (1L..4L).map { galleryPhoto(it, albumId = 7L) }

    @Before
    fun setup() {
        api = FakeGalleryApi()
        api.photosResponse = Response.success(photos)
        api.albumPhotos = mapOf(7L to photos)
        storage = GalleryPhotoStorage(tempDirContext(folder))
        repository = GalleryRepositoryImpl(api, storage, GalleryPhotoDownloader(api, storage), Dispatchers.Unconfined)
    }

    @Test
    fun `downloadAllPhotos emits progress only for saved photos`() = runTest {
        api.respondError(photoUrl(2), 404)

        repository.downloadAllPhotos().test {
            assertEquals(DownloadProgress(0, 4), awaitItem())
            assertEquals(DownloadProgress(1, 4), awaitItem())
            assertEquals(DownloadProgress(2, 4), awaitItem())
            assertEquals(DownloadProgress(3, 4), awaitItem())
            awaitComplete()
        }
        assertEquals(3, photos.count { storage.exists(it.albumId, it.id) })
    }

    @Test
    fun `downloadAllPhotos throws the rate limit error and keeps the photos saved before it`() = runTest {
        api.respondError(photoUrl(3), 429)

        // flowOn drops buffered progress when upstream fails, so only the terminal error is asserted
        val error = runCatching { repository.downloadAllPhotos().collect { } }.exceptionOrNull()

        assertTrue(error is AppError.Server && error.code == 429)
        assertEquals(listOf(photoUrl(1), photoUrl(2), photoUrl(3)), api.requestedUrls)
        assertEquals(2, photos.count { storage.exists(it.albumId, it.id) })
    }

    @Test
    fun `downloadAlbum follows the same rules`() = runTest {
        api.respondIOException(photoUrl(1))

        repository.downloadAlbum(7L).test {
            assertEquals(DownloadProgress(0, 4), awaitItem())
            assertEquals(DownloadProgress(1, 4), awaitItem())
            assertEquals(DownloadProgress(2, 4), awaitItem())
            assertEquals(DownloadProgress(3, 4), awaitItem())
            awaitComplete()
        }
    }
}
