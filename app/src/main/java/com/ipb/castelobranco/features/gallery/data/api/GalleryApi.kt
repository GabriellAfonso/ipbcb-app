package com.ipb.castelobranco.features.gallery.data.api

import com.ipb.castelobranco.features.gallery.data.dto.GalleryAlbumDto
import com.ipb.castelobranco.features.gallery.data.dto.GalleryChangesDto
import com.ipb.castelobranco.features.gallery.data.dto.GalleryPhotoDto
import com.ipb.castelobranco.features.gallery.data.dto.PhotoUploadResultDto
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Reads are for members; writes need the level on `gallery` (`manage`, `owner` for deletes). JSON
 * bodies are [JsonObject]s built in `GalleryWriteBodies.kt`: the project's `Json` drops nulls, and a
 * PATCH needs an explicit `null` to clear a field or move to the root.
 */
interface GalleryApi {

    /** Without [since], every live album and photo; with it, what changed since shortly before it. */
    @GET(GalleryEndpoints.CHANGES)
    suspend fun getChanges(
        @Query(GalleryEndpoints.SINCE) since: String?,
    ): Response<GalleryChangesDto>

    @Streaming
    @GET
    suspend fun downloadFile(
        @Url absoluteUrl: String
    ): Response<ResponseBody>

    @POST(GalleryEndpoints.ALBUMS)
    suspend fun createAlbum(@Body body: JsonObject): Response<GalleryAlbumDto>

    @PATCH(GalleryEndpoints.ALBUM)
    suspend fun patchAlbum(
        @Path(GalleryEndpoints.ID) albumId: Long,
        @Body body: JsonObject,
    ): Response<GalleryAlbumDto>

    @PUT(GalleryEndpoints.ALBUM_ORDER)
    suspend fun orderAlbums(@Body body: JsonObject): Response<Unit>

    @Multipart
    @PUT(GalleryEndpoints.ALBUM_COVER)
    suspend fun putCover(
        @Path(GalleryEndpoints.ID) albumId: Long,
        @Part image: MultipartBody.Part,
    ): Response<GalleryAlbumDto>

    @DELETE(GalleryEndpoints.ALBUM_COVER)
    suspend fun deleteCover(@Path(GalleryEndpoints.ID) albumId: Long): Response<Unit>

    @DELETE(GalleryEndpoints.ALBUM)
    suspend fun deleteAlbum(@Path(GalleryEndpoints.ID) albumId: Long): Response<Unit>

    /** One file per request, with its [clientUploadId], so a retry never creates a second photo. */
    @Multipart
    @POST(GalleryEndpoints.PHOTOS)
    suspend fun uploadPhoto(
        @Part(GalleryEndpoints.PART_ALBUM_ID) albumId: RequestBody,
        @Part(GalleryEndpoints.PART_CLIENT_UPLOAD_ID) clientUploadId: RequestBody,
        @Part image: MultipartBody.Part,
    ): Response<PhotoUploadResultDto>

    @PATCH(GalleryEndpoints.PHOTO)
    suspend fun patchPhoto(
        @Path(GalleryEndpoints.ID) photoId: Long,
        @Body body: JsonObject,
    ): Response<GalleryPhotoDto>

    @PUT(GalleryEndpoints.ALBUM_PHOTOS_ORDER)
    suspend fun orderPhotos(
        @Path(GalleryEndpoints.ID) albumId: Long,
        @Body body: JsonObject,
    ): Response<Unit>

    @DELETE(GalleryEndpoints.PHOTO)
    suspend fun deletePhoto(@Path(GalleryEndpoints.ID) photoId: Long): Response<Unit>
}
