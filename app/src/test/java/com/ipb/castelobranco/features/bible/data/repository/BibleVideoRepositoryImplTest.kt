package com.ipb.castelobranco.features.bible.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.bible.data.api.BibleVideosManageApi
import com.ipb.castelobranco.features.bible.data.dto.BibleVideoDto
import com.ipb.castelobranco.features.bible.data.dto.BibleVideoPlaceDto
import com.ipb.castelobranco.features.bible.domain.model.BiblePlace
import com.ipb.castelobranco.features.bible.domain.repository.AddVideoResult
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class BibleVideoRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val dispatcher = StandardTestDispatcher()
    private val cache = FakeCache()
    private val fetcher = FakeFetcher()
    private val api = FakeApi()
    private val repository = BibleVideoRepositoryImpl(cache, fetcher, api, json, dispatcher)

    private val gn1 = BiblePlace.of("gn", 1)
    private val dto = BibleVideoDto(
        id = 42,
        kind = "playlist",
        youtubeId = "PL1",
        url = "https://www.youtube.com/playlist?list=PL1",
        title = "Gênesis",
        places = listOf(BibleVideoPlaceDto("gn", 1, 0), BibleVideoPlaceDto("ex", null, 3)),
    )

    @Test
    fun `refresh stores the list and preload reads it back`() = runTest(dispatcher) {
        fetcher.result = NetworkResult.Success(listOf(dto), "etag-1")

        repository.refresh()

        val video = repository.videos.value.single()
        assertTrue(video.isIn(BiblePlace.of("ex", null)))
        assertEquals("etag-1", cache.etag)
        val other = BibleVideoRepositoryImpl(cache, fetcher, api, json, dispatcher)
        other.preload()
        assertEquals(listOf(42L), other.videos.value.map { it.id })
    }

    @Test
    fun `a failed refresh keeps the cached list`() = runTest(dispatcher) {
        cache.value = listOf(dto)
        fetcher.result = NetworkResult.Failure(AppError.Network())

        repository.refresh()

        assertEquals(listOf(42L), repository.videos.value.map { it.id })
    }

    @Test
    fun `adding sends the current chapter as the first place`() = runTest(dispatcher) {
        api.addResponse = Response.success(201, dto)

        val result = repository.addVideo(" https://youtu.be/x ", gn1)

        assertTrue(result is AddVideoResult.Added)
        val body = api.lastAddBody!!
        assertEquals("\"https://youtu.be/x\"", body["url"].toString())
        assertEquals("""{"book":"gn","chapter":1}""", (body["places"] as JsonArray).single().toString())
    }

    @Test
    fun `an existing link returns the existing video`() = runTest(dispatcher) {
        val existing = json.encodeToString(BibleVideoDto.serializer(), dto)
        api.addResponse = Response.error(
            409,
            """{"error_code":"BIBLE_VIDEO_EXISTS","detail":"Já cadastrado","video":$existing}""".toResponseBody(),
        )

        val result = repository.addVideo("https://youtu.be/x", gn1)

        assertEquals(42L, (result as AddVideoResult.AlreadyExists).video?.id)
    }

    @Test
    fun `an invalid link fails with the server's message`() = runTest(dispatcher) {
        api.addResponse = Response.error(
            400,
            """{"error_code":"BIBLE_VIDEO_INVALID_URL","detail":"Link inválido"}""".toResponseBody(),
        )

        val result = repository.addVideo("abc", gn1)

        assertEquals("Link inválido", (result as AddVideoResult.Failed).error.userMessage)
    }

    @Test
    fun `whole-book places send an explicit null chapter`() = runTest(dispatcher) {
        repository.setPlaces(42, listOf(BiblePlace.of("ex", null)))

        assertEquals(JsonNull, api.lastPlaces!!.single().jsonObject["chapter"])
    }

    private class FakeCache : SnapshotCache<List<BibleVideoDto>> {
        var value: List<BibleVideoDto>? = null
        var etag: String? = null
        override suspend fun load(): List<BibleVideoDto>? = value
        override suspend fun save(dto: List<BibleVideoDto>, etag: String?) {
            value = dto
            this.etag = etag
        }
        override suspend fun loadETag(): String? = etag
        override suspend fun clear() { value = null }
    }

    private class FakeFetcher : SnapshotFetcher<List<BibleVideoDto>> {
        var result: NetworkResult<List<BibleVideoDto>> = NetworkResult.NotModified
        override suspend fun fetch(etag: String?): NetworkResult<List<BibleVideoDto>> = result
    }

    private class FakeApi : BibleVideosManageApi {
        var addResponse: Response<BibleVideoDto> = Response.error(500, "".toResponseBody())
        var lastAddBody: JsonObject? = null
        var lastPlaces: JsonArray? = null

        override suspend fun addVideo(body: JsonObject): Response<BibleVideoDto> {
            lastAddBody = body
            return addResponse
        }

        override suspend fun setPlaces(id: Long, body: JsonArray): Response<Unit> {
            lastPlaces = body
            return Response.success(204, Unit)
        }

        override suspend fun removePlace(id: Long, book: String, chapter: Int?): Response<Unit> =
            Response.success(204, Unit)

        override suspend fun deleteVideo(id: Long): Response<Unit> = Response.success(204, Unit)

        override suspend fun setOrder(body: JsonObject): Response<Unit> = Response.success(204, Unit)
    }
}
