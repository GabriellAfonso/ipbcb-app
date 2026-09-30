package com.ipb.castelobranco.features.gallery.data.upload

import com.ipb.castelobranco.core.testing.tempDirContext
import com.ipb.castelobranco.features.gallery.data.api.FakeGalleryApi
import com.ipb.castelobranco.features.gallery.data.apiError
import com.ipb.castelobranco.features.gallery.data.dto.PhotoUploadResultDto
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.data.photoDto
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalChange
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncStatus
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import com.ipb.castelobranco.features.gallery.domain.upload.UploadState
import com.ipb.castelobranco.features.gallery.domain.usecase.SyncGalleryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.File

class GalleryUploadRunTest {

    @get:Rule
    val folder = TemporaryFolder()

    /** Records every local change and runs `afterApply` like the syncer does. */
    private class RecordingRepository : GalleryRepository {
        val applied = mutableListOf<GalleryLocalChange>()
        var hasIndex = true
        override val localState: StateFlow<GalleryLocalState> = MutableStateFlow(GalleryLocalState.EMPTY)
        override val syncStatus: StateFlow<GallerySyncStatus> = MutableStateFlow(GallerySyncStatus())
        override suspend fun preload() = Unit
        override suspend fun sync(): GallerySyncResult = GallerySyncResult.Skipped
        override suspend fun syncAfterWrite(): GallerySyncResult = GallerySyncResult.Skipped
        override suspend fun applyLocal(change: GalleryLocalChange, afterApply: suspend () -> Unit): Boolean {
            if (!hasIndex) return false
            applied += change
            afterApply()
            return true
        }
        override suspend fun refreshLocalFiles() = Unit
        override suspend fun clear() = Unit
    }

    /** Every image decodes to a small JPEG. */
    private class JpegCodec : ImageCodec {
        override fun readInfo(file: File) = SourceInfo(3000, 2000, 1, "image/jpeg", file.length(), emptyMap())
        override fun decode(file: File, plan: PreparationPlan.Reencode) = object : DecodedImage {
            override fun writeJpeg(quality: Int, output: File): Long {
                output.writeText("prepared")
                return output.length()
            }
            override fun release() = Unit
        }
        override fun writeExif(file: File, tags: Map<String, String>) = Unit
    }

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeGalleryApi()
    private val repository = RecordingRepository()
    private val syncGallery = mockk<SyncGalleryUseCase> { coEvery { afterWrite() } returns GallerySyncResult.Skipped }
    private var loggedIn = true
    private lateinit var media: GalleryMediaStore
    private lateinit var queue: GalleryUploadQueueStore
    private lateinit var run: GalleryUploadRun
    private val progress = mutableListOf<Pair<Int, Int>>()

    @Before
    fun setup() {
        media = GalleryMediaStore(tempDirContext(folder))
        queue = GalleryUploadQueueStore(GalleryUploadQueueStoreTest.InMemoryQueueSnapshot(), media, dispatcher)
        run = GalleryUploadRun(
            queue = queue,
            preparer = GalleryImagePreparer(JpegCodec(), media),
            api = api,
            classifier = UploadOutcomeClassifier(Json { ignoreUnknownKeys = true }),
            repository = repository,
            syncGallery = syncGallery,
            mediaStore = media,
            session = { loggedIn },
        )
    }

    private suspend fun enqueue(id: String, albumId: Long = 7, time: Long = 0) {
        val fileName = GalleryImagePreparer.rawFileName(id, "heic")
        media.saveUpload(fileName, "raw-$id".byteInputStream())
        queue.append(UploadItem(id, albumId, "$id.HEIC", fileName, UploadState.Waiting, enqueuedAt = time))
    }

    private fun accepted(photoId: Long, albumId: Long = 7) =
        Response.success(201, PhotoUploadResultDto(accepted = listOf(photoDto(photoId, albumId))))

    private suspend fun runOnce() = run.run { current, total -> progress += current to total }

    @Test
    fun `sends each item in order, once, with its own upload id and album`() = runTest(dispatcher) {
        enqueue("a", time = 1)
        enqueue("b", albumId = 8, time = 2)
        api.respond("uploadPhoto") { accepted(41) }
        api.respond("uploadPhoto") { accepted(42, 8) }

        val result = runOnce()

        assertEquals(GalleryUploadRun.Result.Done(failed = 0), result)
        assertEquals(listOf("a", "b"), api.writes.map { it.parts["client_upload_id"] })
        assertEquals(listOf("7", "8"), api.writes.map { it.parts["album_id"] })
        assertEquals(listOf("a.jpg", "b.jpg"), api.writes.map { it.fileName })
        assertEquals(listOf(1 to 2, 2 to 2), progress)
        assertTrue(queue.items.value.isEmpty())
    }

    @Test
    fun `a sent photo is applied locally and its prepared file becomes the original`() = runTest(dispatcher) {
        enqueue("a")
        api.respond("uploadPhoto") { accepted(41) }

        runOnce()

        assertEquals(41L, (repository.applied.single() as GalleryLocalChange.UpsertPhoto).photo.id)
        assertEquals("prepared", media.originalFiles().getValue(41).readText())
        assertFalse(media.uploadFile("a.jpg").exists())
        coVerify(exactly = 1) { syncGallery.afterWrite() }
    }

