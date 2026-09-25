package com.ipb.castelobranco.features.gallery.data.download

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.tempDirContext
import com.ipb.castelobranco.features.gallery.data.api.FakeGalleryApi
import com.ipb.castelobranco.features.gallery.data.dto.GalleryPhotoDto
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.data.local.GalleryPhotoStorage
import com.ipb.castelobranco.features.gallery.data.photoUrl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream

class GalleryPhotoDownloaderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var api: FakeGalleryApi
    private lateinit var storage: GalleryPhotoStorage
    private lateinit var downloader: GalleryPhotoDownloader

    private val progress = mutableListOf<Pair<Int, Int>>()

    @Before
    fun setup() {
        api = FakeGalleryApi()
        storage = GalleryPhotoStorage(tempDirContext(folder))
        downloader = GalleryPhotoDownloader(api, storage)
    }

    private fun photos(count: Int, albumId: Long = 1L) = (1L..count).map { galleryPhoto(it, albumId) }

    private suspend fun download(photos: List<GalleryPhotoDto>) =
        downloader.download(photos, onProgress = { done, total -> progress += done to total })

    private fun onDisk(photos: List<GalleryPhotoDto>) =
        photos.count { storage.exists(it.albumId, it.id) }

    // region US1 — never count a photo that is not on disk

    @Test
    fun `all photos succeed - completed with every photo and its metadata on disk`() = runTest {
        val list = photos(5)

        val run = download(list)

        assertEquals(GalleryDownloadRun.Completed(5, 5, failed = 0, networkFailures = 0), run)
        assertEquals(5, onDisk(list))
        list.forEach { assertEquals(it, storage.getPhotoMetadata(it.albumId, it.id)) }
    }

    @Test
    fun `photo already on disk - counted without a request`() = runTest {
        val list = photos(3)
        storage.save(1L, 2L, "jpg", ByteArrayInputStream("existing".toByteArray()))

        val run = download(list)

        assertEquals(3, run.downloaded)
        assertFalse(api.requestedUrls.contains(photoUrl(2)))
    }

    @Test
    fun `404 on one photo - not saved, not counted, next photos still requested`() = runTest {
        val list = photos(4)
        api.respondError(photoUrl(2), 404)

        val run = download(list)

        assertEquals(GalleryDownloadRun.Completed(3, 4, failed = 1, networkFailures = 0), run)
        assertFalse(storage.exists(1L, 2L))
        assertEquals(list.map { it.imageUrl }, api.requestedUrls)
    }

    @Test
    fun `network error on one photo - not saved, not counted, counted as network failure`() = runTest {
        val list = photos(4)
        api.respondIOException(photoUrl(3))

        val run = download(list)

        assertEquals(GalleryDownloadRun.Completed(3, 4, failed = 1, networkFailures = 1), run)
        assertFalse(storage.exists(1L, 3L))
        assertEquals(4, api.requestedUrls.size)
    }

    @Test
    fun `truncated body - photo not on disk and not counted`() = runTest {
        val list = photos(2)
        api.respondTruncated(photoUrl(1))

        val run = download(list)

        assertEquals(GalleryDownloadRun.Completed(1, 2, failed = 1, networkFailures = 1), run)
        assertFalse(storage.exists(1L, 1L))
    }

    @Test
    fun `server error on one photo - not counted, run continues`() = runTest {
        val list = photos(3)
        api.respondError(photoUrl(1), 500)

        val run = download(list)

        assertEquals(GalleryDownloadRun.Completed(2, 3, failed = 1, networkFailures = 0), run)
    }

    @Test
    fun `429 on photo 41 of 100 - stops with 40 downloaded and no later request`() = runTest {
        val list = photos(100)
        api.respondError(photoUrl(41), 429)

        val run = download(list)

        run as GalleryDownloadRun.Stopped
        assertEquals(StopReason.RATE_LIMITED, run.reason)
        assertEquals(40, run.downloaded)
        assertEquals(100, run.total)
        assertEquals(photoUrl(41), api.requestedUrls.last())
        assertEquals(41, api.requestedUrls.size)
        assertEquals(40, onDisk(list))
    }

    @Test
    fun `rerun after a rate limit - requests exactly the missing photos`() = runTest {
        val list = photos(100)
        api.respondError(photoUrl(41), 429)
        download(list)

        val secondApi = FakeGalleryApi()
        val run = GalleryPhotoDownloader(secondApi, storage).download(list, onProgress = { _, _ -> })

        assertEquals(GalleryDownloadRun.Completed(100, 100, failed = 0, networkFailures = 0), run)
        assertEquals((41L..100L).map { photoUrl(it) }, secondApi.requestedUrls)
    }

    @Test
    fun `progress never exceeds the photos on disk and skips failed photos`() = runTest {
        val list = photos(4)
        api.respondError(photoUrl(2), 404)
        api.respondIOException(photoUrl(3))

        download(list)

        assertEquals(listOf(0 to 4, 1 to 4, 2 to 4), progress)
        assertTrue(progress.all { (done, _) -> done <= onDisk(list) })
    }

    @Test
    fun `onAlbumDone runs once per album on a complete run`() = runTest {
        val list = photos(2, albumId = 1L) + listOf(galleryPhoto(10, albumId = 2L))
        var albumsDone = 0

        downloader.download(list, onProgress = { _, _ -> }, onAlbumDone = { albumsDone++ })

        assertEquals(2, albumsDone)
    }

    @Test
    fun `empty list - completed with nothing to do`() = runTest {
        val run = download(emptyList())

        assertEquals(GalleryDownloadRun.Completed(0, 0, failed = 0, networkFailures = 0), run)
        assertTrue(api.requestedUrls.isEmpty())
    }

    // endregion

    // region US2 — forbidden stops the run

    @Test
    fun `403 on the first photo - stops after a single request`() = runTest {
        val list = photos(10)
        api.respondError(photoUrl(1), 403)

        val run = download(list)

        run as GalleryDownloadRun.Stopped
        assertEquals(StopReason.FORBIDDEN, run.reason)
        assertEquals(0, run.downloaded)
        assertEquals(1, api.requestedUrls.size)
    }

    @Test
    fun `403 on photo 5 - photos before it stay on disk`() = runTest {
        val list = photos(10)
        api.respondError(photoUrl(5), 403)

        val run = download(list)

        assertEquals(4, run.downloaded)
        assertEquals(4, onDisk(list))
        assertEquals(5, api.requestedUrls.size)
    }

    @Test
    fun `403 error carries the structured detail as userMessage`() = runTest {
        api.respondError(
            photoUrl(1),
            403,
            body = """{"error_code":"not_member","detail":"Disponível apenas para membros."}""",
        )

        val run = download(photos(1)) as GalleryDownloadRun.Stopped

        val error = run.error
        assertTrue(error is AppError.Auth && error.code == 403)
        assertEquals("Disponível apenas para membros.", error.userMessage)
    }

    // endregion

    // region US4 — expired session stops the run

    @Test
    fun `401 on photo 3 - stops, photos 1 and 2 stay on disk, no later request`() = runTest {
        val list = photos(6)
        api.respondError(photoUrl(3), 401)

        val run = download(list)

        run as GalleryDownloadRun.Stopped
        assertEquals(StopReason.UNAUTHENTICATED, run.reason)
        assertEquals(2, run.downloaded)
        assertEquals(2, onDisk(list))
        assertEquals(photoUrl(3), api.requestedUrls.last())
    }

    // endregion
}
