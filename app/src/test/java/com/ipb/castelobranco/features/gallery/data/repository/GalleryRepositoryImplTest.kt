package com.ipb.castelobranco.features.gallery.data.repository

import com.ipb.castelobranco.features.gallery.data.api.GalleryApi
import com.ipb.castelobranco.features.gallery.data.download.GalleryPhotoDownloader
import com.ipb.castelobranco.features.gallery.data.local.GalleryPhotoStorage
import com.ipb.castelobranco.features.gallery.domain.model.Album
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryRepositoryImplTest {

    private lateinit var api: GalleryApi
    private lateinit var storage: GalleryPhotoStorage
    private lateinit var repository: GalleryRepositoryImpl

    @Before
    fun setup() {
        api = mockk()
        storage = mockk(relaxed = true)
        repository = GalleryRepositoryImpl(api, storage, GalleryPhotoDownloader(api, storage), Dispatchers.Unconfined)
    }

    // region preload

    @Test
    fun `preload populates albumsFlow from storage`() = runTest {
        every { storage.listAlbums() } returns listOf(1L to "Conferência", 2L to "Culto")
        every { storage.getThumbnailFile(any()) } returns null

        repository.preload()

        assertEquals(
            listOf(Album(1L, "Conferência"), Album(2L, "Culto")),
            repository.albumsFlow.value
        )
    }

    @Test
    fun `preload populates thumbnailsFlow from storage`() = runTest {
        val thumb = mockk<File>()
        every { storage.listAlbums() } returns listOf(1L to "Culto")
        every { storage.getThumbnailFile(1L) } returns thumb

        repository.preload()

        assertEquals(mapOf(1L to thumb), repository.thumbnailsFlow.value)
    }

    @Test
    fun `preload with empty storage results in empty albumsFlow`() = runTest {
        every { storage.listAlbums() } returns emptyList()

        repository.preload()

        assertTrue(repository.albumsFlow.value.isEmpty())
    }

    @Test
    fun `preload stores null thumbnail when storage returns null`() = runTest {
        every { storage.listAlbums() } returns listOf(5L to "Batismos")
        every { storage.getThumbnailFile(5L) } returns null

        repository.preload()

        assertNull(repository.thumbnailsFlow.value[5L])
    }

    // endregion

    // region getLocalPhotos

    @Test
    fun `getLocalPhotos fetches from storage on first call`() = runTest {
        val photos = listOf(mockk<File>())
        every { storage.listPhotos(1L) } returns photos

        val result = repository.getLocalPhotos(1L)

        assertEquals(photos, result)
    }

    @Test
    fun `getLocalPhotos returns cached result on second call without hitting storage again`() = runTest {
        val photos = listOf(mockk<File>())
        every { storage.listPhotos(1L) } returns photos

        repository.getLocalPhotos(1L) // first call populates cache
        repository.getLocalPhotos(1L) // second call should use cache

        verify(exactly = 1) { storage.listPhotos(1L) }
    }

    @Test
    fun `getLocalPhotos returns empty list when storage has no photos`() = runTest {
        every { storage.listPhotos(99L) } returns emptyList()

        val result = repository.getLocalPhotos(99L)

        assertTrue(result.isEmpty())
    }

    // endregion

    // region clearAllPhotos

    @Test
    fun `clearAllPhotos delegates to storage clearAll`() = runTest {
        every { storage.listAlbums() } returns emptyList()

        repository.clearAllPhotos()

        verify { storage.clearAll() }
    }

    @Test
    fun `clearAllPhotos reloads albums after clearing`() = runTest {
        val albums = listOf(1L to "Conferência")
        every { storage.listAlbums() } returns albums
        every { storage.getThumbnailFile(any()) } returns null

        repository.clearAllPhotos()

        // preload() is called after clearAll, so albumsFlow is repopulated
        assertEquals(listOf(Album(1L, "Conferência")), repository.albumsFlow.value)
    }

    // endregion

    // region clearAlbum

    @Test
    fun `clearAlbum delegates to storage clearAlbum`() = runTest {
        repository.clearAlbum(3L)
        verify { storage.clearAlbum(3L) }
    }

    // endregion

    // region getPhotoName

    @Test
    fun `getPhotoName delegates to storage`() = runTest {
        every { storage.getPhotoName(1L, 42L) } returns "Batismo"

        val result = repository.getPhotoName(1L, 42L)

        assertEquals("Batismo", result)
    }

    @Test
    fun `getPhotoName returns null when storage returns null`() = runTest {
        every { storage.getPhotoName(1L, 99L) } returns null

        val result = repository.getPhotoName(1L, 99L)

        assertNull(result)
    }

    // endregion
}
