package com.ipb.castelobranco.features.gallery.data.upload

import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.testing.tempDirContext
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import com.ipb.castelobranco.features.gallery.domain.upload.UploadState
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GalleryUploadQueueStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    class InMemoryQueueSnapshot : SnapshotCache<UploadQueueSnapshot> {
        var value: UploadQueueSnapshot? = null
        override suspend fun load() = value
        override suspend fun save(dto: UploadQueueSnapshot, etag: String?) {
            value = dto
        }
        override suspend fun loadETag(): String? = null
        override suspend fun clear() {
            value = null
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private val snapshot = InMemoryQueueSnapshot()
    private lateinit var media: GalleryMediaStore
    private lateinit var store: GalleryUploadQueueStore

    @Before
    fun setup() {
        media = GalleryMediaStore(tempDirContext(folder))
        store = GalleryUploadQueueStore(snapshot, media, dispatcher)
    }

    private fun item(id: String, albumId: Long = 7, state: UploadState = UploadState.Waiting): UploadItem {
        media.saveUpload("$id.jpg", "x".byteInputStream())
        return UploadItem(id, albumId, "$id.jpg", "$id.jpg", state, enqueuedAt = id.hashCode().toLong())
    }

    @Test
    fun `append persists and publishes, in order`() = runTest(dispatcher) {
        store.append(item("a"))
        store.append(item("b"))

        assertEquals(listOf("a", "b"), store.items.value.map { it.uploadId })
        assertEquals(listOf("a", "b"), snapshot.value?.items?.map { it.uploadId })
        assertEquals("a", store.nextPending()?.uploadId)
    }

    @Test
    fun `markPrepared keeps the upload id and switches the file`() = runTest(dispatcher) {
        store.append(item("a"))

        store.markPrepared("a", "a.prepared.jpg", "IMG.jpg")

        val stored = store.items.value.single()
        assertEquals("a", stored.uploadId)
        assertEquals(UploadState.Prepared, stored.state)
        assertEquals("a.prepared.jpg", stored.fileName)
    }

    @Test
    fun `failed items keep their file and are skipped by nextPending`() = runTest(dispatcher) {
        store.append(item("a"))
        store.append(item("b"))

        store.markFailed("a", "Formato não suportado.")

        assertEquals("Formato não suportado.", store.items.value.first().failure)
        assertTrue(media.uploadFile("a.jpg").exists())
        assertEquals("b", store.nextPending()?.uploadId)
        assertEquals(1, store.pendingCount())
    }

    @Test
    fun `dismiss removes the item and its file`() = runTest(dispatcher) {
        store.append(item("a"))
        store.markFailed("a", "x")

        store.dismiss("a")

        assertTrue(store.items.value.isEmpty())
        assertFalse(media.uploadFile("a.jpg").exists())
    }

    @Test
    fun `failAllOfAlbum fails only that album's pending items`() = runTest(dispatcher) {
        store.append(item("a", albumId = 7))
        store.append(item("b", albumId = 8))
        store.append(item("c", albumId = 7))
        store.markFailed("c", "antes")

        val count = store.failAllOfAlbum(7, "O álbum foi apagado")

        assertEquals(1, count)
        assertEquals("O álbum foi apagado", store.items.value.first { it.uploadId == "a" }.failure)
        assertEquals(UploadState.Waiting, store.items.value.first { it.uploadId == "b" }.state)
        assertEquals("antes", store.items.value.first { it.uploadId == "c" }.failure)
    }

    @Test
    fun `failAllPending fails every pending item`() = runTest(dispatcher) {
        store.append(item("a", albumId = 7))
        store.append(item("b", albumId = 8))

        assertEquals(2, store.failAllPending("Sem permissão."))
        assertNull(store.nextPending())
    }

    @Test
    fun `clear empties the list and the snapshot`() = runTest(dispatcher) {
        store.append(item("a"))

        store.clear()

        assertTrue(store.items.value.isEmpty())
        assertNull(snapshot.value)
    }

    @Test
    fun `a new store reads the same items back, dropping unknown states with their files`() = runTest(dispatcher) {
        store.append(item("a"))
        store.append(item("b"))
        snapshot.value = snapshot.value!!.copy(
            items = snapshot.value!!.items.map { if (it.uploadId == "b") it.copy(state = "sending") else it }
        )

        val reloaded = GalleryUploadQueueStore(snapshot, media, dispatcher)
        reloaded.load()

        assertEquals(listOf("a"), reloaded.items.value.map { it.uploadId })
        assertFalse(media.uploadFile("b.jpg").exists())
    }
}
