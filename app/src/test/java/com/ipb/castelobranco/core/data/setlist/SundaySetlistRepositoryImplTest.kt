package com.ipb.castelobranco.core.data.setlist

import com.ipb.castelobranco.core.data.local.JsonSnapshotStorage
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.errorResponse
import com.ipb.castelobranco.core.testing.setlistOf
import com.ipb.castelobranco.core.testing.tempDirContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

class SundaySetlistRepositoryImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    private class FakeSetlistApi : SetlistApi {
        var next: () -> Response<CurrentSetlistDto> = { Response.success(CurrentSetlistDto(null)) }
        override suspend fun getCurrent(): Response<CurrentSetlistDto> = next()
    }

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val api = FakeSetlistApi()
    private lateinit var storage: JsonSnapshotStorage

    private val setlist = setlistOf(LocalDate.of(2026, 10, 4), 12, 55)

    @Before
    fun setUp() {
        storage = JsonSnapshotStorage(tempDirContext(folder), Dispatchers.Unconfined)
    }

    private fun repository() = SundaySetlistRepositoryImpl(api, storage, json)

    @Test
    fun `a current setlist is stored and survives a new process`() = runTest {
        api.next = { Response.success(CurrentSetlistDto(setlist.toDto())) }
        val repository = repository()

        assertTrue(repository.refreshCurrent().isSuccess)
        assertEquals(setlist, repository.stored.value)

        val reopened = repository()
        reopened.preload()
        assertEquals(setlist, reopened.stored.value)
    }

    @Test
    fun `no current setlist clears the stored copy`() = runTest {
        val repository = repository()
        repository.store(setlist)

        api.next = { Response.success(CurrentSetlistDto(null)) }
        repository.refreshCurrent()

        assertNull(repository.stored.value)
        assertNull(storage.loadOrNull("sunday_setlist"))
    }

    @Test
    fun `a 403 clears and fails`() = runTest {
        val repository = repository()
        repository.store(setlist)

        api.next = { errorResponse(403) }
        val result = repository.refreshCurrent()

        assertTrue(result.exceptionOrNull() is AppError.Auth)
        assertNull(repository.stored.value)
    }

    @Test
    fun `a network failure keeps the stored copy`() = runTest {
        val repository = repository()
        repository.store(setlist)

        api.next = { throw IOException("offline") }
        val result = repository.refreshCurrent()

        assertTrue(result.exceptionOrNull() is AppError.Network)
        assertEquals(setlist, repository.stored.value)
    }

    @Test
    fun `items are kept in position order`() = runTest {
        val shuffled = setlist.toDto().let { it.copy(items = it.items.reversed()) }
        api.next = { Response.success(CurrentSetlistDto(shuffled)) }
        val repository = repository()

        repository.refreshCurrent()

        assertEquals(listOf(1, 2), repository.stored.value?.items?.map { it.position })
    }

    @Test
    fun `clear empties memory and disk`() = runTest {
        val repository = repository()
        repository.store(setlist)

        repository.clear()

        assertNull(repository.stored.value)
        val reopened = repository()
        reopened.preload()
        assertNull(reopened.stored.value)
    }
}
