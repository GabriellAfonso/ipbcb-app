package com.ipb.castelobranco.features.gallery.data.sync

import com.ipb.castelobranco.core.data.local.StorageDirConstants
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.testing.tempDirContext
import com.ipb.castelobranco.features.gallery.data.albumDto
import com.ipb.castelobranco.features.gallery.data.api.FakeGalleryApi
import com.ipb.castelobranco.features.gallery.data.changes
import com.ipb.castelobranco.features.gallery.data.coverUrl
import com.ipb.castelobranco.features.gallery.data.local.GalleryLegacyMigration
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.data.local.GalleryPreferences
import com.ipb.castelobranco.features.gallery.data.photoDto
import com.ipb.castelobranco.features.gallery.data.photoUrl
import com.ipb.castelobranco.features.gallery.data.snapshot.GalleryIndexSnapshot
import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalChange
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class GallerySyncerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private class InMemorySnapshotCache : SnapshotCache<GalleryIndexSnapshot> {
        var value: GalleryIndexSnapshot? = null
        var saves = 0
        override suspend fun load() = value
        override suspend fun save(dto: GalleryIndexSnapshot, etag: String?) {
            value = dto
            saves++
        }
        override suspend fun loadETag(): String? = null
        override suspend fun clear() {
            value = null
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private lateinit var api: FakeGalleryApi
    private lateinit var snapshot: InMemorySnapshotCache
    private lateinit var media: GalleryMediaStore
    private lateinit var galleryDir: File
    private var layoutVersion = GalleryLegacyMigration.FLAT_LAYOUT_VERSION
    private var loggedIn = true
    private lateinit var syncer: GallerySyncer

    @Before
    fun setup() {
        val context = tempDirContext(folder)
        galleryDir = File(context.filesDir, StorageDirConstants.GALLERY)
        api = FakeGalleryApi()
        snapshot = InMemorySnapshotCache()
        media = GalleryMediaStore(context)
        val preferences = mockk<GalleryPreferences> {
            coEvery { layoutVersion() } answers { layoutVersion }
            coEvery { setLayoutVersion(any()) } answers { layoutVersion = firstArg() }
        }
        syncer = GallerySyncer(
            api = api,
            snapshotCache = snapshot,
            mediaStore = media,
            migration = GalleryLegacyMigration(context, preferences),
            session = { loggedIn },
            ioDispatcher = dispatcher,
        )
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    private fun original(id: Long) = media.saveOriginal(id, "jpg", ByteArrayInputStream("o$id".toByteArray()))

    private suspend fun firstSync(vararg photoIds: Long, albums: List<Long> = listOf(1L)) {
        api.enqueueChanges(
            changes(
                cursor = "c1",
                albums = albums.map { albumDto(it) },
                photos = photoIds.map { photoDto(it, albumId = albums.first()) },
            )
        )
        syncer.sync()
    }

    // region core

    @Test
    fun `no index - full read, index saved with its cursor`() = test {
        api.enqueueChanges(changes("c1", albums = listOf(albumDto(1)), photos = listOf(photoDto(10))))

        val result = syncer.sync()

        assertEquals(listOf<String?>(null), api.sinceReceived)
        assertEquals(GallerySyncResult.Synced(missingOriginals = 1), result)
        assertEquals("c1", snapshot.value?.cursor)
        assertEquals(setOf(10L), syncer.state.value.index?.photos?.keys)
    }

    @Test
    fun `with an index - reads with the cursor and applies the delta`() = test {
        firstSync(10, 11)
        api.enqueueChanges(changes("c2", photos = listOf(photoDto(12)), deletedPhotoIds = listOf(11)))

        syncer.sync()

        assertEquals(listOf(null, "c1"), api.sinceReceived)
        assertEquals(setOf(10L, 12L), syncer.state.value.index?.photos?.keys)
        assertEquals("c2", snapshot.value?.cursor)
    }

    @Test
    fun `duplicates in the feed are applied idempotently`() = test {
        firstSync(10)
        api.enqueueChanges(changes("c2", photos = listOf(photoDto(10, position = 3))))
        api.enqueueChanges(changes("c3", photos = listOf(photoDto(10, position = 3))))

        syncer.sync()
        val once = syncer.state.value.index?.photos
        syncer.sync()

        assertEquals(once, syncer.state.value.index?.photos)
    }

    @Test
    fun `missing originals are counted`() = test {
        original(10)
        api.enqueueChanges(changes("c1", albums = listOf(albumDto(1)), photos = listOf(photoDto(10), photoDto(11))))

        assertEquals(GallerySyncResult.Synced(missingOriginals = 1), syncer.sync())
    }

    @Test
    fun `403 on the feed keeps the index and the files`() = test {
        firstSync(10)
        original(10)
        api.enqueueChangesError(403)

        val result = syncer.sync()

        assertTrue(result is GallerySyncResult.Failed && (result.error as AppError.Auth).code == 403)
        assertEquals(setOf(10L), syncer.state.value.index?.photos?.keys)
        assertEquals("c1", snapshot.value?.cursor)
        assertTrue(media.hasOriginal(10, "jpg"))
        assertEquals(403, (syncer.status.value.lastError as AppError.Auth).code)
    }

    @Test
    fun `401 and network errors keep the local copy`() = test {
        firstSync(10)
        api.enqueueChangesError(401)
        api.enqueueChangesIOException()

        val unauthorized = syncer.sync()
        val offline = syncer.sync()

        assertTrue((unauthorized as GallerySyncResult.Failed).error is AppError.Auth)
        assertTrue((offline as GallerySyncResult.Failed).error is AppError.Network)
        assertEquals(1, snapshot.saves)
    }

    @Test
    fun `a sync requested while another runs is skipped`() = test {
        val gate = CompletableDeferred<Unit>()
        api.enqueueChangesAfter(gate, changes("c1"))

        val first = async { syncer.sync() }
        runCurrent()
        val second = syncer.sync()
        gate.complete(Unit)

        assertEquals(GallerySyncResult.Skipped, second)
        assertTrue(first.await() is GallerySyncResult.Synced)
        assertEquals(1, api.sinceReceived.size)
    }

    @Test
    fun `no session - nothing is requested`() = test {
        loggedIn = false

        assertEquals(GallerySyncResult.Skipped, syncer.sync())
        assertTrue(api.sinceReceived.isEmpty())
    }

    // endregion

    // region reconcile and full resync

    @Test
    fun `deleted photo - its original is deleted`() = test {
        firstSync(10, 11)
        original(10)
        original(11)
        api.enqueueChanges(changes("c2", deletedPhotoIds = listOf(11)))

        syncer.sync()

        assertTrue(media.hasOriginal(10, "jpg"))
        assertFalse(media.hasOriginal(11, "jpg"))
    }

    @Test
    fun `deleted album subtree - albums, originals and unused covers are gone`() = test {
        api.enqueueChanges(
            changes(
                "c1",
                albums = listOf(
                    albumDto(1, coverUrl = coverUrl("a")),
                    albumDto(2, parentId = 1, coverUrl = coverUrl("b")),
                ),
                photos = listOf(photoDto(10, albumId = 1), photoDto(20, albumId = 2)),
            )
        )
        syncer.sync()
        original(10)
        original(20)
        api.enqueueChanges(changes("c2", deletedAlbumIds = listOf(1, 2), deletedPhotoIds = listOf(10, 20)))

        syncer.sync()

        assertTrue(syncer.state.value.index!!.albums.isEmpty())
        assertTrue(media.originalFiles().isEmpty())
        assertTrue(media.coverFiles().isEmpty())
    }

    @Test
    fun `photo moved between albums keeps its file and nothing is downloaded`() = test {
        firstSync(10, albums = listOf(1L, 2L))
        val file = original(10)
        val requestsBefore = api.requestedUrls.size
        api.enqueueChanges(changes("c2", photos = listOf(photoDto(10, albumId = 2))))

        val result = syncer.sync()

        assertEquals(GallerySyncResult.Synced(missingOriginals = 0), result)
        assertEquals(2L, syncer.state.value.index!!.photos.getValue(10).albumId)
        assertEquals("o10", file.readText())
        assertEquals(requestsBefore, api.requestedUrls.size)
        assertFalse(photoUrl(10) in api.requestedUrls)
    }

    @Test
    fun `full sync required - reads again without cursor and replaces the index`() = test {
        firstSync(10, 11)
        original(10)
        original(11)
        api.enqueueChanges(changes("stale", fullSyncRequired = true))
        api.enqueueChanges(changes("c9", albums = listOf(albumDto(1)), photos = listOf(photoDto(11))))

        syncer.sync()

        assertEquals(listOf(null, "c1", null), api.sinceReceived)
        assertEquals(setOf(11L), syncer.state.value.index!!.photos.keys)
        assertEquals("c9", snapshot.value?.cursor)
        assertFalse(media.hasOriginal(10, "jpg"))
        assertTrue(media.hasOriginal(11, "jpg"))
    }

    // endregion

    // region covers

    @Test
    fun `covers are downloaded during sync, once per url`() = test {
        api.enqueueChanges(
            changes(
                "c1",
                albums = listOf(
                    albumDto(1, coverUrl = coverUrl("shared")),
                    albumDto(2, parentId = 1, coverUrl = coverUrl("shared")),
                ),
            )
        )

        syncer.sync()

        assertEquals(listOf(coverUrl("shared")), api.requestedUrls)
        assertEquals(1, media.coverFiles().size)
        assertEquals(setOf(coverUrl("shared")), syncer.state.value.covers.keys)

        api.enqueueChanges(changes("c2", albums = listOf(albumDto(1, coverUrl = coverUrl("shared"), name = "X"))))
        syncer.sync()
        assertEquals(1, api.requestedUrls.size)
    }

    @Test
    fun `cover download failure is skipped and the sync still succeeds`() = test {
        api.respondError(coverUrl("a"), 404)
        api.respondIOException(coverUrl("b"))
        api.enqueueChanges(
            changes("c1", albums = listOf(albumDto(1, coverUrl = coverUrl("a")), albumDto(2, coverUrl = coverUrl("b"))))
        )

        val result = syncer.sync()

        assertTrue(result is GallerySyncResult.Synced)
        assertTrue(media.coverFiles().isEmpty())
        assertTrue(syncer.state.value.covers.isEmpty())
    }

    @Test
    fun `replaced cover - new file downloaded, old one deleted`() = test {
        api.enqueueChanges(changes("c1", albums = listOf(albumDto(1, coverUrl = coverUrl("old")))))
        syncer.sync()
        val oldFile = media.coverFile(coverUrl("old"))
        api.enqueueChanges(changes("c2", albums = listOf(albumDto(1, coverUrl = coverUrl("new")))))

        syncer.sync()

        assertFalse(oldFile.exists())
        assertTrue(media.coverFile(coverUrl("new")).exists())
    }

    @Test
    fun `cover removed - its file is deleted`() = test {
        api.enqueueChanges(changes("c1", albums = listOf(albumDto(1, coverUrl = coverUrl("a")))))
        syncer.sync()
        api.enqueueChanges(changes("c2", albums = listOf(albumDto(1, coverUrl = null))))

        syncer.sync()

        assertTrue(media.coverFiles().isEmpty())
    }

    @Test
    fun `shared cover stays while another album still uses it`() = test {
        api.enqueueChanges(
            changes(
                "c1",
                albums = listOf(albumDto(1, coverUrl = coverUrl("s")), albumDto(2, coverUrl = coverUrl("s"))),
            )
        )
        syncer.sync()
        api.enqueueChanges(changes("c2", deletedAlbumIds = listOf(2)))

        syncer.sync()

        assertTrue(media.coverFile(coverUrl("s")).exists())
    }

    // endregion

    // region logout

    @Test
    fun `clear deletes the index, the cursor and every file`() = test {
        api.enqueueChanges(
            changes("c1", albums = listOf(albumDto(1, coverUrl = coverUrl("a"))), photos = listOf(photoDto(10)))
        )
        syncer.sync()
        original(10)

        syncer.clear()

        assertNull(snapshot.value)
        assertFalse(galleryDir.exists())
        assertNull(syncer.state.value.index)
    }

    @Test
    fun `clear during a sync in flight cancels it and nothing is written`() = test {
        val gate = CompletableDeferred<Unit>()
        api.enqueueChangesAfter(gate, changes("c1", albums = listOf(albumDto(1)), photos = listOf(photoDto(10))))

        val running = async { syncer.sync() }
        runCurrent()
        val clearing = async { syncer.clear() }
        runCurrent()
        gate.complete(Unit)
        advanceUntilIdle()
        clearing.await()

        assertEquals(GallerySyncResult.Skipped, running.await())
        assertNull(snapshot.value)
        assertNull(syncer.state.value.index)
    }

    @Test
    fun `sign-out while the feed answers - nothing is saved`() = test {
        val gate = CompletableDeferred<Unit>()
        api.enqueueChangesAfter(gate, changes("c1", albums = listOf(albumDto(1))))

        val running = async { syncer.sync() }
        runCurrent()
        loggedIn = false
        gate.complete(Unit)

        assertEquals(GallerySyncResult.Skipped, running.await())
        assertNull(snapshot.value)
    }

    // endregion

    // region migration

    @Test
    fun `migration then first full sync - orphans deleted, others kept, nothing downloaded`() = test {
        layoutVersion = 0
        File(galleryDir, "3").mkdirs()
        File(galleryDir, "3/10.jpg").writeText("o10")
        File(galleryDir, "3/10.json").writeText("{}")
        File(galleryDir, "3/11.jpg").writeText("o11")
        api.enqueueChanges(changes("c1", albums = listOf(albumDto(3)), photos = listOf(photoDto(10, albumId = 3))))

        val result = syncer.sync()

        assertEquals(GallerySyncResult.Synced(missingOriginals = 0), result)
        assertEquals("o10", File(galleryDir, "photos/10.jpg").readText())
        assertFalse(File(galleryDir, "photos/11.jpg").exists())
        assertFalse(File(galleryDir, "3").exists())
        assertTrue(api.requestedUrls.isEmpty())
        assertEquals(GalleryLegacyMigration.FLAT_LAYOUT_VERSION, layoutVersion)
    }

    // endregion

    // region writes

    @Test
    fun `applyLocal saves the change with the previous cursor and publishes it`() = test {
        firstSync(10)

        val applied = syncer.applyLocal(GalleryLocalChange.UpsertAlbum(galleryAlbum(2, name = "Novo")))

        assertTrue(applied)
        assertEquals("c1", snapshot.value?.cursor)
        assertEquals("Novo", syncer.state.value.index?.albums?.get(2L)?.name)
        assertTrue(snapshot.value!!.albums.any { it.id == 2L })
    }

    @Test
    fun `applyLocal waits for a running sync and applies after it`() = test {
        firstSync(10)
        val gate = CompletableDeferred<Unit>()
        api.enqueueChangesAfter(gate, changes("c2", photos = listOf(photoDto(11))))

        val running = async { syncer.sync() }
        runCurrent()
        val apply = async { syncer.applyLocal(GalleryLocalChange.UpsertAlbum(galleryAlbum(2))) }
        runCurrent()
        assertFalse(apply.isCompleted)

        gate.complete(Unit)
        running.await()
        assertTrue(apply.await())

        // The sync's photo and the write's album both survive: the write saw the synced index.
        val index = syncer.state.value.index!!
        assertTrue(11L in index.photos)
        assertTrue(2L in index.albums)
        assertEquals("c2", index.cursor)
    }

    @Test
    fun `applyLocal of an album tree deletes the originals of its photos`() = test {
        api.enqueueChanges(
            changes(
                "c1",
                albums = listOf(albumDto(1), albumDto(2, parentId = 1), albumDto(3)),
                photos = listOf(photoDto(10, albumId = 2), photoDto(11, albumId = 3)),
            )
        )
        syncer.sync()
        original(10)
        original(11)

        syncer.applyLocal(GalleryLocalChange.RemoveAlbumTree(1))

        assertFalse(File(galleryDir, "photos/10.jpg").exists())
        assertTrue(File(galleryDir, "photos/11.jpg").exists())
        assertEquals(setOf(3L), syncer.state.value.index?.albums?.keys)
    }

    @Test
    fun `applyLocal runs afterApply before pruning, so a file the index now lists is kept`() = test {
        firstSync(10)

        syncer.applyLocal(GalleryLocalChange.UpsertPhoto(galleryPhoto(12))) { original(12) }

        assertTrue(File(galleryDir, "photos/12.jpg").exists())
        assertTrue(12L in syncer.state.value.originals)
    }

    @Test
    fun `applyLocal without an index or a session applies nothing`() = test {
        assertFalse(syncer.applyLocal(GalleryLocalChange.UpsertAlbum(galleryAlbum(2))))

        firstSync(10)
        loggedIn = false
        assertFalse(syncer.applyLocal(GalleryLocalChange.UpsertAlbum(galleryAlbum(2))))
        assertFalse(2L in syncer.state.value.index!!.albums)
    }

    @Test
    fun `syncAfterWrite while a sync runs makes it run exactly once more`() = test {
        firstSync(10)
        val gate = CompletableDeferred<Unit>()
        api.enqueueChangesAfter(gate, changes("c2"))
        api.enqueueChanges(changes("c3", photos = listOf(photoDto(12))))

        val running = async { syncer.sync() }
        runCurrent()
        assertEquals(GallerySyncResult.Skipped, syncer.syncAfterWrite())
        assertEquals(GallerySyncResult.Skipped, syncer.syncAfterWrite())
        gate.complete(Unit)
        running.await()

        assertEquals(listOf(null, "c1", "c2"), api.sinceReceived)
        assertEquals("c3", syncer.state.value.index?.cursor)
    }

    @Test
    fun `syncAfterWrite with nothing running syncs at once`() = test {
        firstSync(10)
        api.enqueueChanges(changes("c2"))

        assertTrue(syncer.syncAfterWrite() is GallerySyncResult.Synced)
        assertEquals(listOf(null, "c1"), api.sinceReceived)
    }

    // endregion
}