    @Test
    fun `a repeat of a live photo (dedup) is treated as sent`() = runTest(dispatcher) {
        enqueue("a")
        api.respond("uploadPhoto") { accepted(41, albumId = 9) }

        runOnce()

        assertEquals(9L, (repository.applied.single() as GalleryLocalChange.UpsertPhoto).photo.albumId)
        assertTrue(queue.items.value.isEmpty())
    }

    @Test
    fun `a network failure keeps the item prepared and the next run resends the same id`() = runTest(dispatcher) {
        enqueue("a")
        api.respondWriteIOException("uploadPhoto")

        assertEquals(GalleryUploadRun.Result.Retry, runOnce())
        val kept = queue.items.value.single()
        assertEquals(UploadState.Prepared, kept.state)
        assertEquals("a", kept.uploadId)

        api.respond("uploadPhoto") { accepted(41) }
        runOnce()

        assertEquals(listOf("a", "a"), api.writes.map { it.parts["client_upload_id"] })
        assertTrue(queue.items.value.isEmpty())
    }

    @Test
    fun `a server error is retried`() = runTest(dispatcher) {
        enqueue("a")
        api.respondWriteError("uploadPhoto", 503, "")

        assertEquals(GalleryUploadRun.Result.Retry, runOnce())
        assertEquals(UploadState.Prepared, queue.items.value.single().state)
    }

    @Test
    fun `a rejected file fails with the reason and the queue continues`() = runTest(dispatcher) {
        enqueue("a", time = 1)
        enqueue("b", time = 2)
        api.respondWriteError(
            "uploadPhoto",
            400,
            apiError(
                "VALIDATION_ERROR",
                "Nenhuma imagem foi aceita.",
                "rejected" to """[{"filename":"a.jpg","reason":"Grande."}]""",
            ),
        )
        api.respond("uploadPhoto") { accepted(42) }

        val result = runOnce()

        assertEquals(GalleryUploadRun.Result.Done(failed = 1), result)
        val failed = queue.items.value.single()
        assertEquals("a", failed.uploadId)
        assertEquals("Grande.", failed.failure)
    }

    @Test
    fun `409 is not retried`() = runTest(dispatcher) {
        enqueue("a")
        val detail = "Esta foto já foi enviada e depois apagada; ela está na lixeira."
        api.respondWriteError("uploadPhoto", 409, apiError("CONFLICT", detail))

        assertEquals(GalleryUploadRun.Result.Done(failed = 1), runOnce())
        assertEquals(detail, queue.items.value.single().failure)
        assertEquals(1, api.writes.size)
    }

    @Test
    fun `404 fails every item of that album and carries on with other albums`() = runTest(dispatcher) {
        enqueue("a", albumId = 7, time = 1)
        enqueue("b", albumId = 7, time = 2)
        enqueue("c", albumId = 8, time = 3)
        api.respondWriteError("uploadPhoto", 404, apiError("NOT_FOUND", "x"))
        api.respond("uploadPhoto") { accepted(43, 8) }

        val result = runOnce()

        assertEquals(GalleryUploadRun.Result.Done(failed = 2), result)
        assertEquals(listOf("a", "b"), queue.items.value.map { it.uploadId })
        assertTrue(queue.items.value.all { it.failure == GalleryUploadRun.ALBUM_GONE_MESSAGE })
        assertEquals(2, api.writes.size)
    }

    @Test
    fun `403 fails every remaining item with the server message`() = runTest(dispatcher) {
        enqueue("a", albumId = 7, time = 1)
        enqueue("b", albumId = 8, time = 2)
        api.respondWriteError("uploadPhoto", 403, apiError("PERMISSION_DENIED", "Sem permissão."))

        assertEquals(GalleryUploadRun.Result.Done(failed = 2), runOnce())
        assertTrue(queue.items.value.all { it.failure == "Sem permissão." })
        assertEquals(1, api.writes.size)
    }

    @Test
    fun `401 stops without failing anything`() = runTest(dispatcher) {
        enqueue("a")
        api.respondWriteError("uploadPhoto", 401, "")

        assertEquals(GalleryUploadRun.Result.Stopped, runOnce())
        assertEquals(UploadState.Prepared, queue.items.value.single().state)
    }

    @Test
    fun `no session - nothing is sent or written`() = runTest(dispatcher) {
        enqueue("a")
        loggedIn = false

        assertEquals(GalleryUploadRun.Result.Stopped, runOnce())
        assertTrue(api.writes.isEmpty())
        assertEquals(UploadState.Waiting, queue.items.value.single().state)
        coVerify(exactly = 0) { syncGallery.afterWrite() }
    }

    @Test
    fun `items added during the run are sent in the same run`() = runTest(dispatcher) {
        enqueue("a", time = 1)
        api.respond("uploadPhoto") {
            enqueue("b", time = 2)
            accepted(41)
        }
        api.respond("uploadPhoto") { accepted(42) }

        runOnce()

        assertEquals(listOf("a", "b"), api.writes.map { it.parts["client_upload_id"] })
        coVerify(exactly = 1) { syncGallery.afterWrite() }
    }

    @Test
    fun `no index yet - the prepared file is dropped and the photo comes with the sync`() = runTest(dispatcher) {
        repository.hasIndex = false
        enqueue("a")
        api.respond("uploadPhoto") { accepted(41) }

        runOnce()

        assertTrue(media.originalFiles().isEmpty())
        assertFalse(media.uploadFile("a.jpg").exists())
        assertTrue(queue.items.value.isEmpty())
    }
}
