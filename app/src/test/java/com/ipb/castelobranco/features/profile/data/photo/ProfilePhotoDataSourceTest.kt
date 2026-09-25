package com.ipb.castelobranco.features.profile.data.photo

import com.ipb.castelobranco.core.data.local.StorageDirConstants
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.tempDirContext
import com.ipb.castelobranco.features.profile.data.api.FakeProfileApi
import com.ipb.castelobranco.features.profile.data.local.ProfilePhotoCacheStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Every protected-media outcome of [ProfilePhotoDataSource.downloadAndPersist], each starting from a
 * device that already holds a profile photo and its ETag.
 */
class ProfilePhotoDataSourceTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = Dispatchers.Unconfined

    private lateinit var api: FakeProfileApi
    private lateinit var photoCache: ProfilePhotoCacheStorage
    private lateinit var dataSource: ProfilePhotoDataSource
    private lateinit var storedPhoto: File

    @Before
    fun setup() = runBlocking {
        val context = tempDirContext(folder)
        api = FakeProfileApi()
        photoCache = ProfilePhotoCacheStorage(context, dispatcher)
        dataSource = ProfilePhotoDataSource(api, context, BASE_URL, photoCache, dispatcher)

        val dir = File(context.filesDir, StorageDirConstants.PROFILE).apply { mkdirs() }
        storedPhoto = File(dir, "profile_photo.jpg").apply { writeBytes(STORED_BYTES) }
        photoCache.saveLastUrl(PHOTO_URL)
        photoCache.saveETag(STORED_ETAG)
    }

    private fun assertStoredPhotoKept() {
        assertTrue(storedPhoto.exists())
        assertArrayEquals(STORED_BYTES, storedPhoto.readBytes())
    }

    @Test
    fun `200 - replaces the photo and saves the new ETag`() = runTest {
        api.respondBytes(bytes = NEW_BYTES, etag = "\"v2\"")

        val result = dataSource.downloadAndPersist(PHOTO_URL)

        assertArrayEquals(NEW_BYTES, result.getOrThrow()!!.readBytes())
        assertEquals("\"v2\"", photoCache.loadETagOrNull())
    }

    @Test
    fun `stored ETag is sent as If-None-Match`() = runTest {
        api.respondNotModified()

        dataSource.downloadAndPersist(PHOTO_URL)

        assertEquals(STORED_ETAG, api.lastIfNoneMatch)
    }

    @Test
    fun `304 - keeps the stored photo`() = runTest {
        api.respondNotModified()

        val result = dataSource.downloadAndPersist(PHOTO_URL)

        assertEquals(storedPhoto, result.getOrThrow())
        assertStoredPhotoKept()
    }

    @Test
    fun `404 - clears the stored photo and its ETag`() = runTest {
        api.respondError(404)

        val result = dataSource.downloadAndPersist(PHOTO_URL)

        assertNull(result.getOrThrow())
        assertFalse(storedPhoto.exists())
        assertNull(photoCache.loadETagOrNull())
    }

    @Test
    fun `403 - clears the stored photo, ETag and URL without an error`() = runTest {
        api.respondError(403)

        val result = dataSource.downloadAndPersist(PHOTO_URL)

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
        assertFalse(storedPhoto.exists())
        assertNull(photoCache.loadETagOrNull())
        assertNull(photoCache.loadLastUrlOrNull())
        assertNull(dataSource.findLastLocalPhotoOrNull())
    }

    @Test
    fun `429 - keeps the stored photo and its ETag without an error`() = runTest {
        api.respondError(429)

        val result = dataSource.downloadAndPersist(PHOTO_URL)

        assertEquals(storedPhoto, result.getOrThrow())
        assertStoredPhotoKept()
        assertEquals(STORED_ETAG, photoCache.loadETagOrNull())
    }

    @Test
    fun `network error - keeps the stored photo without an error`() = runTest {
        api.respondIOException()

        val result = dataSource.downloadAndPersist(PHOTO_URL)

        assertEquals(storedPhoto, result.getOrThrow())
        assertStoredPhotoKept()
    }

    @Test
    fun `500 - fails with a server error and keeps the stored photo`() = runTest {
        api.respondError(500)

        val result = dataSource.downloadAndPersist(PHOTO_URL)

        val error = result.exceptionOrNull()
        assertTrue(error is AppError.Server && error.code == 500)
        assertStoredPhotoKept()
    }

    @Test
    fun `401 after a failed refresh - fails with an auth error and keeps the stored photo`() = runTest {
        api.respondError(401)

        val result = dataSource.downloadAndPersist(PHOTO_URL)

        val error = result.exceptionOrNull()
        assertTrue(error is AppError.Auth && error.code == 401)
        assertStoredPhotoKept()
        assertEquals(STORED_ETAG, photoCache.loadETagOrNull())
    }

    private companion object {
        const val BASE_URL = "https://example.com/ipbcb/"
        const val PHOTO_URL = "https://example.com/ipbcb/media/profiles/me.jpg"
        const val STORED_ETAG = "\"v1\""
        val STORED_BYTES = "stored-photo".toByteArray()
        val NEW_BYTES = "new-photo".toByteArray()
    }
}
